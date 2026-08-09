package co.bssply.bsa

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.round

/**
 * Small local cache for Overpass responses.
 *
 * BSA deliberately snaps nearby requests to a coarse geographic grid. A 4 km
 * Overpass query therefore gets reused while the user pans/drives within the
 * same area instead of repeatedly asking the public Overpass service for almost
 * identical data.
 */
object PoiCache {
    private const val GRID_DEGREES = 0.02
    private const val FRESH_MS = 12L * 60L * 60L * 1000L
    private const val STALE_FALLBACK_MS = 7L * 24L * 60L * 60L * 1000L
    private const val MIN_NETWORK_INTERVAL_MS = 10_000L

    @Volatile private var lastNetworkRequestAt = 0L

    data class Result(
        val pois: List<OsmPoi>,
        val source: Source,
        val ageMs: Long = 0L,
        val queryLat: Double,
        val queryLon: Double
    )

    enum class Source { CACHE, NETWORK, STALE_CACHE }

    data class Stats(val files: Int, val bytes: Long, val oldestAgeMs: Long?, val newestAgeMs: Long?)

    fun fetch(context: Context, lat: Double, lon: Double, radiusMeters: Int = 4000, force: Boolean = false): Result {
        val qLat = snap(lat)
        val qLon = snap(lon)
        val file = cacheFile(context, qLat, qLon, radiusMeters)
        val now = System.currentTimeMillis()
        val cached = read(file)
        val age = cached?.first?.let { now - it } ?: Long.MAX_VALUE

        if (!force && cached != null && age <= FRESH_MS) {
            return Result(OverpassClient.parse(cached.second), Source.CACHE, age, qLat, qLon)
        }

        return try {
            throttleNetworkRequests()
            val raw = OverpassClient.fetchRaw(qLat, qLon, radiusMeters)
            write(file, now, raw)
            Result(OverpassClient.parse(raw), Source.NETWORK, 0L, qLat, qLon)
        } catch (t: Throwable) {
            if (cached != null && age <= STALE_FALLBACK_MS) {
                Result(OverpassClient.parse(cached.second), Source.STALE_CACHE, age, qLat, qLon)
            } else {
                throw t
            }
        }
    }

    fun clear(context: Context) {
        cacheDir(context).listFiles()?.forEach { it.delete() }
    }

    fun stats(context: Context): Stats {
        val now = System.currentTimeMillis()
        val files = cacheDir(context).listFiles()?.filter { it.isFile }.orEmpty()
        val ages = files.mapNotNull { read(it)?.first?.let { ts -> now - ts } }
        return Stats(
            files = files.size,
            bytes = files.sumOf { it.length() },
            oldestAgeMs = ages.maxOrNull(),
            newestAgeMs = ages.minOrNull()
        )
    }

    private fun throttleNetworkRequests() {
        synchronized(this) {
            val now = System.currentTimeMillis()
            val wait = MIN_NETWORK_INTERVAL_MS - (now - lastNetworkRequestAt)
            if (wait > 0) Thread.sleep(wait)
            lastNetworkRequestAt = System.currentTimeMillis()
        }
    }

    private fun snap(value: Double): Double = round(value / GRID_DEGREES) * GRID_DEGREES

    private fun cacheDir(context: Context) = File(context.cacheDir, "poi-cache").apply { mkdirs() }

    private fun cacheFile(context: Context, lat: Double, lon: Double, radius: Int): File {
        val key = String.format(Locale.US, "%.2f_%.2f_%d.json", lat, lon, radius)
        return File(cacheDir(context), key)
    }

    private fun write(file: File, timestamp: Long, raw: String) {
        val wrapper = JSONObject().put("cached_at", timestamp).put("overpass", JSONObject(raw))
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(wrapper.toString())
        if (!tmp.renameTo(file)) {
            file.writeText(wrapper.toString())
            tmp.delete()
        }
    }

    private fun read(file: File): Pair<Long, String>? = runCatching {
        if (!file.exists()) return null
        val wrapper = JSONObject(file.readText())
        wrapper.getLong("cached_at") to wrapper.getJSONObject("overpass").toString()
    }.getOrNull()
}
