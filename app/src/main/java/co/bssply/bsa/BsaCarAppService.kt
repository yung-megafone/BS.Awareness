package co.bssply.bsa

import android.content.Intent
import android.content.pm.PackageManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SessionInfo
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.model.*
import androidx.car.app.validation.HostValidator
import java.util.concurrent.Executor

class BsaCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(sessionInfo: SessionInfo): Session = BsaCarSession()
}

class BsaCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = BsaCarHomeScreen(carContext)
}

class BsaCarHomeScreen(carContext: CarContext) : Screen(carContext) {
    private val executor = Executor { r -> carContext.mainExecutor.execute(r) }
    private var energyListener: OnCarDataAvailableListener<EnergyLevel>? = null

    init {
        VehicleState.androidAutoConnected = true
        probeFuel()
    }

    private fun probeFuel() {
        try {
            if (carContext.carAppApiLevel < 3) {
                VehicleState.fuelCapability = "host API too old"
                return
            }
            if (carContext.checkSelfPermission("com.google.android.gms.permission.CAR_FUEL") != PackageManager.PERMISSION_GRANTED) {
                VehicleState.fuelCapability = "permission not granted / unsupported"
                return
            }
            val manager = carContext.getCarService(CarHardwareManager::class.java)
            val info = manager.carInfo
            val listener = OnCarDataAvailableListener<EnergyLevel> { energy ->
                VehicleState.fuelCapability = "available"
                VehicleState.fuelPercent = successValue(energy.fuelPercent)
                VehicleState.remainingRangeMeters = successValue(energy.rangeRemainingMeters)
                VehicleState.lowFuel = successValue(energy.energyIsLow)
                invalidate()
            }
            energyListener = listener
            info.addEnergyLevelListener(executor, listener)
            VehicleState.fuelCapability = "probing"
        } catch (t: Throwable) {
            VehicleState.fuelCapability = "unavailable (${t.javaClass.simpleName})"
        }
    }

    private fun <T> successValue(value: CarValue<T>): T? = if (value.status == CarValue.STATUS_SUCCESS) value.value else null

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        val route = AppState.route
        if (route != null) {
            list.addItem(Row.Builder().setTitle("Navigating").addText(route.destinationLabel).addText("${(route.distanceMeters / 1609.344).let { String.format("%.1f mi", it) }} • ${(route.durationSeconds/60).toInt()} min").build())
        }
        val loc = AppState.location
        val pois = if (loc == null) AppState.pois.take(5) else AppState.pois
            .filter { it.kind == OsmPoi.Kind.SURVEILLANCE || it.kind == OsmPoi.Kind.FUEL }
            .sortedBy { p -> val out=FloatArray(1); android.location.Location.distanceBetween(loc.latitude,loc.longitude,p.lat,p.lon,out); out[0] }
            .take(5)
        pois.forEach { p ->
            val out=FloatArray(1); if(loc!=null) android.location.Location.distanceBetween(loc.latitude,loc.longitude,p.lat,p.lon,out)
            val distance = if(loc==null) "nearby" else if(out[0]<305) "${out[0].toInt()} m" else String.format("%.1f mi",out[0]/1609.344)
            list.addItem(Row.Builder().setTitle(if(p.kind==OsmPoi.Kind.SURVEILLANCE) "Camera • ${p.operator ?: p.name}" else "Fuel • ${p.name}").addText(distance).build())
        }
        val fuelText = when {
            VehicleState.fuelPercent != null -> "Fuel ${VehicleState.fuelPercent!!.toInt()}%" + (VehicleState.remainingRangeMeters?.let { " • ${String.format("%.0f mi", it/1609.344)} range" } ?: "")
            else -> "Fuel data ${VehicleState.fuelCapability}"
        }
        list.addItem(Row.Builder().setTitle("Vehicle").addText(fuelText).build())
        val sp = carContext.getSharedPreferences("bsa_prefs", android.content.Context.MODE_PRIVATE)
        val preferred = sp.getString("preferred_fuel_name", null)
        if (sp.getBoolean("fuel_assist", true) && preferred != null && (VehicleState.fuelPercent ?: 101f) <= 50f) {
            list.addItem(Row.Builder().setTitle("Fuel Assist").addText("Preferred stop: $preferred • fuel is at or below 50%").build())
        }
        return ListTemplate.Builder().setSingleList(list.build()).setHeader(Header.Builder().setStartHeaderAction(Action.APP_ICON).setTitle("BSA • Awareness").build()).build()
    }
}
