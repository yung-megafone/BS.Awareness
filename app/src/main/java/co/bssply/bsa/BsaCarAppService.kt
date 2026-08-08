package co.bssply.bsa

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
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
    private var carInfo: CarInfo? = null
    private var energyListener: OnCarDataAvailableListener<EnergyLevel>? = null

    init {
        VehicleState.androidAutoConnected = true
        logger.event(
            "car",
            "Android Auto connected",
            mapOf("car_app_api_level" to carContext.carAppApiLevel)
        )

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

                // Actual vehicle values are intentionally omitted from logs by default.
                // They are included only when the user explicitly enables that setting.
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
        val list = ItemList.Builder()
        val route = AppState.route
        if (route != null) {
            list.addItem(
                Row.Builder()
                    .setTitle("Navigating")
                    .addText(route.destinationLabel)
                    .addText(
                        "${String.format("%.1f mi", route.distanceMeters / 1609.344)} • " +
                            "${(route.durationSeconds / 60).toInt()} min"
                    )
                    .build()
            )
        }

        val loc = AppState.location
        val pois = if (loc == null) {
            AppState.pois.take(5)
        } else {
            AppState.pois
                .filter { it.kind == OsmPoi.Kind.SURVEILLANCE || it.kind == OsmPoi.Kind.FUEL }
                .sortedBy { p ->
                    val out = FloatArray(1)
                    android.location.Location.distanceBetween(
                        loc.latitude,
                        loc.longitude,
                        p.lat,
                        p.lon,
                        out
                    )
                    out[0]
                }
                .take(5)
        }

        pois.forEach { p ->
            val out = FloatArray(1)
            if (loc != null) {
                android.location.Location.distanceBetween(
                    loc.latitude,
                    loc.longitude,
                    p.lat,
                    p.lon,
                    out
                )
            }
            val distance = if (loc == null) {
                "nearby"
            } else if (out[0] < 305) {
                "${out[0].toInt()} m"
            } else {
                String.format("%.1f mi", out[0] / 1609.344)
            }

            list.addItem(
                Row.Builder()
                    .setTitle(
                        if (p.kind == OsmPoi.Kind.SURVEILLANCE) {
                            "Camera • ${p.operator ?: p.name}"
                        } else {
                            "Fuel • ${p.name}"
                        }
                    )
                    .addText(distance)
                    .build()
            )
        }

        val fuelText = when {
            VehicleState.fuelPercent != null -> {
                "Fuel ${VehicleState.fuelPercent!!.toInt()}%" +
                    (VehicleState.remainingRangeMeters?.let {
                        " • ${String.format("%.0f mi", it / 1609.344)} range"
                    } ?: "")
            }
            else -> "Fuel data ${VehicleState.fuelCapability}"
        }
        list.addItem(Row.Builder().setTitle("Vehicle").addText(fuelText).build())

        val sp = carContext.getSharedPreferences("bsa_prefs", android.content.Context.MODE_PRIVATE)
        val preferred = sp.getString("preferred_fuel_name", null)
        if (
            sp.getBoolean("fuel_assist", true) &&
            preferred != null &&
            (VehicleState.fuelPercent ?: 101f) <= 50f
        ) {
            list.addItem(
                Row.Builder()
                    .setTitle("Fuel Assist")
                    .addText("Preferred stop: $preferred • fuel is at or below 50%")
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setSingleList(list.build())
            .setHeader(
                Header.Builder()
                    .setStartHeaderAction(Action.APP_ICON)
                    .setTitle("BSA • Awareness")
                    .build()
            )
            .build()
    }
}
