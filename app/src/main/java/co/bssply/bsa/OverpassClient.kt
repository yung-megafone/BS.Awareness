package co.bssply.bsa

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

object OverpassClient {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"
    private const val USER_AGENT = "BSAwareness/0.4 (OSM field-awareness proof of concept)"

    fun fetch(lat: Double, lon: Double, radiusMeters: Int = 4000): List<OsmPoi> {
        val query = """
            [out:json][timeout:25];
            (
              nwr(around:$radiusMeters,$lat,$lon)[man_made=surveillance];
              nwr(around:$radiusMeters,$lat,$lon)[surveillance];
              nwr(around:$radiusMeters,$lat,$lon)[amenity];
              nwr(around:$radiusMeters,$lat,$lon)[shop];
              nwr(around:$radiusMeters,$lat,$lon)[tourism];
            );
            out center tags;
        """.trimIndent()

        val body = "data=" + URLEncoder.encode(query, Charsets.UTF_8.name())
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", USER_AGENT)
        }

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) error("Overpass HTTP $code: ${text.take(180)}")

        val root = JSONObject(text)
        val elements = root.getJSONArray("elements")
        val result = ArrayList<OsmPoi>(elements.length())

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tagsObj = el.optJSONObject("tags") ?: continue
            val tags = mutableMapOf<String, String>()
            for (key in tagsObj.keys()) tags[key] = tagsObj.optString(key)

            val latValue: Double
            val lonValue: Double
            if (el.has("lat") && el.has("lon")) {
                latValue = el.getDouble("lat")
                lonValue = el.getDouble("lon")
            } else {
                val center = el.optJSONObject("center") ?: continue
                latValue = center.getDouble("lat")
                lonValue = center.getDouble("lon")
            }

            val kind = classify(tags)
            val fallback = when (kind) {
                OsmPoi.Kind.SURVEILLANCE -> "Surveillance object"
                OsmPoi.Kind.FOOD -> tags["brand"] ?: "Food"
                OsmPoi.Kind.FUEL -> tags["brand"] ?: "Fuel station"
                OsmPoi.Kind.SHOPPING -> tags["brand"] ?: tags["shop"]?.replace('_', ' ') ?: "Shop"
                OsmPoi.Kind.SERVICES -> tags["amenity"]?.replace('_', ' ') ?: "Service"
                OsmPoi.Kind.LODGING -> tags["brand"] ?: "Lodging"
                OsmPoi.Kind.OTHER -> tags["brand"] ?: tags["amenity"] ?: tags["shop"] ?: tags["tourism"] ?: "Mapped place"
            }

            result += OsmPoi(
                osmType = el.getString("type"),
                osmId = el.getLong("id"),
                lat = latValue,
                lon = lonValue,
                kind = kind,
                name = tags["name"] ?: fallback,
                tags = tags
            )
        }
        return result.distinctBy { "${it.osmType}/${it.osmId}" }
    }

    private fun classify(tags: Map<String, String>): OsmPoi.Kind {
        if (tags["man_made"] == "surveillance" || tags.containsKey("surveillance")) return OsmPoi.Kind.SURVEILLANCE
        if (tags["amenity"] == "fuel") return OsmPoi.Kind.FUEL
        if (tags["tourism"] in setOf("hotel", "motel", "hostel", "guest_house", "camp_site")) return OsmPoi.Kind.LODGING
        if (tags["amenity"] in setOf("restaurant", "fast_food", "cafe", "bar", "pub", "food_court", "ice_cream")) return OsmPoi.Kind.FOOD
        if (tags.containsKey("shop")) return OsmPoi.Kind.SHOPPING
        if (tags["amenity"] in setOf("bank", "atm", "pharmacy", "hospital", "clinic", "doctors", "dentist", "post_office", "police", "fire_station", "parking", "charging_station", "toilets")) return OsmPoi.Kind.SERVICES
        return OsmPoi.Kind.OTHER
    }
}
