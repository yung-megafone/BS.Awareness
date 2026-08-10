Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

[System.Windows.Forms.Application]::EnableVisualStyles()

$script:Busy = $false
$script:RepoRoot = $null
$script:Adb = $null
$script:PhoneSerial = $null
$script:LogPath = $null

function Find-RepoRoot {
    $starts = @()
    if ($PSScriptRoot) { $starts += $PSScriptRoot }
    $starts += (Get-Location).Path

    foreach ($start in ($starts | Select-Object -Unique)) {
        try { $dir = Get-Item $start } catch { continue }
        while ($dir) {
            if ((Test-Path (Join-Path $dir.FullName "gradlew.bat")) -and
                (Test-Path (Join-Path $dir.FullName "app\build.gradle.kts"))) {
                return $dir.FullName
            }
            $dir = $dir.Parent
        }
    }
    return $null
}

function UI-DoEvents {
    [System.Windows.Forms.Application]::DoEvents()
}

function UI-Log([string]$Message, [string]$Level = "INFO") {
    $stamp = Get-Date -Format "HH:mm:ss"
    $line = "[$stamp] [$Level] $Message"
    $txtLog.AppendText($line + [Environment]::NewLine)
    $txtLog.SelectionStart = $txtLog.TextLength
    $txtLog.ScrollToCaret()
    UI-DoEvents

    if ($script:LogPath) {
        try { Add-Content -Path $script:LogPath -Value $line -Encoding UTF8 } catch {}
    }
}

function UI-Status([string]$Message, [int]$Percent = -1) {
    $lblStatus.Text = $Message
    if ($Percent -ge 0) {
        $progress.Value = [Math]::Max(0, [Math]::Min(100, $Percent))
    }
    UI-DoEvents
}

function Set-MainButtonText([string]$Text) {
    if ($btnFull) {
        $btnFull.Text = $Text
        UI-DoEvents
    }
}

function Set-Busy([bool]$Value) {
    $script:Busy = $Value
    $btnFull.Enabled = -not $Value
    $btnBuild.Enabled = -not $Value
    $btnInstall.Enabled = -not $Value
    $btnDhu.Enabled = -not $Value
    $btnSigning.Enabled = -not $Value
    $btnRefresh.Enabled = -not $Value
    $chkClean.Enabled = -not $Value
}

function Get-JavaMajorVersion([string]$JavaExe) {
    try {
        $output = & $JavaExe -version 2>&1
        $first = ($output | Select-Object -First 1).ToString()

        if ($first -match '"1\.(\d+)') {
            return [int]$Matches[1]
        }
        if ($first -match '"(\d+)') {
            return [int]$Matches[1]
        }
    } catch {}
    return $null
}

function Ensure-Java {
    # BSA is an Android Studio project. Prefer Android Studio's bundled JBR
    # instead of whichever Java happens to be first on Windows PATH.
    $candidates = @(
        "C:\Program Files\Android\Android Studio\jbr",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
        "C:\Program Files\Android\Android Studio\jre"
    )

    foreach ($j in $candidates) {
        $javaExe = Join-Path $j "bin\java.exe"
        if (Test-Path $javaExe) {
            $major = Get-JavaMajorVersion $javaExe
            if ($major -and $major -ge 17) {
                $env:JAVA_HOME = $j

                # Remove stale Java bin entries so Gradle cannot fall back to Java 8.
                $parts = $env:Path -split ';' | Where-Object {
                    $_ -and
                    ($_ -notmatch '(?i)\\Java\\.*\\bin$') -and
                    ($_ -notmatch '(?i)\\Android Studio\\jbr\\bin$') -and
                    ($_ -notmatch '(?i)\\Android Studio\\jre\\bin$')
                }

                $env:Path = "$j\bin;" + ($parts -join ';')
                $env:GRADLE_JAVA_HOME = $j

                UI-Log "Using Android Studio JBR: $j (Java $major)" "OK"
                return
            }
        }
    }

    # Fallback only if Android Studio's bundled runtime cannot be located.
    $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($cmd) {
        $major = Get-JavaMajorVersion $cmd.Source
        if ($major -and $major -ge 17) {
            $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $cmd.Source)
            $env:GRADLE_JAVA_HOME = $env:JAVA_HOME
            UI-Log "Android Studio JBR not found; using Java $major at $($cmd.Source)." "WARN"
            return
        }

        if ($major) {
            throw "Only Java $major was found. BSA requires Java 17+ and Android Studio's JBR could not be found."
        }
    }

    throw "Java 17+ was not found. Install Android Studio or configure a compatible JDK."
}

