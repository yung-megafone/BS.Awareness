$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$GradleVersion = "8.13"
$GradleHome = Join-Path $ProjectRoot ".gradle-dist\gradle-$GradleVersion"
$GradleZip = Join-Path $ProjectRoot ".gradle-dist\gradle-$GradleVersion-bin.zip"

if (-not $env:JAVA_HOME) {
    $AndroidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path (Join-Path $AndroidStudioJbr "bin\java.exe")) {
        $env:JAVA_HOME = $AndroidStudioJbr
    }
}

if (-not $env:ANDROID_HOME) {
    $DefaultSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk"
    if (Test-Path $DefaultSdk) {
        $env:ANDROID_HOME = $DefaultSdk
    }
}

if ($env:ANDROID_HOME) {
    $EscapedSdk = $env:ANDROID_HOME -replace '\\','\\'
    "sdk.dir=$EscapedSdk" | Set-Content -Encoding ASCII (Join-Path $ProjectRoot "local.properties")
}

if (-not (Test-Path (Join-Path $GradleHome "bin\gradle.bat"))) {
    New-Item -ItemType Directory -Force (Split-Path $GradleZip) | Out-Null
    Write-Host "Downloading Gradle $GradleVersion..."
    Invoke-WebRequest "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $GradleZip
    Write-Host "Extracting Gradle..."
    Expand-Archive -Path $GradleZip -DestinationPath (Split-Path $GradleZip) -Force
}

Push-Location $ProjectRoot
try {
    & (Join-Path $GradleHome "bin\gradle.bat") :app:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
    $Apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    Write-Host ""
    Write-Host "APK: $Apk"
} finally {
    Pop-Location
}
