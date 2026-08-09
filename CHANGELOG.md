# Changelog

## 0.4.2-alpha
- Fixed Android Auto startup by declaring application-level `androidx.car.app.minCarApiLevel`.
- Added local geographic POI caching with a 12-hour freshness window and stale-cache fallback.
- Added a 10-second minimum interval between public Overpass network requests to avoid hammering shared infrastructure.
- Snapped nearby POI queries to reusable geographic cells so small map movements reuse cached responses.
- Added POI cache source/status logging and cache statistics in Diagnostics.
- Added manual Refresh POIs and Clear POI cache controls in Settings.
- Map taps now dismiss open Layers/Settings panels.
- Removed the translucent right-side toolbar tray so controls float independently over the map.
- Removed the outline from floating map buttons and added elevation for a cleaner floating-control treatment.
- Reworked the Settings glyph so it reads as a gear rather than a locate/crosshair icon.

## 0.4.0-alpha

- Switched default basemap to OpenFreeMap Dark vector style.
- Added destination search and basic OSRM road routing with reroute heuristic.
- Added saved Home/Work destinations.
- Added automatic OSM POI loading.
- Added persistent layer/settings state.
- Added operator/agency display for surveillance POIs.
- Added numeric camera-bearing visualization and optional estimated coverage sectors.
- Added Android Auto awareness list and graceful CAR_FUEL capability probe.
- Added preferred-fuel station scaffold and Fuel Assist state.
- Added local diagnostics, field notes, ZIP export, About, and Privacy screens.
- Reworked toolbar/icon presentation around a compact stroke-icon language.

## 0.4.1-alpha — branding patch
- Added BSA eye/navigation brand system.
- Added Android adaptive, round, legacy and monochrome launcher icons.
- Added in-app header brand mark.
- Added reusable SVG wordmark, mark and monochrome branding sources.
