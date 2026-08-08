package co.bssply.bsa

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: String? = null,
    val maneuverModifier: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)

data class BsaRoute(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val coordinates: List<Pair<Double, Double>>,
    val steps: List<RouteStep>,
    val destinationLabel: String,
    val destinationLat: Double,
    val destinationLon: Double
)

data class GeocodeResult(val label: String, val lat: Double, val lon: Double)
