package co.bssply.bsa

import java.util.Locale

data class OsmPoi(
    val osmType: String,
    val osmId: Long,
    val lat: Double,
    val lon: Double,
    val kind: Kind,
    val name: String,
    val tags: Map<String, String>
) {
    enum class Kind { SURVEILLANCE, FOOD, FUEL, SHOPPING, SERVICES, LODGING, OTHER }

    val operator: String?
        get() = tags["operator"] ?: tags["agency"] ?: tags["owner"]

    val cameraBearing: Float?
        get() {
            val raw = tags["camera:direction"] ?: tags["direction"] ?: return null
            return raw.toFloatOrNull()?.let { ((it % 360f) + 360f) % 360f }
        }

    val estimatedRangeMeters: Double?
        get() {
            val raw = tags["bsa:range_m"] ?: tags["camera:range"] ?: tags["range"] ?: return null
            val n = Regex("-?[0-9]+(?:\\.[0-9]+)?").find(raw)?.value?.toDoubleOrNull() ?: return null
            return when {
                raw.contains("ft", true) || raw.contains("feet", true) -> n * 0.3048
                raw.contains("mi", true) -> n * 1609.344
                else -> n
            }
        }

    fun categoryLabel(): String = when (kind) {
        Kind.SURVEILLANCE -> when {
            tags["surveillance:type"]?.contains("alpr", true) == true -> "ALPR camera"
            tags["camera:type"] != null -> "Surveillance camera"
            else -> "Surveillance"
        }
        Kind.FOOD -> "Food & drink"
        Kind.FUEL -> "Fuel"
        Kind.SHOPPING -> "Shopping"
        Kind.SERVICES -> "Service"
        Kind.LODGING -> "Lodging"
        Kind.OTHER -> "Place"
    }

    fun humanSummary(): String {
        val bits = mutableListOf<String>()
        when (kind) {
            Kind.SURVEILLANCE -> {
                tags["surveillance:type"]?.let { bits += it.uppercase(Locale.US) }
                tags["surveillance:zone"]?.let { bits += "${it.replace('_', ' ')} zone" }
                tags["camera:type"]?.let { bits += "${it.replace('_', ' ')} camera" }
                operator?.let { bits += it }
                cameraBearing?.let { bits += "bearing ${it.toInt()}°" }
            }
            Kind.FOOD -> bits += (tags["cuisine"]?.replace(';', '•') ?: tags["amenity"]?.replace('_', ' ') ?: "food")
            Kind.FUEL -> bits += "fuel station"
            Kind.SHOPPING -> bits += (tags["shop"]?.replace('_', ' ') ?: "shop")
            Kind.SERVICES -> bits += (tags["amenity"]?.replace('_', ' ') ?: "service")
            Kind.LODGING -> bits += (tags["tourism"]?.replace('_', ' ') ?: "lodging")
            Kind.OTHER -> bits += (tags["amenity"] ?: tags["shop"] ?: tags["tourism"] ?: "mapped place").replace('_', ' ')
        }
        tags["brand"]?.takeIf { it != name }?.let { bits += it }
        return bits.distinct().joinToString(" • ")
    }

    fun rawTags(): String = tags.entries.sortedBy { it.key }.joinToString("  •  ") { "${it.key}=${it.value}" }
}