function Find-Adb {
    $candidate = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $candidate) { return $candidate }

    $cmd = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

function Find-Dhu {
    $sdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    $candidates = @(
        (Join-Path $sdkRoot "extras\google\auto\desktop-head-unit.exe"),
        (Join-Path $sdkRoot "extras\google\auto\desktop-head-unit64.exe")
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }
    return $null
}

function Wait-For-Phone {
    if (-not $script:Adb) {
        $script:Adb = Find-Adb
        if (-not $script:Adb) { throw "ADB was not found. Install Android SDK Platform-Tools." }
    }

    & $script:Adb start-server | Out-Null
    UI-Log "Waiting for an authorized Android device..."

    while ($true) {
        $rows = @(& $script:Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S' })

        $device = $rows | Where-Object { $_ -match "\tdevice$" } | Select-Object -First 1
        if ($device) {
            $script:PhoneSerial = ($device -split "\t")[0]
            UI-Log "Phone connected and authorized: $script:PhoneSerial" "OK"
            return
        }

        $unauth = $rows | Where-Object { $_ -match "\tunauthorized$" } | Select-Object -First 1
        if ($unauth) {
            UI-Status "Unlock phone and approve USB debugging…"
            $lblPhone.Text = "Phone: unauthorized — unlock and approve"
        } else {
            UI-Status "Waiting for phone…"
            $lblPhone.Text = "Phone: not connected"
        }
        UI-DoEvents
        Start-Sleep -Seconds 2
    }
}

function Read-KeystoreProperties {
    $propsPath = Join-Path $script:RepoRoot "keystore.properties"
    if (-not (Test-Path $propsPath)) { return $null }

    $map = @{}
    foreach ($line in Get-Content $propsPath) {
        if ($line -match '^\s*#' -or [string]::IsNullOrWhiteSpace($line)) { continue }

        $idx = $line.IndexOf('=')
        if ($idx -gt 0) {
            $key = $line.Substring(0, $idx).Trim()
            $value = $line.Substring($idx + 1)
            $map[$key] = $value
        }
    }

    return $map
}

function Write-KeystoreProperties([string]$StoreFile, [string]$Password, [string]$Alias) {
    # IMPORTANT: Java .properties treats backslashes as escapes.
    # Always store Gradle paths with forward slashes.
    $storeForGradle = $StoreFile.Replace('\','/')

    $content = @(
        "storeFile=$storeForGradle",
        "storePassword=$Password",
        "keyAlias=$Alias",
        "keyPassword=$Password"
    ) -join [Environment]::NewLine

    $propsPath = Join-Path $script:RepoRoot "keystore.properties"

    # Windows PowerShell's Set-Content -Encoding UTF8 adds a BOM.
    # Java Properties.load(InputStream) can interpret that BOM as part of
    # the first property name. Write UTF-8 explicitly WITHOUT a BOM.
    [System.IO.File]::WriteAllText(
        $propsPath,
        $content + [Environment]::NewLine,
        (New-Object System.Text.UTF8Encoding($false))
    )
}

function Get-Keytool {
    $candidates = @()

    if ($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME "bin\keytool.exe")
    }

    $candidates += @(
        "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
        "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe"
    )

    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }

    $cmd = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    return $null
}

