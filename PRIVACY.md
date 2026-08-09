# Privacy Policy

**Effective:** August 2026

---

# Overview

B.S. Awareness ("BSA") is an open-source, privacy-conscious navigation and environmental awareness application.

BSA is designed around a simple principle:

> **Collect only the information necessary to perform the features you ask it to perform.**

BSA does **not** operate:

- User accounts
- Cloud synchronization
- Advertising networks
- Analytics services
- Telemetry collection servers
- User profiles

Unless you explicitly export or share information yourself, application data is intended to remain on your own device.

---

# Information Stored Locally

BSA stores certain information locally to provide its functionality.

Depending on enabled features, this may include:

- Application settings
- Enabled POI filters
- Saved destinations
- Cached OpenStreetMap data
- Cached map regions
- Diagnostic logs (if enabled)
- User field notes (if created)

This information is stored on your device and is not automatically transmitted to B.S. Awareness.

---

# Location Information

Navigation software requires access to location information in order to function.

When location permission has been granted, BSA may use your location to:

- Display your current position
- Calculate navigation routes
- Display nearby points of interest
- Determine direction of travel
- Calculate proximity alerts
- Track navigation progress

BSA does **not** operate its own location tracking servers.

However, some features depend upon third-party services.

For example:

- Route calculation
- Destination search
- Geocoding
- OpenStreetMap feature queries

When you request one of these online features, the geographic information necessary to answer that request may be transmitted to the external service responsible for fulfilling it.

BSA cannot control how those independent services process or retain requests.

---

# External Services

Depending on which features you use, BSA may communicate with external services such as:

| Service | Purpose |
|----------|---------|
| OpenStreetMap | Geographic data |
| OpenFreeMap | Vector map tiles |
| Overpass API | OpenStreetMap feature queries |
| Nominatim | Destination search / geocoding |
| OSRM | Route calculation |
| Android Auto | Vehicle display integration |

These services are independent from B.S. Awareness and operate under their own privacy policies and terms of service.

---

# Android Auto

When connected to Android Auto, BSA may request vehicle capabilities exposed by Android's Car Hardware APIs.

Depending on the vehicle and Android Auto host, this may include:

- Fuel level
- Remaining driving range
- Low-fuel status

Vehicle support varies.

If a requested capability is unavailable, BSA simply treats it as unavailable and continues operating normally.

Vehicle information is used only for features you choose to use.

BSA does not upload vehicle telemetry to any BSA-operated service.

---

# Diagnostic Logging

BSA includes optional diagnostic logging to assist with troubleshooting and development.

When enabled, diagnostic information may include:

- Application events
- Errors and exceptions
- Performance timings
- POI query statistics
- Cache behavior
- Routing events
- Android Auto connection state
- Vehicle capability availability

Diagnostic logs remain on your device unless you explicitly export them.

Before sharing exported diagnostics, users should review their contents.

---

# Permissions

BSA requests only the permissions necessary for the features you choose to use.

### Location

Used for:

- Navigation
- Current location display
- Nearby POI calculations
- Route tracking

### Internet

Used for:

- Downloading map tiles
- Route calculation
- Destination search
- OpenStreetMap queries

Future versions may request additional permissions only when required for optional functionality.

---

# Data Sharing

B.S. Awareness does **not**:

- Sell personal information
- Operate advertising services
- Build advertising profiles
- Require user accounts
- Synchronize data to BSA-operated servers
- Automatically upload diagnostic information

Information leaves your device only when:

- You request an online feature that depends on an external service, or
- You explicitly export information yourself.

---

# Open Source

B.S. Awareness is open source.

Anyone may inspect the source code to verify how information is handled.

Transparency is considered an important part of the project's privacy philosophy.

---

# Changes

As BSA evolves, this Privacy Policy may be updated to reflect new functionality.

If future features require additional permissions or communication with external services, this policy will be updated accordingly.

---

# Contact

Questions, bug reports, privacy concerns, and feature requests may be submitted through the project's GitHub repository.

---

# Plain-English Summary

If you skipped everything above, here's the short version.

BSA itself does not operate accounts, advertising, cloud synchronization, analytics, or telemetry servers.

Most information remains on your device.

Some features—such as routing, destination search, or OpenStreetMap queries—necessarily communicate with the third-party services that provide those functions.

Those requests are a consequence of using those online services, not because BSA is collecting or monetizing your information.

Whenever practical, BSA is designed to favor local storage, local processing, and user control over cloud infrastructure.
