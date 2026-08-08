# B.S. Awareness — 0.4.0-alpha

**Navigation & Awareness Alpha** — an open-source, local-first Android mapping client built around OpenStreetMap.

This is a field-test build, not a production navigation product. The goal is to combine normal OSM POIs, surveillance awareness, basic routing, Android Auto awareness, vehicle capability probing, and exportable diagnostics in one coherent testable app.

## Highlights

- **OpenFreeMap Dark** vector basemap by default through MapLibre Native. Liberty is available as the day-map fallback.
- Compact, viewport-first phone UI.
- Lucide-style native stroke icon system for the UI and POI badges, with Lucide attribution in the repository.
- Live foreground GPS marker: arrow, dot, or car.
- Optional follow mode with heading-oriented map.
- Automatic nearby OSM/Overpass refresh as the map moves.
- Persistent POI filters: surveillance, fuel, food, shopping, services/parking, lodging, other.
- `operator=*`, `agency=*`, then `owner=*` display for surveillance agency/operator information.
- Camera-bearing-aware markers when `camera:direction=*` or `direction=*` is numeric.
- Optional estimated coverage sectors when a bearing and a usable range tag (`bsa:range_m`, `camera:range`, or `range`) both exist. Coverage is explicitly advisory.
- User-submitted destination search (Nominatim; **no autocomplete**).
- Basic A→B driving routes using OSRM: route geometry, distance, ETA, maneuver text, GPS progress, and simple off-route recalculation.
- Save map center as **Home** or **Work**, then route to either from Settings.
- `Next Gas` v1: nearest practical loaded fuel POI, preferring ahead-of-travel candidates.
- Mark a fuel POI as a locally saved preferred station for Fuel Assist.
- Android Auto POI-category screen showing route state, nearby fuel/surveillance, and vehicle fuel capability state.
- Android Auto `CAR_FUEL` probe. Unsupported/denied vehicle data gracefully stays unavailable and does not interfere with the rest of the app.
- Fuel Assist scaffold: if Auto exposes fuel and a preferred station is set, the car screen can surface the preference when fuel is at/below 50%.
- Surveillance proximity alert v1: approximately 0.25 mi, ahead-of-travel filtering, 10-minute duplicate suppression.
- OSM contribution bridge opens the OSM editor at the current map center.
- Detailed About and Privacy dialogs.
- Local diagnostic logging, field notes, clear logs, and explicit ZIP export.

## Build on Windows

The least-confusing path is the included helper script. It uses Android Studio's JBR when available, discovers the normal Android SDK location, downloads **Gradle 8.13** locally, and builds the debug APK:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\build-debug.ps1
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Or open the directory in Android Studio and run `:app:assembleDebug`.

Requirements: Android SDK 36 and JDK 17+ (Android Studio's bundled JBR is fine).

## Suggested 0.4 field test

1. Install over the older alpha and grant foreground location.
2. Verify the OpenFreeMap dark vector style loads.
3. Drive with **Surveillance + Fuel** enabled and note marker readability, bearing, left/right descriptions, and alerts.
4. Search a familiar destination and verify the route puts you on the expected major roads/highway.
5. Deliberately miss one safe turn and see whether the simple off-route reroute behaves sensibly.
6. Save Home/Work and test the saved-route buttons.
7. Mark a known fuel station as preferred.
8. Connect Android Auto. Check whether BSA appears and whether the Vehicle row reports fuel as `available`, `probing`, or gracefully unavailable.
9. Use **Mark issue / field note** whenever something is weird.
10. Export the diagnostic ZIP after the drive.

## Privacy model

BSA itself has no account system, advertising SDK, BSA analytics backend, or BSA cloud telemetry in this alpha. Diagnostics stay local until explicitly exported.

This is not an offline-only app: network services necessarily receive the data required for requests. OpenFreeMap receives basemap requests; Overpass receives the queried area; Nominatim receives searches the user explicitly submits; OSRM receives route endpoints. Exact GPS logging in BSA diagnostic files is off by default.

## External-service caveats

The included endpoints are convenient development/public endpoints, not promises of production capacity. Nominatim search is intentionally submit-only (no autocomplete) and should remain within its public-use policy. The public OSRM demo service and public Overpass instances should be replaceable/self-hosted before any significant distribution.

## Known limitations

- No predictive traffic or live congestion.
- Maneuver language is intentionally basic.
- Side-of-travel is not yet verified driveway/road-side accessibility.
- Next Gas does not yet calculate driveway access, median crossings, or turn-count detour cost.
- Camera coverage sectors require usable bearing and range metadata and remain estimates.
- Android Auto is an awareness/POI surface in 0.4, not a full custom navigation-map renderer.
- Vehicle fuel/range availability depends entirely on what the Android Auto host and car expose.
- Nearby POI cache is currently session-memory oriented; persistent regional/offline data is a later milestone.
- `ALLOW_ALL_HOSTS_VALIDATOR` is used for Android Auto alpha testing. Tighten this before any production release.