function Repair-SigningConfig {
    $props = Read-KeystoreProperties
    if (-not $props) { return $false }

    $required = @("storeFile", "storePassword", "keyAlias", "keyPassword")
    foreach ($key in $required) {
        if (-not $props.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($props[$key])) {
            return $false
        }
    }

    # Repair old Windows-style path values such as signing\bsa-release.jks.
    $normalized = $props["storeFile"].Replace('\','/')
    if ($normalized -ne $props["storeFile"]) {
        UI-Log "Repairing backslashes in keystore.properties." "WARN"
        Write-KeystoreProperties $normalized $props["storePassword"] $props["keyAlias"]
        $props = Read-KeystoreProperties
    }

    $keystorePath = Join-Path $script:RepoRoot $props["storeFile"]
    if (-not (Test-Path $keystorePath)) {
        return $false
    }

    return $true
}

function Signing-Configured {
    return (Repair-SigningConfig)
}

function Show-SigningDialog {
    $dlg = New-Object System.Windows.Forms.Form
    $dlg.Text = "BSA Local Release Signing"
    $dlg.Size = New-Object System.Drawing.Size(470, 300)
    $dlg.StartPosition = "CenterParent"
    $dlg.FormBorderStyle = "FixedDialog"
    $dlg.MaximizeBox = $false
    $dlg.MinimizeBox = $false
    $dlg.BackColor = [System.Drawing.Color]::FromArgb(24,27,31)
    $dlg.ForeColor = [System.Drawing.Color]::Gainsboro
    $dlg.Font = New-Object System.Drawing.Font("Segoe UI", 9)

    $info = New-Object System.Windows.Forms.Label
    $info.Text = "Create a LOCAL BSA release-signing key.`r`nNo Google account or Play developer account is involved."
    $info.AutoSize = $true
    $info.Location = New-Object System.Drawing.Point(18,18)
    $dlg.Controls.Add($info)

    $l1 = New-Object System.Windows.Forms.Label
    $l1.Text = "Password (6+ characters)"
    $l1.AutoSize = $true
    $l1.Location = New-Object System.Drawing.Point(18,78)
    $dlg.Controls.Add($l1)

    $p1 = New-Object System.Windows.Forms.TextBox
    $p1.UseSystemPasswordChar = $true
    $p1.Size = New-Object System.Drawing.Size(410,25)
    $p1.Location = New-Object System.Drawing.Point(18,100)
    $dlg.Controls.Add($p1)

    $l2 = New-Object System.Windows.Forms.Label
    $l2.Text = "Confirm password"
    $l2.AutoSize = $true
    $l2.Location = New-Object System.Drawing.Point(18,136)
    $dlg.Controls.Add($l2)

    $p2 = New-Object System.Windows.Forms.TextBox
    $p2.UseSystemPasswordChar = $true
    $p2.Size = New-Object System.Drawing.Size(410,25)
    $p2.Location = New-Object System.Drawing.Point(18,158)
    $dlg.Controls.Add($p2)

    $note = New-Object System.Windows.Forms.Label
    $note.Text = "Back up signing\bsa-release.jks somewhere safe."
    $note.AutoSize = $true
    $note.ForeColor = [System.Drawing.Color]::Khaki
    $note.Location = New-Object System.Drawing.Point(18,195)
    $dlg.Controls.Add($note)

    $ok = New-Object System.Windows.Forms.Button
    $ok.Text = "Create / Configure"
    $ok.Size = New-Object System.Drawing.Size(140,32)
    $ok.Location = New-Object System.Drawing.Point(288,220)
    $ok.DialogResult = [System.Windows.Forms.DialogResult]::OK
    $dlg.Controls.Add($ok)

    $cancel = New-Object System.Windows.Forms.Button
    $cancel.Text = "Cancel"
    $cancel.Size = New-Object System.Drawing.Size(90,32)
    $cancel.Location = New-Object System.Drawing.Point(188,220)
    $cancel.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
    $dlg.Controls.Add($cancel)

    $dlg.AcceptButton = $ok
    $dlg.CancelButton = $cancel

    while ($true) {
        $result = $dlg.ShowDialog($form)
        if ($result -ne [System.Windows.Forms.DialogResult]::OK) {
            return $null
        }

        if ($p1.Text.Length -lt 6) {
            [void][System.Windows.Forms.MessageBox]::Show(
                "Password must be at least 6 characters.",
                "Signing",
                [System.Windows.Forms.MessageBoxButtons]::OK,
                [System.Windows.Forms.MessageBoxIcon]::Warning
            )
            continue
        }

        if ($p1.Text -ne $p2.Text) {
            [void][System.Windows.Forms.MessageBox]::Show(
                "Passwords do not match.",
                "Signing",
                [System.Windows.Forms.MessageBoxButtons]::OK,
                [System.Windows.Forms.MessageBoxIcon]::Warning
            )
            continue
        }

        return $p1.Text
    }
}

function Run-Signing-Setup {
    Ensure-Java

    $keytool = Get-Keytool
    if (-not $keytool) {
        throw "keytool.exe was not found in Android Studio's JBR."
    }

    $alias = "bsa"
    $relativeKeystore = "signing/bsa-release.jks"
    $fullKeystore = Join-Path $script:RepoRoot $relativeKeystore
    $keystoreDir = Split-Path -Parent $fullKeystore
    New-Item -ItemType Directory -Force -Path $keystoreDir | Out-Null

    $password = Show-SigningDialog
    if ($null -eq $password) {
        throw "Signing setup cancelled."
    }

    if (-not (Test-Path $fullKeystore)) {
        UI-Log "Generating local BSA release key..."

        $args = @(
            "-genkeypair", "-v",
            "-keystore", $fullKeystore,
            "-alias", $alias,
            "-keyalg", "RSA",
            "-keysize", "4096",
            "-validity", "10000",
            "-storepass", $password,
            "-keypass", $password,
            "-dname", "CN=B.S. Awareness, OU=Local Release Signing, O=B.S. Awareness"
        )

        & $keytool @args 2>&1 | ForEach-Object { UI-Log "$_" "KEYTOOL" }

        if ($LASTEXITCODE -ne 0) {
            throw "keytool failed with exit code $LASTEXITCODE."
        }
    }
    else {
        UI-Log "Reusing existing keystore: $fullKeystore" "WARN"

        & $keytool -list -keystore $fullKeystore -storepass $password -alias $alias 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Existing keystore could not be opened with that password / alias."
        }
    }

    Write-KeystoreProperties $relativeKeystore $password $alias

    if (-not (Repair-SigningConfig)) {
        throw "Signing files were created, but validation failed."
    }

    UI-Log "Local release signing is fully configured and validated." "OK"
}

