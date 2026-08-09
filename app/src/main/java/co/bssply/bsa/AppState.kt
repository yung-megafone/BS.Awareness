package co.bssply.bsa

import android.location.Location

object AppState {
    @Volatile var location: Location? = null
    @Volatile var pois: List<OsmPoi> = emptyList()
    @Volatile var route: BsaRoute? = null
    // Populated by a future road-metadata provider. Null means unknown.
    @Volatile var speedLimitMph: Int? = null
}
