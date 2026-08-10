package co.bssply.bsa

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BsaCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(sessionInfo: SessionInfo): Session = BsaCarSession()
}

class BsaCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = BsaCarHomeScreen(carContext)
}

class BsaCarHomeScreen(carContext: CarContext) : Screen(carContext) {
    private val logger = BsaLogger(carContext)
    private val prefs = carContext.getSharedPreferences("bsa_prefs", android.content.Context.MODE_PRIVATE)
    private var carInfo: CarInfo? = null
    private var energyListener: OnCarDataAvailableListener<EnergyLevel>? = null
    private val navMode = BuildConfig.CAR_APP_MODE == "navigation"
    private var navSurfaceRenderer: BsaNavSurfaceRenderer? = null

    init {
        VehicleState.androidAutoConnected = true
        logger.event(
            "car",
            "Android Auto connected",
            mapOf(
                "car_app_api_level" to carContext.carAppApiLevel,
                "car_app_mode" to BuildConfig.CAR_APP_MODE
            )
        )

        if (navMode) {
            navSurfaceRenderer = BsaNavSurfaceRenderer(carContext)
            carContext.getCarService(AppManager::class.java)
                .setSurfaceCallback(navSurfaceRenderer)
            logger.event("car", "Navigation test surface callback registered")
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                stopFuelProbe()
                VehicleState.androidAutoConnected = false
                VehicleState.clearVehicleData()
                VehicleState.fuelCapability = "disconnected"
                logger.event("car", "Android Auto disconnected")
            }
        })

        probeFuel()
    }

    private fun probeFuel() {
        logger.event(
            "car_fuel",
            "Probing EnergyLevel",
            mapOf("car_app_api_level" to carContext.carAppApiLevel)
        )

        if (carContext.carAppApiLevel < 3) {
            VehicleState.fuelCapability = "unsupported: Car App API < 3"
            logger.event("car_fuel", "EnergyLevel unsupported: Car App API < 3")
            return
        }

        try {
            val manager = carContext.getCarService(CarHardwareManager::class.java)
            val info = manager.carInfo
            carInfo = info

            val listener = OnCarDataAvailableListener<EnergyLevel> { energy ->
                val fuel = successValue(energy.fuelPercent)
                val range = successValue(energy.rangeRemainingMeters)
                val low = successValue(energy.energyIsLow)

                VehicleState.fuelPercent = fuel
                VehicleState.remainingRangeMeters = range
                VehicleState.lowFuel = low
                VehicleState.lastUpdatedAtMillis = System.currentTimeMillis()

                val anyValueAvailable = fuel != null || range != null || low != null
                VehicleState.fuelCapability = if (anyValueAvailable) {
                    "available"
                } else {
                    "listener active; values unavailable"
                }

                val fields = linkedMapOf<String, Any?>(
                    "fuel_status" to statusName(energy.fuelPercent.status),
                    "range_status" to statusName(energy.rangeRemainingMeters.status),
                    "low_fuel_status" to statusName(energy.energyIsLow.status),
                    "fuel_value_available" to (fuel != null),
                    "range_value_available" to (range != null),
                    "low_fuel_value_available" to (low != null)
                )

                val prefs = carContext.getSharedPreferences("bsa_prefs", android.content.Context.MODE_PRIVATE)
                if (prefs.getBoolean("log_vehicle_values", false)) {
                    fields["fuel_percent"] = fuel
                    fields["remaining_range_m"] = range
                    fields["low_fuel"] = low
                }

                logger.event("car_fuel", "EnergyLevel callback received", fields)
                invalidate()
            }

            energyListener = listener
            info.addEnergyLevelListener(carContext.mainExecutor, listener)
            VehicleState.fuelCapability = "listener registered; awaiting data"
            logger.event("car_fuel", "EnergyLevel listener registered")
        } catch (security: SecurityException) {
            VehicleState.fuelCapability = "permission denied / host did not grant CAR_FUEL"
            logger.event(
                "car_fuel",
                "EnergyLevel permission denied",
                mapOf("exception" to security.javaClass.simpleName)
            )
        } catch (t: Throwable) {
            VehicleState.fuelCapability = "unavailable (${t.javaClass.simpleName})"
            logger.event(
                "car_fuel",
                "EnergyLevel probe failed",
                mapOf(
                    "exception" to t.javaClass.simpleName,
                    "message" to t.message
                )
            )
        }
    }

    private fun stopFuelProbe() {
        val listener = energyListener ?: return
        try {
            carInfo?.removeEnergyLevelListener(listener)
            logger.event("car_fuel", "EnergyLevel listener removed")
        } catch (t: Throwable) {
            logger.event(
                "car_fuel",
                "EnergyLevel listener removal failed",
                mapOf("exception" to t.javaClass.simpleName)
            )
        } finally {
            energyListener = null
            carInfo = null
        }
    }

    private fun <T> successValue(value: CarValue<T>): T? =
        if (value.status == CarValue.STATUS_SUCCESS) value.value else null

    private fun statusName(status: Int): String = when (status) {
        CarValue.STATUS_SUCCESS -> "success"
        CarValue.STATUS_UNIMPLEMENTED -> "unimplemented"
        CarValue.STATUS_UNAVAILABLE -> "unavailable"
        else -> "status_$status"
    }

    override fun onGetTemplate(): Template {
        val loc = AppState.location
        val pois = visiblePois(loc)

        return if (navMode) {
            buildNavigationTemplate(loc, pois)
        } else {
            buildPoiTemplate(loc, pois)
        }
    }

    private fun visiblePois(loc: android.location.Location?): List<OsmPoi> {
        if (loc == null) return emptyList()
        return AppState.pois
            .asSequence()
            .filter(::isPoiEnabled)
            .sortedBy { poi -> distanceMeters(loc.latitude, loc.longitude, poi.lat, poi.lon) }
            .take(6)
            .toList()
    }

    private fun buildPoiTemplate(
        loc: android.location.Location?,
        pois: List<OsmPoi>
    ): Template {
        val list = ItemList.Builder()

        pois.forEach { poi ->
            val meters = distanceMeters(loc!!.latitude, loc.longitude, poi.lat, poi.lon)
            val place = Place.Builder(CarLocation.create(poi.lat, poi.lon))
                .setMarker(markerFor(poi))
                .build()

            list.addItem(
                Row.Builder()
                    .setTitle(poiTitle(poi))
                    .addText(distanceText(meters, poi.humanSummary()))
                    .setMetadata(Metadata.Builder().setPlace(place).build())
                    .setOnClickListener {
                        logger.event(
                            "car_poi",
                            "POI selected",
                            mapOf("osm_type" to poi.osmType, "osm_id" to poi.osmId, "kind" to poi.kind.name)
                        )
                    }
                    .build()
            )
        }

        val template = PlaceListMapTemplate.Builder()
            .setTitle("BSA • Awareness")
            .setHeaderAction(Action.APP_ICON)
            .setCurrentLocationEnabled(true)
            .setActionStrip(speedStrip())

        // PlaceListMapTemplate requires content when it is not in a loading state.
        template.setItemList(list.build())

        if (loc != null) {
            template.setAnchor(Place.Builder(CarLocation.create(loc.latitude, loc.longitude)).build())
        }

        if (carContext.carAppApiLevel >= 5) {
            template.setOnContentRefreshListener {
                logger.event("car", "Android Auto POI refresh requested")
                invalidate()
            }
        }

        return template.build()
    }

    private fun buildNavigationTemplate(
        loc: android.location.Location?,
        pois: List<OsmPoi>
    ): Template {
        val list = ItemList.Builder()

        if (pois.isEmpty()) {
            list.addItem(
                Row.Builder()
                    .setTitle("BSA NAV test")
                    .addText("No enabled nearby POIs in the current cache")
                    .build()
            )
        } else {
            pois.forEach { poi ->
                val meters = distanceMeters(loc!!.latitude, loc.longitude, poi.lat, poi.lon)
                list.addItem(
                    Row.Builder()
                        .setTitle(poiTitle(poi))
                        .addText(distanceText(meters, poi.humanSummary()))
                        .setOnClickListener {
                            logger.event(
                                "car_poi",
                                "NAV test POI selected",
                                mapOf("osm_type" to poi.osmType, "osm_id" to poi.osmId, "kind" to poi.kind.name)
                            )
                        }
                        .build()
                )
            }
        }

        val content = ListTemplate.Builder()
            .setSingleList(list.build())
            .setTitle("BSA • NAVIGATION test")
            .setHeaderAction(Action.APP_ICON)
            .build()

        // MapWithContentTemplate requires Car App API 7+. The current DHU negotiated
        // API 8, but keep a generic list fallback for older hosts.
        if (carContext.carAppApiLevel < 7) {
            return content
        }

        return MapWithContentTemplate.Builder()
            .setContentTemplate(content)
            .setActionStrip(speedStrip())
            .build()
    }

    private fun isPoiEnabled(poi: OsmPoi): Boolean = when (poi.kind) {
        OsmPoi.Kind.SURVEILLANCE -> prefs.getBoolean("layer_surveillance", true)
        OsmPoi.Kind.FOOD -> prefs.getBoolean("layer_food", false)
        OsmPoi.Kind.FUEL -> prefs.getBoolean("layer_fuel", true)
        OsmPoi.Kind.SHOPPING -> prefs.getBoolean("layer_shopping", false)
        OsmPoi.Kind.SERVICES -> prefs.getBoolean("layer_services", false)
        OsmPoi.Kind.LODGING -> prefs.getBoolean("layer_lodging", false)
        OsmPoi.Kind.OTHER -> prefs.getBoolean("layer_other", false)
    }

    private fun speedStrip(): ActionStrip {
        val location = AppState.location
        val speedMph = if (location?.hasSpeed() == true) {
            (location.speed * 2.2369363f).coerceAtLeast(0f).toInt()
        } else {
            null
        }
        val speedLimit = AppState.speedLimitMph

        return ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(speedMph?.let { "$it mph" } ?: "-- mph")
                    .setOnClickListener { }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(speedLimit?.let { "Limit $it" } ?: "Limit --")
                    .setOnClickListener { }
                    .build()
            )
            .build()
    }

    private fun markerFor(poi: OsmPoi): PlaceMarker {
        val (label, color) = when (poi.kind) {
            OsmPoi.Kind.SURVEILLANCE -> "C" to CarColor.RED
            OsmPoi.Kind.FUEL -> "G" to CarColor.GREEN
            OsmPoi.Kind.FOOD -> "F" to CarColor.YELLOW
            OsmPoi.Kind.SHOPPING -> "S" to CarColor.BLUE
            OsmPoi.Kind.SERVICES -> "SV" to CarColor.BLUE
            OsmPoi.Kind.LODGING -> "L" to CarColor.BLUE
            OsmPoi.Kind.OTHER -> "P" to CarColor.BLUE
        }
        return PlaceMarker.Builder().setLabel(label).setColor(color).build()
    }

    private fun poiTitle(poi: OsmPoi): String = when (poi.kind) {
        OsmPoi.Kind.SURVEILLANCE -> "Camera • ${poi.operator ?: poi.name}"
        OsmPoi.Kind.FUEL -> "Fuel • ${poi.name}"
        else -> "${poi.categoryLabel()} • ${poi.name}"
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val out = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, out)
        return out[0]
    }

    private fun distanceText(meters: Float, detail: String): CharSequence {
        val distance = if (meters < 160.9344f) {
            Distance.create((meters / 0.3048).toDouble(), Distance.UNIT_FEET)
        } else {
            Distance.create((meters / 1609.344).toDouble(), Distance.UNIT_MILES_P1)
        }
        val safeDetail = detail.ifBlank { "Mapped point of interest" }
        val text = SpannableString("  • $safeDetail")
        text.setSpan(
            DistanceSpan.create(distance),
            0,
            1,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE
        )
        return text
    }
}