function Build-Matrix {
    if (-not (Signing-Configured)) {
        $answer = [System.Windows.Forms.MessageBox]::Show(
            "Release signing is not configured. Run the local signing setup now?",
            "BSA LAD",
            [System.Windows.Forms.MessageBoxButtons]::YesNo,
            [System.Windows.Forms.MessageBoxIcon]::Question
        )
        if ($answer -ne [System.Windows.Forms.DialogResult]::Yes) {
            throw "Release signing is required for the full 2x2 matrix."
        }
        Run-Signing-Setup
    }

    $args = @()
    if ($chkClean.Checked) { $args += "clean" }
    $args += @(
        "assemblePoiDebug",
        "assemblePoiRelease",
        "assembleNavDebug",
        "assembleNavRelease"
    )

    UI-Log "Running Gradle: .\gradlew.bat $($args -join ' ')"
    UI-Status "Building four APK variants…", 20

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = (Join-Path $script:RepoRoot "gradlew.bat")
    $psi.WorkingDirectory = $script:RepoRoot
    $psi.Arguments = ($args -join " ")
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true

    $proc = New-Object System.Diagnostics.Process
    $proc.StartInfo = $psi
    [void]$proc.Start()

    while (-not $proc.HasExited) {
        while (-not $proc.StandardOutput.EndOfStream) {
            UI-Log ($proc.StandardOutput.ReadLine()) "GRADLE"
        }
        while (-not $proc.StandardError.EndOfStream) {
            UI-Log ($proc.StandardError.ReadLine()) "GRADLE"
        }
        UI-DoEvents
        Start-Sleep -Milliseconds 100
    }

    while (-not $proc.StandardOutput.EndOfStream) {
        UI-Log ($proc.StandardOutput.ReadLine()) "GRADLE"
    }
    while (-not $proc.StandardError.EndOfStream) {
        UI-Log ($proc.StandardError.ReadLine()) "GRADLE"
    }

    if ($proc.ExitCode -ne 0) {
        throw "Gradle build failed with exit code $($proc.ExitCode)."
    }

    UI-Log "All four APK variants built successfully." "OK"
    UI-Status "Build complete.", 55
}

