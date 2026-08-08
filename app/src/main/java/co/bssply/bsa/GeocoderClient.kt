package co.bssply.bsa

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object GeocoderClient {
    var baseUrl: String = "https://nominatim.openstreetmap.org"
    private const val USER_AGENT = "BSAwareness/0.4 (open-source personal navigation alpha)"

    fun searchOnce(query: String): List<GeocodeResult> {
        val q = URLEncoder.encode(query, Charsets.UTF_8.name())
        val url = URL("$baseUrl/search?q=$q&format=jsonv2&limit=5&addressdetails=0")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", USER_AGENT)
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) error("Search HTTP $code")
        val arr = JSONArray(text)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(GeocodeResult(o.getString("display_name"), o.getString("lat").toDouble(), o.getString("lon").toDouble()))
            }
        }
    }
}
