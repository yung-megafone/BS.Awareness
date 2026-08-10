# Android Auto POI vs Navigation A/B Test

This branch keeps the existing BSA phone application shared while producing two Android Auto flavors.

## Variants

| Variant | Android Auto category | Application ID | Purpose |
|---|---|---|---|
| `poiDebug` | `POI` | `co.bssply.bsa` | Existing control: POI + debug signing |
| `navDebug` | `NAVIGATION` | `co.bssply.bsa.navtest` | Tests Android Auto category while keeping debug signing |
| `poiRelease` | `POI` | `co.bssply.bsa` | Tests release signing without changing category |
| `navRelease` | `NAVIGATION` | `co.bssply.bsa.navtest` | Tests navigation category + release signing |

Release variants are not configured with a private signing key yet. Configure local release signing before using the release pair for the real-car experiment.

## Build the debug controls

```powershell
.\gradlew.bat clean assemblePoiDebug assembleNavDebug
```

Expected APKs:

```text
app\build\outputs\apk\poi\debug\app-poi-debug.apk
app\build\outputs\apk\nav\debug\app-nav-debug.apk
```

Because the NAV test has an `applicationIdSuffix`, it can be installed beside the normal BSA POI build.

## What differs

The POI flavor retains BSA's current `PlaceListMapTemplate` and `androidx.car.app.category.POI` declaration.

The NAV flavor declares:

- `androidx.car.app.category.NAVIGATION`
- `androidx.car.app.NAVIGATION_TEMPLATES`
- `androidx.car.app.ACCESS_SURFACE`
- `MapWithContentTemplate` when the host supports Car App API 7+
- A deliberately minimal app-rendered test surface
- The same enabled nearby-POI list used by the POI flavor

The test surface is intentionally not the final BSA map. It exists to exercise the navigation/custom-surface path without mixing a MapLibre port into the discovery experiment.

## Suggested test order

1. Confirm `poiDebug` still works in DHU.
2. Confirm `navDebug` appears and launches in DHU.
3. With both installed, connect to the real Android Auto head unit and note which package(s) appear.
4. After local release signing is configured, repeat with `poiRelease` and `navRelease`, uninstalling the corresponding debug package first when the signing certificate differs.

This keeps category and signing as separable variables instead of changing both at once.
