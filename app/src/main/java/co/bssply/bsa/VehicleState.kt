package co.bssply.bsa

object VehicleState {
    @Volatile var androidAutoConnected: Boolean = false
    @Volatile var fuelPercent: Float? = null
    @Volatile var remainingRangeMeters: Float? = null
    @Volatile var lowFuel: Boolean? = null
    @Volatile var fuelCapability: String = "not probed"
}
