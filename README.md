# B.S. Awareness

> [!IMPORTANT]
> **PROJECT CANCELLED — August 11, 2026**
>
> B.S. Awareness is no longer under development.

B.S. Awareness (BSA) was an experimental, privacy-conscious driving-awareness application built around OpenStreetMap, MapLibre, routing, environmental POIs, surveillance/ALPR awareness, and Android Auto.

The project was created primarily to put this information on the **vehicle display**. The standalone Android mapping client was useful as the foundation for that goal, but it was not the reason for developing another mapping application.

## Why development stopped

BSA reached a functional Android Auto proof-of-concept. The project could build Android for Cars variants, run in Google's Desktop Head Unit, expose its `CarAppService`, and exercise POI/navigation-class Android Auto integration. Real-vehicle testing then exposed a platform restriction that conflicts with a core project requirement.

Android Auto's developer option for allowing unknown-source applications does **not** apply to applications built with the Android for Cars App Library. Consequently, a locally built/sideloaded Car App Library application cannot simply be enabled for ordinary use on a physical Android Auto head unit through the same unknown-sources development workflow available to some other Android Auto application categories.

BSA was deliberately intended to remain an open-source, locally installable project without requiring Google Play distribution or a Google developer account merely to run the software on the developer's own vehicle. Requiring that distribution relationship defeats that design goal.

Without the vehicle-display component, BSA becomes primarily another Android/OpenStreetMap mapping application. There are already mature projects serving that role, and duplicating them was not the objective of BSA.

**Development is therefore cancelled indefinitely.**

This repository is retained only as source history and a proof of concept. No further releases, compatibility work, bug fixes, support, or roadmap items should be expected. Anyone is free to study, fork, or continue the code subject to the repository's license.

## State at cancellation

At the time development stopped, BSA contained or had experimental groundwork for:

- MapLibre/OpenStreetMap vector mapping
- Local-first POI caching and stale-cache refresh behavior
- Fuel, food, business, parking, lodging, service, and miscellaneous POIs
- Surveillance-camera and ALPR awareness
- Direction/bearing-aware surveillance visualization
- Experimental estimated surveillance coverage
- Device location and heading display
- Destination search
- Experimental A→B routing and rerouting
- Saved-destination groundwork
- Next Gas / Fuel Assist experimentation
- Local diagnostics and field notes
- Android Auto POI and navigation application variants
- Android Auto vehicle-capability probing
- Debug/release signing and four-variant Android Auto test builds
- LAD (Lazy Ass Developer), a Windows development helper for building, signing, installing, and launching Android Auto/DHU test workflows

The Android Auto code should be regarded as experimental development code, not a supported vehicle application.

## Android Auto result

Four test variants were used to separate application category and build/signing variables:

```text
POI Debug
POI Release
NAV Debug
NAV Release
```

All four could be built and installed through ADB. Android Auto development through the Desktop Head Unit was functional. During physical-head-unit testing, the applications were not normally enumerated in the Android Auto launcher. A NAV Debug instance that had been used during DHU testing was observed binding to Android Auto, demonstrating that the Car App service itself could be recognized, but this did not make the application launcher-eligible.

The project was stopped after confirming the documented unknown-source limitation for Android for Cars App Library applications. No attempt will be made to circumvent Android Auto's trusted-source restrictions.

## Privacy philosophy

BSA was designed around local ownership of the application and its data. It did not require a BSA account, advertising system, analytics backend, telemetry backend, or cloud profile. Some online mapping, geocoding, POI, and routing functionality necessarily involved third-party services, and the project documentation intentionally distinguished those requests from data collected by BSA itself.

See [`PRIVACY.md`](PRIVACY.md) for the project's privacy documentation as it existed at cancellation.

## Building the archived source

The repository remains buildable source code rather than a maintained product. Development used Android Studio, the Android SDK, the included Gradle wrapper, and a suitable JDK. Build requirements may stop working as dependencies and Android tooling evolve.

