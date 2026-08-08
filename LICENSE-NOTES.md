# Third-party data, libraries, and attribution

B.S. Awareness is intended to be open-source, but the project contains or interacts with components that have their own licenses and attribution requirements.

## OpenStreetMap

Map/POI data is © OpenStreetMap contributors and available under the Open Database License (ODbL). Keep visible attribution in map surfaces and comply with OSM service usage policies.

## OpenFreeMap / OpenMapTiles

The default map style is loaded from OpenFreeMap and is based on OpenStreetMap-derived OpenMapTiles data. Keep the OpenFreeMap/OpenStreetMap attribution visible and review OpenFreeMap's current terms/privacy policy before broad distribution.

## MapLibre Native

Map rendering uses MapLibre Native (`org.maplibre.gl:android-sdk-opengl`). Review the MapLibre license in the upstream project before redistribution.

## Lucide

Lucide is released under the ISC License. This alpha uses a tiny procedural Android-native stroke renderer that follows the Lucide visual language and credits Lucide; no icon font or runtime Lucide package is required. If the repository later vendors official Lucide SVG path data, preserve Lucide's ISC license notice with those assets.

Lucide project: https://lucide.dev/

## Android for Cars App Library

Android Auto integration uses AndroidX Car App Library artifacts distributed by Google/AndroidX under their applicable licenses.

## Public development services

The alpha defaults to public endpoints for Overpass, Nominatim, OpenFreeMap, and the OSRM demo router. These are network services, not bundled libraries. Their usage policies and capacity limitations apply independently of this repository's license. Any wider distribution should make endpoints configurable and/or use infrastructure intended for the resulting traffic.
