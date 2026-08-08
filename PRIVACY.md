# B.S. Awareness Privacy Statement — Alpha

B.S. Awareness is designed as a local-first application. This alpha contains no BSA account system, advertising SDK, BSA analytics backend, or BSA-operated cloud telemetry.

The application uses location locally for map positioning, POI relevance, routing, distance/direction calculations, saved-location features, and alerts. Diagnostics are stored on the device and remain there unless the user explicitly chooses to export/share a diagnostic archive. Precise coordinates are not intentionally written to diagnostic events by default.

BSA is not currently offline-only. To provide network-backed functions, third-party services necessarily receive request information: OpenFreeMap receives map-style/tile requests; Overpass receives geographic query areas; Nominatim receives place searches the user explicitly submits; OSRM receives route endpoints. These providers have their own privacy and usage policies.

Android Auto vehicle information is capability-gated. BSA probes supported car hardware only for features the app exposes. If fuel/range information is unsupported or permission is unavailable, BSA treats the value as absent and continues normally. BSA does not transmit vehicle telemetry to a BSA-operated server.

Because this is alpha software, inspect the source and diagnostic export before relying on it for sensitive workflows.
