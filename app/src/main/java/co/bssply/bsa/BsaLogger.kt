package co.bssply.bsa

import android.content.Context
import android.location.Location
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BsaLogger(private val context: Context) {
    private val dir = File(context.filesDir, "diagnostics").apply { mkdirs() }
    private val human = File(dir, "bsa.log")
    private val events = File(dir, "events.jsonl")
    private val notes = File(dir, "user-notes.txt")
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized fun event(type: String, message: String, fields: Map<String, Any?> = emptyMap()) {
        val now = System.currentTimeMillis()
        human.appendText("[${fmt.format(Date(now))}] ${type.uppercase(Locale.US)} $message\n")
        val json = JSONObject().put("ts", now).put("type", type).put("message", message)
        fields.forEach { (k, v) -> json.put(k, v ?: JSONObject.NULL) }
        events.appendText(json.toString() + "\n")
    }

    fun location(location: Location, includeExact: Boolean) {
        val f = linkedMapOf<String, Any?>(
            "accuracy_m" to if (location.hasAccuracy()) location.accuracy else null,
            "speed_mps" to if (location.hasSpeed()) location.speed else null,
            "bearing_deg" to if (location.hasBearing()) location.bearing else null
        )
        if (includeExact) { f["lat"] = location.latitude; f["lon"] = location.longitude }
        event("gps", "location update", f)
    }

    fun userNote(note: String) {
        notes.appendText("[${fmt.format(Date())}] $note\n")
        event("user_note", note)
    }

    fun clear() { dir.listFiles()?.forEach { it.delete() } }

    fun exportZip(includePreciseLocation: Boolean, includeVehicleValues: Boolean): File {
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val zip = File(outDir, "BSA-diagnostics-$stamp.zip")
        val diagnostics = JSONObject()
            .put("app", "B.S. Awareness")
            .put("version", BuildConfig.VERSION_NAME)
            .put("exported_at", System.currentTimeMillis())
            .put("precise_location_logging_enabled", includePreciseLocation)
            .put("vehicle_values_logging_enabled", includeVehicleValues)
            .put("note", "No data is uploaded by BSA. This archive was created only because the user explicitly exported it.")
        ZipOutputStream(zip.outputStream()).use { zos ->
            fun add(name: String, bytes: ByteArray) { zos.putNextEntry(ZipEntry(name)); zos.write(bytes); zos.closeEntry() }
            if (human.exists()) add("bsa.log", human.readBytes())
            if (events.exists()) add("events.jsonl", events.readBytes())
            if (notes.exists()) add("user-notes.txt", notes.readBytes())
            add("diagnostics.json", diagnostics.toString(2).toByteArray())
        }
        return zip
    }
}