function Get-Variants {
    return @(
        @{ Name="POI Debug";   Package="co.bssply.bsa.poi.debug"; Apk=(Join-Path $script:RepoRoot "app\build\outputs\apk\poi\debug\app-poi-debug.apk") },
        @{ Name="POI Release"; Package="co.bssply.bsa.poi";       Apk=(Join-Path $script:RepoRoot "app\build\outputs\apk\poi\release\app-poi-release.apk") },
        @{ Name="NAV Debug";   Package="co.bssply.bsa.nav.debug"; Apk=(Join-Path $script:RepoRoot "app\build\outputs\apk\nav\debug\app-nav-debug.apk") },
        @{ Name="NAV Release"; Package="co.bssply.bsa.nav";       Apk=(Join-Path $script:RepoRoot "app\build\outputs\apk\nav\release\app-nav-release.apk") }
    )
}

function Install-Matrix {
    Wait-For-Phone
    $variants = Get-Variants

    UI-Status "Installing APK matrix…", 65

    foreach ($v in $variants) {
        if (-not (Test-Path $v.Apk)) {
            throw "Missing APK: $($v.Apk). Build first."
        }

        UI-Log "Installing $($v.Name) [$($v.Package)]..."
        $out = & $script:Adb -s $script:PhoneSerial install -r -g $v.Apk 2>&1
        $out | ForEach-Object { UI-Log "$_" "ADB" }
        if ($LASTEXITCODE -ne 0) {
            throw "ADB install failed for $($v.Name)."
        }
    }

    UI-Log "All four variants installed." "OK"
    UI-Status "Install complete.", 80
}

function Test-HeadUnitServer {
    $commands = @("ss -ltn 2>/dev/null", "netstat -ltn 2>/dev/null")
    foreach ($cmd in $commands) {
        try {
            $out = & $script:Adb -s $script:PhoneSerial shell $cmd 2>$null
            if ($out -match '(^|[\s:\[\]])5277(\s|$)') { return $true }
        } catch {}
    }
    return $false
}

function Wait-For-HeadUnitServer {
    Wait-For-Phone

    while (-not (Test-HeadUnitServer)) {
        UI-Status "Start Android Auto Head Unit Server on phone…", 85
        $lblAa.Text = "AA server: not running"

        [void][System.Windows.Forms.MessageBox]::Show(
            "Android Auto's Head Unit Server is not running.`r`n`r`nOn your phone:`r`n1. Open Android Auto settings.`r`n2. Open the developer / three-dot menu.`r`n3. Tap 'Start head unit server'.`r`n4. Keep the phone unlocked.`r`n`r`nClick OK after starting it.",
            "Start Android Auto Head Unit Server",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Information
        )
    }

    $lblAa.Text = "AA server: running"
    UI-Log "Android Auto Head Unit Server detected." "OK"
}