No compatibility updates are planned.

## License

The source remains available under [`LICENSE`](LICENSE). Third-party components and geographic data retain their respective licenses and attribution requirements; see [`LICENSE-NOTES.md`](LICENSE-NOTES.md).

---

<details>
<summary><strong>Original alpha README / project documentation</strong></summary>

<p align="center">
  <img src="branding/bsa-wordmark.svg" alt="B.S. Awareness" width="520">
</p>

<p align="center">
  <strong>Open-source navigation and environmental awareness powered by OpenStreetMap.</strong>
</p>

<p align="center">
  Mapping • Navigation • POIs • Surveillance Awareness • Android Auto
</p>

<p align="center">
  <strong>Know what's around you.</strong>
</p>

---

# B.S. Awareness

**B.S. Awareness (BSA)** is a privacy-conscious, open-source mapping and navigation client built around OpenStreetMap.

BSA is designed around a simple idea:

> A navigation application can tell you more about the environment you're traveling through than merely how to reach your destination.

BSA combines ordinary points of interest—fuel, food, businesses, parking, lodging, and services—with mapped infrastructure such as surveillance cameras, automatic license plate readers, and traffic-monitoring equipment.

Rather than maintaining an isolated proprietary map, BSA uses the shared OpenStreetMap ecosystem and provides pathways for users to contribute verified corrections and additions back to the wider mapping community.

BSA is currently an **experimental alpha project under active development**.

> [!WARNING]
> BSA should not be relied upon as the sole source of navigation, road conditions, surveillance information, emergency information, or other safety-critical data.
>
> OpenStreetMap data may be incomplete, inaccurate, outdated, or incorrectly positioned.

---

## Project Status

**Current release: `v0.4.2-alpha`**

BSA began as a proof-of-concept for displaying OpenStreetMap surveillance nodes on an Android device.

It has since expanded into a broader navigation and environmental-awareness project.

The current development focus includes:

- Dark-first vector mapping
- Everyday OpenStreetMap POIs
- Surveillance and ALPR mapping
- Basic A→B navigation
- Route-aware environmental information
- Android Auto integration
- Vehicle capability detection
- Fuel-assistance experimentation
- OpenStreetMap contribution tools
- Privacy-conscious field diagnostics

The `0.4.x` development cycle is focused primarily on **field testing, stability, Android Auto compatibility, routing reliability, map performance, and bug fixes**.

Expect bugs.

---

## Features

### Dark-First Vector Mapping

BSA uses a dark vector basemap rendered with MapLibre.

Unlike a darkened raster image, vector mapping provides control over individual geographic layers and gives BSA a foundation for displaying:

- Roads
- Buildings
- Labels
- Land
- Water
- POIs
- Routes
- Surveillance equipment
- Directional markers
- Coverage estimates
- Custom BSA overlays

Dark mode is the default BSA experience.

---

### OpenStreetMap POIs

BSA can retrieve and display OpenStreetMap points of interest surrounding the user.

Supported categories currently include:

- Surveillance
- Fuel
- Food
- Shopping
- Parking and services
- Lodging
- Miscellaneous POIs

Filters can be independently enabled or disabled.

The goal is to allow the user to decide what information deserves space on the map.

Someone interested only in navigation and fuel should not be forced to display surveillance equipment.

Someone interested in infrastructure mapping should not be forced to display every restaurant in town.

---

### Surveillance Awareness

Surveillance infrastructure is treated as a first-class geographic object rather than a generic map pin.

Where available in OpenStreetMap, BSA can interpret metadata including:

- Surveillance type
- Camera type
- Surveillance zone
- Camera direction or bearing
- Operator
- Agency
- Manufacturer
- OSM object identity
- Additional raw OSM tags

For agency/operator information, BSA supports conventional OSM `operator=*` information while also recognizing `agency=*` where present.

---

### Directional Camera Visualization

Where a camera bearing is known, BSA can use that information to represent the direction in which surveillance equipment is oriented.

