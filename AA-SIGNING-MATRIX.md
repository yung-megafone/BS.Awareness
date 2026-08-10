# Android Auto 2x2 Signing / Category Test

This experiment isolates two variables that may affect real-vehicle Android Auto discovery:

| | Debug signed | Local release signed |
|---|---|---|
| **POI** | `co.bssply.bsa.poi.debug` | `co.bssply.bsa.poi` |
| **NAVIGATION** | `co.bssply.bsa.nav.debug` | `co.bssply.bsa.nav` |

All four APKs are intentionally installable at the same time.

## 1. Create a local signing key

No Google or Play developer account is involved.

```powershell
.\tools\setup-local-signing.ps1
```

The script creates:

- `signing/bsa-release.jks`
- `keystore.properties`

Both locations are ignored by Git.

**Back up the JKS file.** An APK signed by this key can only be updated by another APK signed with the same key.

## 2. Build and install the complete matrix

```powershell
.\tools\build-aa-matrix.ps1
```

To build without installing:

```powershell
.\tools\build-aa-matrix.ps1 -NoInstall
```

## 3. Record the real-vehicle result

| Variant | Appears in Civic? | Launches? |
|---|---:|---:|
| POI Debug | | |
| POI Release | | |
| NAV Debug | | |
| NAV Release | | |

### Interpretation

- Release variants work, debug variants do not: signing/build type is implicated.
- NAV variants work, POI variants do not: Android Auto category is implicated.
- Only NAV Release works: both variables or their interaction may matter.
- None work: category and local debug-vs-release signing do not explain the production-host gate.