function Launch-Dhu {
    Wait-For-HeadUnitServer

    & $script:Adb -s $script:PhoneSerial forward --remove tcp:5277 2>$null | Out-Null
    & $script:Adb -s $script:PhoneSerial forward tcp:5277 tcp:5277 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not create ADB forward tcp:5277." }

    $dhu = Find-Dhu
    if (-not $dhu) {
        throw "DHU not found. Install Android Auto Desktop Head Unit Emulator from Android Studio SDK Tools."
    }

    UI-Log "Launching DHU: $dhu" "OK"
    UI-Status "DHU running — close it when finished.", 95

    $proc = Start-Process -FilePath $dhu -PassThru
    while (-not $proc.HasExited) {
        UI-DoEvents
        Start-Sleep -Milliseconds 250
    }

    & $script:Adb -s $script:PhoneSerial forward --remove tcp:5277 2>$null | Out-Null
    UI-Log "DHU closed; ADB forward removed."
    UI-Status "Ready.", 100
}

function Refresh-Environment {
    try {
        $script:RepoRoot = Find-RepoRoot
        if ($script:RepoRoot) {
            $lblRepo.Text = "Repo: $script:RepoRoot"
        } else {
            $lblRepo.Text = "Repo: NOT FOUND"
        }

        try {
            Ensure-Java
            $lblJava.Text = "Java: ready"
        } catch {
            $lblJava.Text = "Java: missing"
        }

        $script:Adb = Find-Adb
        $lblAdb.Text = if ($script:Adb) { "ADB: ready" } else { "ADB: missing" }

        if ($script:RepoRoot) {
            $lblSigning.Text = if (Signing-Configured) { "Signing: ready" } else { "Signing: not configured" }
        }

        if ($script:Adb) {
            $rows = @(& $script:Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\S' })
            if ($rows | Where-Object { $_ -match "\tdevice$" }) {
                $lblPhone.Text = "Phone: connected"
            } elseif ($rows | Where-Object { $_ -match "\tunauthorized$" }) {
                $lblPhone.Text = "Phone: unauthorized"
            } else {
                $lblPhone.Text = "Phone: not connected"
            }
        }

        $lblAa.Text = "AA server: check on launch"
        UI-Status "Ready.", 0
    } catch {
        UI-Log $_.Exception.Message "ERROR"
    }
}

function Run-Action([scriptblock]$Action) {
    if ($script:Busy) { return }
    Set-Busy $true
    Set-MainButtonText "PLEASE DON'T FUCKING BREAK"

    $succeeded = $false

    try {
        if (-not $script:RepoRoot) {
            $script:RepoRoot = Find-RepoRoot
            if (-not $script:RepoRoot) { throw "Could not find the BSA repo root." }
        }

        Set-Location $script:RepoRoot

        $logDir = Join-Path $script:RepoRoot "build-logs"
        New-Item -ItemType Directory -Force -Path $logDir | Out-Null
        $script:LogPath = Join-Path $logDir ("lad-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".log")

        & $Action
        $succeeded = $true
        Set-MainButtonText "HOLY SHIT IT WORKED"
    } catch {
        UI-Log $_.Exception.Message "ERROR"
        UI-Status "Failed — see log.", 0
        Set-MainButtonText "PLEASE DON'T BREAK (AGAIN)"

        [void][System.Windows.Forms.MessageBox]::Show(
            $_.Exception.Message + "`r`n`r`nLog:`r`n" + $script:LogPath,
            "BSA LAD — Failed",
            [System.Windows.Forms.MessageBoxButtons]::OK,
            [System.Windows.Forms.MessageBoxIcon]::Error
        )
    } finally {
        Set-Busy $false
        Refresh-Environment

        if ($succeeded) {
            $resetTimer = New-Object System.Windows.Forms.Timer
            $resetTimer.Interval = 2500
            $resetTimer.Add_Tick({
                $resetTimer.Stop()
                $resetTimer.Dispose()
                Set-MainButtonText "JUST DO IT"
            })
            $resetTimer.Start()
        }
    }
}

# -----------------------------
# GUI
# -----------------------------

$form = New-Object System.Windows.Forms.Form
$form.Text = "BSA LAD — Lazy Ass Developer"
$form.Size = New-Object System.Drawing.Size(860, 650)
$form.StartPosition = "CenterScreen"
$form.MinimumSize = New-Object System.Drawing.Size(760, 560)
$form.BackColor = [System.Drawing.Color]::FromArgb(24, 27, 31)
$form.ForeColor = [System.Drawing.Color]::Gainsboro
$form.Font = New-Object System.Drawing.Font("Segoe UI", 9)

$title = New-Object System.Windows.Forms.Label
$title.Text = "BSA LAD"
$title.Font = New-Object System.Drawing.Font("Segoe UI Semibold", 20)
$title.AutoSize = $true
$title.Location = New-Object System.Drawing.Point(20, 16)
$form.Controls.Add($title)

$subtitle = New-Object System.Windows.Forms.Label
$subtitle.Text = "Lazy Ass Developer • Build • Install • Android Auto • DHU"
$subtitle.ForeColor = [System.Drawing.Color]::Silver
$subtitle.AutoSize = $true
$subtitle.Location = New-Object System.Drawing.Point(23, 55)
$form.Controls.Add($subtitle)

$statusPanel = New-Object System.Windows.Forms.Panel
$statusPanel.Location = New-Object System.Drawing.Point(20, 88)
$statusPanel.Size = New-Object System.Drawing.Size(805, 105)
$statusPanel.Anchor = "Top,Left,Right"
$statusPanel.BackColor = [System.Drawing.Color]::FromArgb(34, 38, 43)
$form.Controls.Add($statusPanel)

$lblRepo = New-Object System.Windows.Forms.Label
$lblRepo.AutoSize = $true
$lblRepo.Location = New-Object System.Drawing.Point(12, 10)
$statusPanel.Controls.Add($lblRepo)

$lblJava = New-Object System.Windows.Forms.Label
$lblJava.AutoSize = $true
$lblJava.Location = New-Object System.Drawing.Point(12, 34)
$statusPanel.Controls.Add($lblJava)

$lblAdb = New-Object System.Windows.Forms.Label
$lblAdb.AutoSize = $true
$lblAdb.Location = New-Object System.Drawing.Point(180, 34)
$statusPanel.Controls.Add($lblAdb)

$lblSigning = New-Object System.Windows.Forms.Label
$lblSigning.AutoSize = $true
$lblSigning.Location = New-Object System.Drawing.Point(330, 34)
$statusPanel.Controls.Add($lblSigning)

$lblPhone = New-Object System.Windows.Forms.Label
$lblPhone.AutoSize = $true
$lblPhone.Location = New-Object System.Drawing.Point(12, 58)
$statusPanel.Controls.Add($lblPhone)

$lblAa = New-Object System.Windows.Forms.Label
$lblAa.AutoSize = $true
$lblAa.Location = New-Object System.Drawing.Point(180, 58)
$statusPanel.Controls.Add($lblAa)

$chkClean = New-Object System.Windows.Forms.CheckBox
$chkClean.Text = "Clean before build"
$chkClean.Checked = $true
$chkClean.AutoSize = $true
$chkClean.Location = New-Object System.Drawing.Point(12, 80)
$statusPanel.Controls.Add($chkClean)

$btnRefresh = New-Object System.Windows.Forms.Button
$btnRefresh.Text = "Refresh"
$btnRefresh.Size = New-Object System.Drawing.Size(90, 28)
$btnRefresh.Location = New-Object System.Drawing.Point(700, 68)
$btnRefresh.Anchor = "Top,Right"
$statusPanel.Controls.Add($btnRefresh)

$buttonPanel = New-Object System.Windows.Forms.FlowLayoutPanel
$buttonPanel.Location = New-Object System.Drawing.Point(20, 210)
$buttonPanel.Size = New-Object System.Drawing.Size(805, 78)
$buttonPanel.Anchor = "Top,Left,Right"
$buttonPanel.WrapContents = $true
$form.Controls.Add($buttonPanel)

function New-Button([string]$Text, [int]$Width = 145) {
    $b = New-Object System.Windows.Forms.Button
    $b.Text = $Text
    $b.Size = New-Object System.Drawing.Size($Width, 34)
    $b.Margin = New-Object System.Windows.Forms.Padding(0,0,8,8)
    return $b
}

$btnFull = New-Button "Run Full LAD Run" 170
$btnBuild = New-Button "Build Matrix"
$btnInstall = New-Button "Install Existing"
$btnDhu = New-Button "Launch DHU"
$btnSigning = New-Button "Setup Signing"

$buttonPanel.Controls.AddRange(@($btnFull,$btnBuild,$btnInstall,$btnDhu,$btnSigning))

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Location = New-Object System.Drawing.Point(20, 300)
$progress.Size = New-Object System.Drawing.Size(805, 20)
$progress.Anchor = "Top,Left,Right"
$form.Controls.Add($progress)

$lblStatus = New-Object System.Windows.Forms.Label
$lblStatus.Text = "Ready."
$lblStatus.AutoSize = $true
$lblStatus.Location = New-Object System.Drawing.Point(20, 327)
$form.Controls.Add($lblStatus)

$txtLog = New-Object System.Windows.Forms.TextBox
$txtLog.Location = New-Object System.Drawing.Point(20, 355)
$txtLog.Size = New-Object System.Drawing.Size(805, 225)
$txtLog.Anchor = "Top,Bottom,Left,Right"
$txtLog.Multiline = $true
$txtLog.ScrollBars = "Vertical"
$txtLog.ReadOnly = $true
$txtLog.BackColor = [System.Drawing.Color]::FromArgb(17, 19, 22)
$txtLog.ForeColor = [System.Drawing.Color]::Gainsboro
$txtLog.Font = New-Object System.Drawing.Font("Consolas", 9)
$form.Controls.Add($txtLog)

$footer = New-Object System.Windows.Forms.Label
$footer.Text = "LAD: overcomplicate the tooling so you can undercomplicate the work."
$footer.AutoSize = $true
$footer.ForeColor = [System.Drawing.Color]::Gray
$footer.Location = New-Object System.Drawing.Point(20, 590)
$footer.Anchor = "Bottom,Left"
$form.Controls.Add($footer)

$btnRefresh.Add_Click({ Refresh-Environment })

$btnSigning.Add_Click({
    Run-Action {
        Ensure-Java
        Run-Signing-Setup
        UI-Status "Signing configured.", 100
    }
})

$btnBuild.Add_Click({
    Run-Action {
        Ensure-Java
        Build-Matrix
        UI-Status "Build complete.", 100
    }
})

$btnInstall.Add_Click({
    Run-Action {
        Ensure-Java
        Install-Matrix
        UI-Status "Install complete.", 100
    }
})

$btnDhu.Add_Click({
    Run-Action {
        Ensure-Java
        Launch-Dhu
    }
})

$btnFull.Add_Click({
    Run-Action {
        Ensure-Java
        UI-Status "Preparing…", 5
        Wait-For-Phone
        UI-Status "Phone ready.", 10
        Build-Matrix
        Install-Matrix
        Launch-Dhu
        UI-Status "Done.", 100
    }
})

$form.Add_Shown({
    Refresh-Environment
    UI-Log "LAD ready. Do the shit."
})

[void]$form.ShowDialog()
