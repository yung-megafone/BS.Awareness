package co.bssply.bsa

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object OsrmClient {
    var baseUrl: String = "https://router.project-osrm.org"
    private const val USER_AGENT = "BSAwareness/0.4 (open-source personal navigation alpha)"

    fun route(startLat: Double, startLon: Double, endLat: Double, endLon: Double, destinationLabel: String): BsaRoute {
        val url = URL("$baseUrl/route/v1/driving/$startLon,$startLat;$endLon,$endLat?steps=true&overview=full&geometries=geojson&annotations=false")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 25_000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) error("Router HTTP $code")
        val root = JSONObject(text)
        if (root.optString("code") != "Ok") error(root.optString("message", "No route"))
        val route = root.getJSONArray("routes").getJSONObject(0)
        val coordsJson = route.getJSONObject("geometry").getJSONArray("coordinates")
        val coords = ArrayList<Pair<Double, Double>>(coordsJson.length())
        for (i in 0 until coordsJson.length()) {
            val p = coordsJson.getJSONArray(i)
            coords += p.getDouble(1) to p.getDouble(0)
        }
        val steps = mutableListOf<RouteStep>()
        val legs = route.getJSONArray("legs")
        for (li in 0 until legs.length()) {
            val stepArray = legs.getJSONObject(li).getJSONArray("steps")
            for (si in 0 until stepArray.length()) {
                val s = stepArray.getJSONObject(si)
                val m = s.getJSONObject("maneuver")
                val mod = m.optString("modifier").takeIf { it.isNotBlank() }
                val type = m.optString("type").takeIf { it.isNotBlank() }
                val road = s.optString("name").ifBlank { "road" }
                val instruction = instruction(type, mod, road)
                val loc = m.optJSONArray("location")
                steps += RouteStep(
                    instruction,
                    s.optDouble("distance", 0.0),
                    s.optDouble("duration", 0.0),
                    type,
                    mod,
                    loc?.optDouble(1),
                    loc?.optDouble(0)
                )
            }
        }
        return BsaRoute(route.getDouble("distance"), route.getDouble("duration"), coords, steps, destinationLabel, endLat, endLon)
    }

    private fun instruction(type: String?, modifier: String?, road: String): String {
        val direction = modifier?.replace('_', ' ')?.let { "$it " } ?: ""
        return when (type) {
            "depart" -> "Start on $road"
            "arrive" -> "Arrive at destination"
            "merge" -> "Merge $direction onto $road"
            "on ramp" -> "Take the ${direction}ramp to $road"
            "off ramp" -> "Take the ${direction}exit toward $road"
            "roundabout", "rotary" -> "Enter the roundabout toward $road"
            "continue" -> "Continue $direction on $road"
            "fork" -> "Keep $direction toward $road"
            "end of road" -> "Turn $direction onto $road"
            else -> "Turn $direction onto $road"
        }.replace("  ", " ").trim()
    }
}