Camera objects may eventually distinguish between states such as:

```text
Camera mapped
Camera bearing known
Estimated coverage available
```

Directional information is only as accurate as the underlying data.

---

### Estimated Surveillance Coverage

BSA contains experimental support for estimating surveillance coverage geometry.

An estimate may consider information such as:

- Camera coordinates
- Camera bearing
- Approximate field of view
- Advertised or estimated recognition distance
- Observed installation characteristics

These visualizations are **estimates only**.

A rendered coverage area does not mean that every person, vehicle, plate, or object inside that area is necessarily visible, recorded, identifiable, or successfully recognized.

Real-world performance can be affected by installation angle, lens configuration, lighting, weather, obstruction, vehicle position, camera configuration, and other factors.

---

## Location Awareness

BSA displays the location reported by the Android device directly on the map.

Location-marker styles can include:

- Navigation arrow
- High-contrast position marker
- Vehicle-style marker

Where available, GPS bearing can be used to indicate direction of travel.

Future versions will continue improving follow-mode and driving-oriented map behavior.

---

## Search

BSA includes place and destination search.

Search is intended to provide access to ordinary geographic destinations while respecting the usage requirements of external geocoding services.

BSA does not depend on continuous search autocomplete to function.

---

# Navigation

## Basic A→B Routing

BSA includes experimental automobile routing.

The current navigation system is intended to provide the fundamentals:

1. Select a destination.
2. Calculate a road route.
3. Display the route on the map.
4. Show route distance and estimated travel time.
5. Retrieve maneuver information.
6. Track progress using device location.
7. Recalculate when appropriate.

BSA does **not** currently attempt to replicate commercial predictive traffic systems.

Current routing does not provide:

- Historical traffic modeling
- Predictive congestion
- Traffic-aware ETA intelligence
- Commercial navigation traffic feeds

The immediate objective is much simpler:

> Reliably get from A to B using the road network.

---

## Saved Destinations

BSA supports the groundwork for frequently used destinations such as:

- Home
- Work
- Frequently visited locations

Saved destinations are intended to make common routes faster to initiate and provide context for future route-aware features.

---

# Next Gas

BSA includes an experimental **Next Gas** feature.

The long-term goal is not merely to answer:

> What gas station is geographically closest?

Instead, BSA aims to eventually answer:

> What useful gas station can I reach without a ridiculous detour?

Future routing logic may consider:

- Whether the station is ahead
- Route detour distance
- Number of turns
- U-turn requirements
- Divided highways
- Side-of-road accessibility
- Direct driveway access
- Frontage roads
- User-defined tolerances

The current implementation is intentionally simpler while the routing architecture matures.

---

# Fuel Assist

BSA includes experimental groundwork for optional vehicle-aware fuel assistance.

Potential Fuel Assist behavior includes:

- Preferred fuel stations
- User-defined fuel thresholds
- Saved destination context
- Route-aware station suggestions
- Remaining-range awareness
- Optional fuel-stop recommendations

BSA does **not** assume that vehicle fuel information is available.

---

## Vehicle Data

When connected through Android Auto, BSA can attempt to access vehicle information exposed through Android's Car Hardware APIs.

Potential information includes:

- Fuel percentage
- Remaining range
- Low-fuel state

Vehicle support varies.

BSA treats every vehicle capability as optional.

If the vehicle does not expose a requested value, BSA should gracefully treat the value as unavailable and continue operating normally.

For example:

```text
Android Auto       Connected
Fuel capability    Unavailable
Fuel level         Unavailable
Remaining range    Unavailable
```

is a valid state.

Unsupported vehicle hardware should never prevent ordinary mapping or navigation.

---

# Android Auto

BSA includes experimental Android Auto integration.

The Android Auto interface is intended to function as a simplified **driving-awareness surface**, not as a mirror of the complete phone interface.

Development goals include displaying:

- Current navigation state
- Nearby enabled POIs
- Fuel stations
- Surveillance infrastructure
- Distance and direction information
- Route-aware POIs
- Surveillance proximity information
- Future Fuel Assist suggestions

Android Auto support is currently experimental.

Compatibility may vary by:

- Vehicle
- Head unit
- Android version
- Android Auto version
- Car App Library support
- Available vehicle hardware APIs

Real-world testing is an important part of the `0.4.x` development cycle.

---

# Surveillance Alerts

BSA contains groundwork for optional surveillance proximity alerts.

Potential controls include:

- Alert distance
- Ahead-of-travel filtering
- Surveillance-type filters
- Repeat-alert suppression
- Phone display
- Android Auto display
- Audible alerts

Future versions may improve alert relevance using:

- Active route geometry
- Road association
- Camera bearing
- Estimated camera coverage
- Direction of travel

The objective is useful awareness rather than constant notification spam.

---

# OpenStreetMap Contribution

BSA is intentionally built around a **shared geographic dataset**.

If a camera, business, fuel station, or other object is missing or incorrectly positioned, correcting OpenStreetMap can improve the map for more than just BSA.

BSA therefore provides a pathway back to OpenStreetMap for editing or contributing geographic information.

The current project does not attempt to implement an entire native OSM editor.

Native changeset creation and more advanced contribution workflows may be explored later.

---

# Field Testing & Diagnostics

Real-world mapping and navigation bugs are often difficult to reproduce from memory alone.

BSA therefore includes a local diagnostic system intended specifically for alpha testing.

Diagnostics may record information such as:

- BSA version/build
- Application events
- Errors and exceptions
- GPS capability and accuracy
- OSM query performance
- POI counts
- Cache behavior
- Routing requests
- Routing duration
- Rerouting events
- Android Auto connection state
- Vehicle capability availability
- Performance timings

---

## Field Notes

BSA can associate human-readable field observations with diagnostic information.

Examples include:

```text
Camera marker appears on wrong side of roadway.
```

```text
POIs loaded unusually slowly here.
```

```text
Next Gas selected a station behind direction of travel.
```

```text
Android Auto disconnected unexpectedly.
```

These notes can provide useful context when investigating behavior later.

---

## Diagnostic Export

BSA can export diagnostic information for troubleshooting.

A diagnostic bundle may contain files such as:

```text
BSA-diagnostics-YYYYMMDD-HHMMSS.zip

├── bsa.log
├── events.jsonl
├── diagnostics.json
└── user-notes.txt
```

Diagnostic exports are intended to support both human inspection and structured analysis.

---

# Privacy

## Local-First by Design

BSA is designed as a **local-first application**.

BSA itself does not require:

- A BSA account
- Advertising
- A BSA analytics service
- A BSA telemetry backend
- A BSA cloud profile

Application preferences and diagnostic information are intended to remain on the device unless the user explicitly exports or shares them.

---

## Location

BSA requires location information for features such as:

- Current-position display
- Navigation
- Nearby POI calculations
- Route progress
- Direction-of-travel calculations
- Proximity alerts

BSA does not claim that location information **never leaves the device**.

Some functionality relies on external mapping, POI, geocoding, or routing services. When an online service must calculate a route or answer a geographic query, the coordinates necessary to perform that request may be transmitted to that service.

BSA aims to clearly distinguish its own behavior from the behavior of external infrastructure providers.

---

## Vehicle Information

Vehicle information obtained through Android Auto is intended only for enabled application features.

Potential values may include:

- Fuel level
- Remaining range
- Low-fuel state

Unsupported values are treated as unavailable.

BSA does not operate a BSA server for collecting vehicle telemetry.

---

## Diagnostic Privacy

Diagnostic logging is designed to minimize unnecessary sensitive information.

Sensitive diagnostic fields can be independently controlled.

Examples include:

```text
Diagnostic logging             ON
Performance events             ON
Errors                         ON
Vehicle capability status      ON

Precise GPS in diagnostic log  OFF
Route geometry in log          OFF
Vehicle values in log          OFF
```

Actual vehicle values should not be logged simply because BSA probes whether the capability exists.

Before sharing diagnostic information, users should review what the export contains.

See [`PRIVACY.md`](PRIVACY.md) for additional information.

---

# External Services

BSA depends on open mapping infrastructure for several online features.

Depending on configuration and feature use, this may include:

| Service | Purpose |
|---|---|
| OpenStreetMap | Geographic and POI data |
| OpenFreeMap | Vector basemap |
| Overpass API | OSM feature queries |
| Nominatim | Place/destination geocoding |
| OSRM | Road routing |
| Android Auto | Vehicle display integration |

These services are independent of B.S. Awareness and may operate under their own privacy policies, licenses, availability guarantees, and usage requirements.

---

# Branding

<p align="center">
  <img src="branding/bsa-mark-transparent.svg" alt="BSA Mark" width="160">
</p>

The B.S. Awareness mark combines two concepts central to the project.

### Navigation

The directional arrow represents movement, routing, and geographic orientation.

### Awareness

The surrounding eye represents awareness of the environment beyond the route itself.

Together, the mark represents the central BSA concept:

> **Know what's around you.**

The BSA interface is designed to visually complement the minimalist icon language used by Lucide while maintaining an independent project identity.

Brand assets are located in:

```text
branding/
├── bsa-app-icon.svg
├── bsa-mark-transparent.svg
├── bsa-monochrome.svg
└── bsa-wordmark.svg
```

---

# Technology

BSA is built using open-source software and open geographic ecosystems.

Major components include:

### OpenStreetMap

Provides the shared geographic database underlying POIs, surveillance objects, roads, businesses, and other mapped infrastructure.

### MapLibre

Provides native map rendering within the Android application.

### OpenFreeMap

Provides the vector basemap used by the current dark-first map experience.

### Lucide

Provides the visual foundation for BSA's minimalist interface iconography.

Custom BSA symbols are used where ordinary UI icons cannot adequately represent GIS-specific concepts such as directional surveillance equipment and coverage geometry.

### OSRM

Provides the current experimental road-routing backend.

### Android for Cars App Library

Provides the framework used for BSA's Android Auto integration.

See [`LICENSE-NOTES.md`](LICENSE-NOTES.md) for licensing and attribution information.

---

# Building BSA

## Requirements

Development currently assumes:

- Android Studio
- Android SDK
- JDK 17 or newer
- Gradle wrapper included with the project
- Android device or emulator

Android Studio's bundled JBR can be used as the Java runtime.

On Windows PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Verify:

```powershell
java -version
```

Then build:

```powershell
.\gradlew.bat clean assembleDebug
```

A successful debug build will normally produce:

```text
app\build\outputs\apk\debug\app-debug.apk
```

---

## Installing Development Builds

BSA is currently distributed primarily as source/development builds.

Android may display additional warnings when installing manually built or sideloaded APKs.

The developer is responsible for signing and installing builds appropriately for their device.

Android Auto may require additional developer/testing configuration before sideloaded applications appear on a vehicle display.

---

# Repository Structure

A simplified project layout:

```text
BSA/
├── app/
│   └── src/
│       └── main/
│           ├── java/co/bssply/bsa/
│           └── res/
│
├── branding/
│   ├── bsa-app-icon.svg
│   ├── bsa-mark-transparent.svg
│   ├── bsa-monochrome.svg
│   └── bsa-wordmark.svg
│
├── gradle/
├── README.md
├── PRIVACY.md
├── CHANGELOG.md
├── LICENSE
├── LICENSE-NOTES.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── gradlew.bat
```

---

# Development Roadmap

## Current — 0.4.x

Primary focus:

- Field testing
- Build reliability
- Android Auto compatibility
- Vehicle capability testing
- Routing reliability
- Map performance
- POI behavior
- Surveillance visualization
- Diagnostics
- UI refinement