/**
 * Minimal app-rendered surface used only by the NAVIGATION test flavor.
 *
 * This deliberately does not attempt to duplicate the phone MapLibre map yet. Its purpose is
 * to exercise the same custom-surface path used by Android Auto navigation applications while
 * preserving the nearby-POI content panel. Once the NAV flavor is proven on a real head unit,
 * this surface can be replaced by the actual BSA map renderer.
 */
private class BsaNavSurfaceRenderer(
    private val carContext: CarContext
) : SurfaceCallback {
    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        draw(surfaceContainer)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        try {
            surfaceContainer.surface?.release()
        } catch (_: Throwable) {
        }
    }

    override fun onVisibleAreaChanged(visibleArea: android.graphics.Rect) {
        // The next surface draw uses the full host-provided surface.
    }

    override fun onStableAreaChanged(stableArea: android.graphics.Rect) {
        // Kept intentionally minimal for this discovery/A-B test build.
    }

    private fun draw(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: return
        var canvas: Canvas? = null
        try {
            canvas = surface.lockCanvas(null)
            canvas.drawColor(Color.rgb(18, 24, 31))

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(225, 232, 240)
                textSize = 30f
            }
            canvas.drawText("BSA NAV TEST", 28f, 48f, paint)

            paint.color = Color.rgb(125, 145, 160)
            paint.strokeWidth = 4f
            val midY = canvas.height * 0.58f
            canvas.drawLine(0f, midY, canvas.width.toFloat(), midY - 45f, paint)
            canvas.drawLine(canvas.width * 0.18f, canvas.height.toFloat(), canvas.width * 0.55f, 0f, paint)

            paint.color = Color.rgb(80, 170, 255)
            canvas.drawCircle(canvas.width * 0.48f, canvas.height * 0.54f, 12f, paint)

            paint.color = Color.rgb(190, 200, 210)
            paint.textSize = 18f
            canvas.drawText("Navigation-category / app-rendered surface", 28f, canvas.height - 28f, paint)
        } catch (_: Throwable) {
            // A surface draw failure must never take down the car app during this experiment.
        } finally {
            if (canvas != null) {
                try {
                    surface.unlockCanvasAndPost(canvas)
                } catch (_: Throwable) {
                }
            }
        }
    }
}