Minor releases such as `0.4.1`, `0.4.2`, and later revisions should prioritize fixing and refining the existing architecture rather than continuously adding major new systems.

---

## Future Development

Potential future work includes:

- Improved route-aware POI selection
- Better Next Gas scoring
- Road-side accessibility analysis
- Turn-count and detour penalties
- Divided-highway awareness
- Driveway/access-road analysis
- Improved camera coverage geometry
- Native OSM contribution workflows
- Offline geographic regions
- Offline routing
- Improved POI clustering
- Better Android Auto navigation
- Additional vehicle-data integrations
- Optional alternative routing providers

These items are not promises or guaranteed release targets.

---

# Historical Builds

Early BSA builds represent the project's proof-of-concept phase.

Suggested release history:

```text
v0.1.x-pre-alpha
Initial Android / MapLibre / OSM proof of concept

v0.2.0-pre-alpha
POI and filtering experiments

v0.3.0-pre-alpha
Dark-interface and location-awareness prototype

v0.4.0-alpha
Navigation & Awareness architecture

v0.4.1-alpha
Current development alpha
```

Historical binary/source archives are better preserved through GitHub Releases than committed directly into the Git source history.

---

# Contributing

BSA is an open-source project and contributions are welcome.

Useful contributions can include:

- Bug reports
- Android compatibility testing
- Android Auto testing
- MapLibre improvements
- Routing improvements
- UI/UX improvements
- Performance optimization
- Documentation
- Privacy review
- Accessibility improvements
- OpenStreetMap interoperability
- New POI handling
- Vehicle compatibility reports

When reporting a bug, useful information includes:

- BSA version
- Android version
- Device model
- Whether Android Auto was connected
- What you expected
- What actually happened
- Steps to reproduce
- Relevant diagnostic export, if comfortable sharing it

**Do not publish sensitive diagnostic information without reviewing it first.**

---

# OpenStreetMap Contributions

If contributing geographic information discovered while using BSA, contributors should follow OpenStreetMap's tagging conventions and community standards.

Distinguish between:

- Verified observations
- Existing OSM data
- Manufacturer specifications
- Estimates
- Inferences

Do not represent estimates as verified geographic facts.

BSA-specific derived information does not automatically belong in OpenStreetMap.

---

# Disclaimer

B.S. Awareness is experimental software.

Information displayed by BSA may be incomplete, inaccurate, delayed, estimated, or outdated.

This includes, but is not limited to:

- Road geometry
- Navigation instructions
- Business information
- POI locations
- Surveillance locations
- Camera direction
- Camera ownership/operator information
- Estimated surveillance coverage
- Vehicle information
- Fuel information
- Remaining range
- Route accessibility

Always obey traffic laws and operate vehicles safely.

Do not interact with the application in a manner that distracts from driving.

BSA is not affiliated with OpenStreetMap, MapLibre, OpenFreeMap, Lucide, OSRM, Google, Android Auto, vehicle manufacturers, surveillance-equipment manufacturers, or government agencies unless explicitly stated otherwise.

Product and organization names may be trademarks of their respective owners.

---

# License & Attribution

BSA is open-source software.

See [`LICENSE`](LICENSE) for the project's software license.

Third-party components, geographic datasets, map styles, iconography, and services may have separate licenses and attribution requirements.

See:

[`LICENSE-NOTES.md`](LICENSE-NOTES.md)

for third-party licensing and attribution information.

OpenStreetMap data is © OpenStreetMap contributors and is subject to the applicable OpenStreetMap licensing requirements.

---

<p align="center">
  <img src="branding/bsa-mark-transparent.svg" alt="B.S. Awareness" width="72">
</p>

<p align="center">
  <strong>B.S. Awareness</strong><br>
  <em>Know what's around you.</em>
</p>

<p align="center">
  Open-source mapping • Navigation • Environmental awareness
</p>


</details>
