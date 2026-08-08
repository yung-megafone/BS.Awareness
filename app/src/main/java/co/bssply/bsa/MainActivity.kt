package co.bssply.bsa

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.*
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.*

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var mapView: MapView
    private var map: MapLibreMap? = null
    private val io = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("bsa_prefs", MODE_PRIVATE) }
    private val logger by lazy { BsaLogger(this) }

    private lateinit var statusText: TextView
    private lateinit var searchInput: EditText
    private lateinit var filterPanel: LinearLayout
    private lateinit var settingsPanel: LinearLayout
    private lateinit var detailCard: LinearLayout
    private lateinit var routeCard: LinearLayout
    private lateinit var detailTitle: TextView
    private lateinit var detailCategory: TextView
    private lateinit var detailOperator: TextView
    private lateinit var detailDistance: TextView
    private lateinit var detailTags: TextView
    private lateinit var routeTitle: TextView
    private lateinit var routeSummary: TextView
    private lateinit var routeInstruction: TextView

    private lateinit var surveillanceCheck: CheckBox
    private lateinit var foodCheck: CheckBox
    private lateinit var fuelCheck: CheckBox
    private lateinit var shoppingCheck: CheckBox
    private lateinit var servicesCheck: CheckBox
    private lateinit var lodgingCheck: CheckBox
    private lateinit var otherCheck: CheckBox
    private lateinit var coverageCheck: CheckBox
    private lateinit var nightModeCheck: CheckBox
    private lateinit var followModeCheck: CheckBox
    private lateinit var autoLoadCheck: CheckBox
    private lateinit var surveillanceAlertsCheck: CheckBox
    private lateinit var fuelAssistCheck: CheckBox
    private lateinit var locationMarkerGroup: RadioGroup

    private var allPois: List<OsmPoi> = emptyList()
    private val markerToPoi = mutableMapOf<Long, OsmPoi>()
    private val poiMarkers = mutableListOf<Marker>()
    private val coveragePolygons = mutableListOf<Polygon>()
    private var locationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var activeRoute: BsaRoute? = null
    private var selectedPoi: OsmPoi? = null
    private var lastLocation: Location? = null
    private var lastLoadedCenter: LatLng? = null
    private var lastSurveillanceAlertId: String? = null
    private var lastSurveillanceAlertAt = 0L
    private var lastRerouteAt = 0L

    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) startLocationUpdates(true) else status("Location denied — browsing still works")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_main)
        bindViews()
        applySavedPrefs()
        installButtonIcons()
        installListeners()
        logger.event("app", "startup", mapOf("version" to BuildConfig.VERSION_NAME))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topBar)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, bars.top + dp(4), v.paddingRight, v.paddingBottom)
            insets
        }

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isAttributionEnabled = false
            m.cameraPosition = CameraPosition.Builder().target(LatLng(42.2639, -88.8443)).zoom(13.2).build()
            m.setOnMarkerClickListener { marker -> markerToPoi[marker.id]?.let(::showPoi); true }
            m.addOnCameraIdleListener { if (autoLoadCheck.isChecked) scheduleAutoLoad() }
            applyMapStyle(true)
        }
    }

    private fun bindViews() {
        mapView=findViewById(R.id.mapView); statusText=findViewById(R.id.statusText); searchInput=findViewById(R.id.searchInput)
        filterPanel=findViewById(R.id.filterPanel); settingsPanel=findViewById(R.id.settingsPanel); detailCard=findViewById(R.id.detailCard); routeCard=findViewById(R.id.routeCard)
        detailTitle=findViewById(R.id.detailTitle); detailCategory=findViewById(R.id.detailCategory); detailOperator=findViewById(R.id.detailOperator); detailDistance=findViewById(R.id.detailDistance); detailTags=findViewById(R.id.detailTags)
        routeTitle=findViewById(R.id.routeTitle); routeSummary=findViewById(R.id.routeSummary); routeInstruction=findViewById(R.id.routeInstruction)
        surveillanceCheck=findViewById(R.id.surveillanceCheck); foodCheck=findViewById(R.id.foodCheck); fuelCheck=findViewById(R.id.fuelCheck); shoppingCheck=findViewById(R.id.shoppingCheck); servicesCheck=findViewById(R.id.servicesCheck); lodgingCheck=findViewById(R.id.lodgingCheck); otherCheck=findViewById(R.id.otherCheck); coverageCheck=findViewById(R.id.coverageCheck)
        nightModeCheck=findViewById(R.id.nightModeCheck); followModeCheck=findViewById(R.id.followModeCheck); autoLoadCheck=findViewById(R.id.autoLoadCheck); surveillanceAlertsCheck=findViewById(R.id.surveillanceAlertsCheck); fuelAssistCheck=findViewById(R.id.fuelAssistCheck); locationMarkerGroup=findViewById(R.id.locationMarkerGroup)
    }

    private fun applySavedPrefs() {
        fun ck(c:CheckBox,key:String,def:Boolean){ c.isChecked=prefs.getBoolean(key,def) }
        ck(surveillanceCheck,"layer_surveillance",true); ck(foodCheck,"layer_food",false); ck(fuelCheck,"layer_fuel",true); ck(shoppingCheck,"layer_shopping",false); ck(servicesCheck,"layer_services",false); ck(lodgingCheck,"layer_lodging",false); ck(otherCheck,"layer_other",false); ck(coverageCheck,"coverage",false)
        ck(nightModeCheck,"night_mode",true); ck(followModeCheck,"follow",false); ck(autoLoadCheck,"auto_load",true); ck(surveillanceAlertsCheck,"surveillance_alerts",true); ck(fuelAssistCheck,"fuel_assist",true)
        when(prefs.getString("location_marker","arrow")){"dot"->findViewById<RadioButton>(R.id.locationDotRadio).isChecked=true;"car"->findViewById<RadioButton>(R.id.locationCarRadio).isChecked=true;else->findViewById<RadioButton>(R.id.locationArrowRadio).isChecked=true}
    }

    private fun installButtonIcons() {
        fun icon(id:Int,g:LucideIconFactory.Glyph){ findViewById<ImageButton>(id).setImageBitmap(LucideIconFactory.bitmap(this,g,72)) }
        icon(R.id.locateButton,LucideIconFactory.Glyph.LOCATE); icon(R.id.filterButton,LucideIconFactory.Glyph.LAYERS); icon(R.id.gasButton,LucideIconFactory.Glyph.FUEL); icon(R.id.routeButton,LucideIconFactory.Glyph.ROUTE); icon(R.id.contributeButton,LucideIconFactory.Glyph.EDIT); icon(R.id.settingsButton,LucideIconFactory.Glyph.SETTINGS); icon(R.id.searchButton,LucideIconFactory.Glyph.SEARCH)
    }

    private fun installListeners() {
        findViewById<ImageButton>(R.id.locateButton).setOnClickListener { ensureLocationPermission(); followModeCheck.isChecked=true }
        findViewById<ImageButton>(R.id.filterButton).setOnClickListener { toggle(filterPanel) }
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener { toggle(settingsPanel) }
        findViewById<ImageButton>(R.id.gasButton).setOnClickListener { showNextGas() }
        findViewById<ImageButton>(R.id.routeButton).setOnClickListener { selectedPoi?.let { requestRoute(it.lat,it.lon,it.name) } ?: Toast.makeText(this,"Select a POI or search a destination",Toast.LENGTH_SHORT).show() }
        findViewById<ImageButton>(R.id.contributeButton).setOnClickListener { openOsmEditorAtMapCenter() }
        findViewById<ImageButton>(R.id.searchButton).setOnClickListener { searchDestination() }
        searchInput.setOnEditorActionListener { _, action, _ -> if(action==EditorInfo.IME_ACTION_SEARCH){searchDestination();true}else false }
        findViewById<ImageButton>(R.id.closeDetailButton).setOnClickListener { detailCard.visibility=View.GONE }
        findViewById<Button>(R.id.routePoiButton).setOnClickListener { selectedPoi?.let { requestRoute(it.lat,it.lon,it.name) } }
        findViewById<Button>(R.id.openOsmButton).setOnClickListener { selectedPoi?.let(::openOsmObject) }
        findViewById<Button>(R.id.preferredFuelButton).setOnClickListener { selectedPoi?.takeIf { it.kind==OsmPoi.Kind.FUEL }?.let(::savePreferredFuel) }
        findViewById<Button>(R.id.saveHomeButton).setOnClickListener { saveCenter("home") }
        findViewById<Button>(R.id.saveWorkButton).setOnClickListener { saveCenter("work") }
        findViewById<Button>(R.id.routeHomeButton).setOnClickListener { routeSaved("home") }
        findViewById<Button>(R.id.routeWorkButton).setOnClickListener { routeSaved("work") }
        findViewById<Button>(R.id.clearRouteButton).setOnClickListener { clearRoute() }
        findViewById<Button>(R.id.markIssueButton).setOnClickListener { markIssue() }
        findViewById<Button>(R.id.diagnosticsButton).setOnClickListener { showDiagnostics() }
        findViewById<Button>(R.id.aboutButton).setOnClickListener { showAbout() }
        findViewById<Button>(R.id.privacyButton).setOnClickListener { showPrivacy() }

        val checks=listOf(surveillanceCheck to "layer_surveillance",foodCheck to "layer_food",fuelCheck to "layer_fuel",shoppingCheck to "layer_shopping",servicesCheck to "layer_services",lodgingCheck to "layer_lodging",otherCheck to "layer_other",coverageCheck to "coverage")
        checks.forEach { (c,k)->c.setOnCheckedChangeListener { _,v->prefs.edit().putBoolean(k,v).apply();renderMarkers() } }
        nightModeCheck.setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("night_mode",v).apply();applyMapStyle() }
        followModeCheck.setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("follow",v).apply() }
        autoLoadCheck.setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("auto_load",v).apply(); if(v)scheduleAutoLoad() }
        surveillanceAlertsCheck.setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("surveillance_alerts",v).apply() }
        fuelAssistCheck.setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("fuel_assist",v).apply() }
        locationMarkerGroup.setOnCheckedChangeListener { _,id-> val v=when(id){R.id.locationDotRadio->"dot";R.id.locationCarRadio->"car";else->"arrow"};prefs.edit().putString("location_marker",v).apply();updateLocationMarker() }
    }

    private fun toggle(panel:LinearLayout){ val show=panel.visibility!=View.VISIBLE;filterPanel.visibility=View.GONE;settingsPanel.visibility=View.GONE;if(show)panel.visibility=View.VISIBLE }

    private fun applyMapStyle(first:Boolean=false){
        val m=map?:return
        val style=if(nightModeCheck.isChecked) "https://tiles.openfreemap.org/styles/dark" else "https://tiles.openfreemap.org/styles/liberty"
        m.setStyle(style){ renderMarkers(); redrawRoute(); if(first){status("BSA 0.4 • OpenFreeMap ${if(nightModeCheck.isChecked)"Dark" else "Liberty"}");if(hasLocationPermission())startLocationUpdates(false);if(autoLoadCheck.isChecked)scheduleAutoLoad()} }
    }

    private fun scheduleAutoLoad(){ handler.removeCallbacksAndMessages(null); handler.postDelayed({ refreshAtMapCenter(false) },700) }

    private fun refreshAtMapCenter(force:Boolean){
        val c=map?.cameraPosition?.target?:return
        val old=lastLoadedCenter
        if(!force&&old!=null&&distanceBetween(old.latitude,old.longitude,c.latitude,c.longitude)<1200f)return
        status("Loading OSM POIs…")
        val start=System.currentTimeMillis()
        io.execute { runCatching{OverpassClient.fetch(c.latitude,c.longitude,4000)}.onSuccess{pois->runOnUiThread{allPois=pois;AppState.pois=pois;lastLoadedCenter=c;renderMarkers();status("${pois.size} POIs • ${pois.count{it.kind==OsmPoi.Kind.SURVEILLANCE}} surveillance • ${pois.count{it.kind==OsmPoi.Kind.FUEL}} fuel");logger.event("overpass","loaded",mapOf("count" to pois.size,"ms" to System.currentTimeMillis()-start))}}.onFailure{e->runOnUiThread{status("OSM failed: ${e.message}");logger.event("error","overpass failed",mapOf("message" to e.message))}} }
    }

    private fun searchDestination(){
        val q=searchInput.text.toString().trim(); if(q.isBlank())return
        status("Searching…")
        io.execute { runCatching{GeocoderClient.searchOnce(q)}.onSuccess{results->runOnUiThread{if(results.isEmpty())status("No search results") else showSearchResults(results)}}.onFailure{e->runOnUiThread{status("Search failed: ${e.message}");logger.event("error","geocoder failed",mapOf("message" to e.message))}} }
    }

    private fun showSearchResults(results:List<GeocodeResult>){
        AlertDialog.Builder(this).setTitle("Destination").setItems(results.map{it.label}.toTypedArray()){_,which-> val r=results[which]; map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(r.lat,r.lon),15.5),600); requestRoute(r.lat,r.lon,r.label) }.setNegativeButton("Cancel",null).show()
    }

    private fun requestRoute(lat:Double,lon:Double,label:String){
        val origin=lastLocation ?: return Toast.makeText(this,"Enable location before routing",Toast.LENGTH_SHORT).show()
        status("Calculating route…"); val t=System.currentTimeMillis()
        io.execute { runCatching{OsrmClient.route(origin.latitude,origin.longitude,lat,lon,label)}.onSuccess{route->runOnUiThread{activeRoute=route;AppState.route=route;drawRoute(route);showRouteCard(route);logger.event("route","calculated",mapOf("distance_m" to route.distanceMeters,"duration_s" to route.durationSeconds,"ms" to System.currentTimeMillis()-t))}}.onFailure{e->runOnUiThread{status("Route failed: ${e.message}");logger.event("error","route failed",mapOf("message" to e.message))}} }
    }

    private fun drawRoute(route:BsaRoute){ routePolyline?.let{runCatching{map?.removePolyline(it)}}; routePolyline=map?.addPolyline(PolylineOptions().addAll(route.coordinates.map{LatLng(it.first,it.second)}).color(Color.rgb(46,180,255)).width(7f)); routeCard.visibility=View.VISIBLE; detailCard.visibility=View.GONE }
    private fun redrawRoute(){ activeRoute?.let(::drawRoute) }
    private fun clearRoute(){routePolyline?.let{runCatching{map?.removePolyline(it)}};routePolyline=null;activeRoute=null;AppState.route=null;routeCard.visibility=View.GONE;status("Route cleared")}
    private fun showRouteCard(r:BsaRoute){routeTitle.text=r.destinationLabel;routeSummary.text="${formatMiles(r.distanceMeters)} • ${formatDuration(r.durationSeconds)}";routeInstruction.text=r.steps.firstOrNull()?.instruction?:"Route ready";status("Route ready")}

    private fun updateRouteProgress(loc:Location){
        val r=activeRoute?:return
        val nearest=r.coordinates.indices.minByOrNull{i->distanceBetween(loc.latitude,loc.longitude,r.coordinates[i].first,r.coordinates[i].second)}?:0
        val offRoute=distanceBetween(loc.latitude,loc.longitude,r.coordinates[nearest].first,r.coordinates[nearest].second)
        if(offRoute>90f && System.currentTimeMillis()-lastRerouteAt>30_000){ lastRerouteAt=System.currentTimeMillis(); status("Off route • recalculating…"); requestRoute(r.destinationLat,r.destinationLon,r.destinationLabel); return }
        val next=r.steps.filter{it.lat!=null&&it.lon!=null}.minByOrNull{distanceBetween(loc.latitude,loc.longitude,it.lat!!,it.lon!!)}
        if(next!=null) routeInstruction.text=next.instruction
        if(nearest>r.coordinates.size*0.93) routeInstruction.text="Approaching ${r.destinationLabel}"
    }

    private fun renderMarkers(){
        val m=map?:return
        poiMarkers.forEach{runCatching{m.removeMarker(it)}};poiMarkers.clear();markerToPoi.clear();coveragePolygons.forEach{runCatching{m.removePolygon(it)}};coveragePolygons.clear()
        allPois.asSequence().filter(::isVisible).take(800).forEach{poi->
            val marker=m.addMarker(MarkerOptions().position(LatLng(poi.lat,poi.lon)).icon(IconFactory.getInstance(this).fromBitmap(drawPoiBadge(poi))).title(poi.name));poiMarkers+=marker;markerToPoi[marker.id]=poi
            if(coverageCheck.isChecked&&poi.kind==OsmPoi.Kind.SURVEILLANCE) drawCoverage(poi)
        }
        updateLocationMarker()
    }

    private fun drawPoiBadge(poi:OsmPoi):Bitmap{
        val size=dp(42).coerceAtLeast(42);val b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG)
        p.color=when(poi.kind){OsmPoi.Kind.SURVEILLANCE->Color.rgb(225,65,75);OsmPoi.Kind.FUEL->Color.rgb(245,183,52);OsmPoi.Kind.FOOD->Color.rgb(228,126,54);OsmPoi.Kind.SHOPPING->Color.rgb(55,174,205);OsmPoi.Kind.SERVICES->Color.rgb(76,143,225);OsmPoi.Kind.LODGING->Color.rgb(153,105,211);OsmPoi.Kind.OTHER->Color.rgb(103,118,126)};c.drawCircle(size/2f,size/2f,size*.44f,p);p.style=Paint.Style.STROKE;p.strokeWidth=size*.055f;p.color=Color.WHITE;c.drawCircle(size/2f,size/2f,size*.44f,p)
        val glyph=when(poi.kind){OsmPoi.Kind.SURVEILLANCE->LucideIconFactory.Glyph.CAMERA;OsmPoi.Kind.FUEL->LucideIconFactory.Glyph.FUEL;OsmPoi.Kind.FOOD->LucideIconFactory.Glyph.UTENSILS;OsmPoi.Kind.SHOPPING->LucideIconFactory.Glyph.CART;OsmPoi.Kind.LODGING->LucideIconFactory.Glyph.BED;else->LucideIconFactory.Glyph.MAP_PIN}
        val icon=LucideIconFactory.bitmap(this,glyph,(size*.58f).toInt());c.save();poi.cameraBearing?.let{c.rotate(it,size/2f,size/2f)};c.drawBitmap(icon,(size-icon.width)/2f,(size-icon.height)/2f,null);c.restore();return b
    }

    private fun drawCoverage(poi:OsmPoi){ val bearing=poi.cameraBearing?:return;val range=poi.estimatedRangeMeters?:return;val pts=mutableListOf(LatLng(poi.lat,poi.lon));for(i in 0..12){val a=bearing-18f+i*3f;pts+=destinationPoint(poi.lat,poi.lon,a.toDouble(),range)};pts+=LatLng(poi.lat,poi.lon);map?.addPolygon(PolygonOptions().addAll(pts).fillColor(Color.argb(55,235,65,75)).strokeColor(Color.argb(130,255,110,115)))?.let{coveragePolygons+=it} }

    private fun showPoi(p:OsmPoi){selectedPoi=p;detailTitle.text=p.name;detailCategory.text="${p.categoryLabel()} • ${p.humanSummary()}";detailOperator.visibility=if(p.operator!=null)View.VISIBLE else View.GONE;detailOperator.text=p.operator?.let{"Agency / operator: $it"};detailDistance.text=relativeDescription(p);detailTags.text=p.rawTags();findViewById<Button>(R.id.preferredFuelButton).visibility=if(p.kind==OsmPoi.Kind.FUEL)View.VISIBLE else View.GONE;detailCard.visibility=View.VISIBLE;routeCard.visibility=if(activeRoute!=null)View.VISIBLE else View.GONE;filterPanel.visibility=View.GONE;settingsPanel.visibility=View.GONE}

    private fun showNextGas(){ val loc=lastLocation;val fuels=allPois.filter{it.kind==OsmPoi.Kind.FUEL};if(fuels.isEmpty())return Toast.makeText(this,"No fuel POIs loaded yet",Toast.LENGTH_SHORT).show();val best=if(loc==null)fuels.first() else fuels.sortedBy{distanceMeters(loc,it)}.firstOrNull{!loc.hasBearing()||abs(normalizeSigned(loc.bearingTo(locationFor(it))-loc.bearing))<100f}?:fuels.minBy{distanceMeters(loc,it)};showPoi(best);map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(best.lat,best.lon),16.2),500);status("Next gas candidate: ${best.name}") }

    private fun isVisible(p:OsmPoi)=when(p.kind){OsmPoi.Kind.SURVEILLANCE->surveillanceCheck.isChecked;OsmPoi.Kind.FOOD->foodCheck.isChecked;OsmPoi.Kind.FUEL->fuelCheck.isChecked;OsmPoi.Kind.SHOPPING->shoppingCheck.isChecked;OsmPoi.Kind.SERVICES->servicesCheck.isChecked;OsmPoi.Kind.LODGING->lodgingCheck.isChecked;OsmPoi.Kind.OTHER->otherCheck.isChecked}

    private fun relativeDescription(p:OsmPoi):String{ val loc=lastLocation?:return "Location unavailable";val d=distanceMeters(loc,p);val bearing=loc.bearingTo(locationFor(p));if(!loc.hasBearing()||loc.speed<.8f)return "${formatDistance(d)} away • bearing ${normalizeBearing(bearing).toInt()}°";val rel=normalizeSigned(bearing-loc.bearing);val side=when{abs(rel)<=22.5f->"straight ahead";rel in 22.5f..157.5f->"ahead/right of travel";rel in -157.5f..-22.5f->"ahead/left of travel";else->"behind you"};return "${formatDistance(d)} • $side • ${normalizeBearing(bearing).toInt()}°" }

    private fun checkSurveillanceAlert(loc:Location){ if(!surveillanceAlertsCheck.isChecked)return;val p=allPois.filter{it.kind==OsmPoi.Kind.SURVEILLANCE}.minByOrNull{distanceMeters(loc,it)}?:return;val d=distanceMeters(loc,p);if(d>402f)return;if(loc.hasBearing()&&abs(normalizeSigned(loc.bearingTo(locationFor(p))-loc.bearing))>100f)return;val id="${p.osmType}/${p.osmId}";if(id==lastSurveillanceAlertId&&System.currentTimeMillis()-lastSurveillanceAlertAt<10*60_000)return;lastSurveillanceAlertId=id;lastSurveillanceAlertAt=System.currentTimeMillis();Toast.makeText(this,"Surveillance ahead • ${formatDistance(d)} • ${p.operator?:p.name}",Toast.LENGTH_LONG).show();logger.event("surveillance_alert","nearby",mapOf("osm" to id,"distance_m" to d)) }

    private fun ensureLocationPermission(){if(hasLocationPermission())startLocationUpdates(true)else requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))}
    private fun hasLocationPermission()=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    @SuppressLint("MissingPermission") private fun startLocationUpdates(center:Boolean){if(!hasLocationPermission())return;val lm=getSystemService(LOCATION_SERVICE) as LocationManager;listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER).forEach{if(runCatching{lm.isProviderEnabled(it)}.getOrDefault(false))runCatching{lm.requestLocationUpdates(it,1500L,3f,this)}};if(center)lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(::centerOn)}
    override fun onLocationChanged(loc:Location){lastLocation=loc;AppState.location=loc;updateLocationMarker();selectedPoi?.let{detailDistance.text=relativeDescription(it)};updateRouteProgress(loc);checkSurveillanceAlert(loc);logger.location(loc,prefs.getBoolean("log_precise",false));if(followModeCheck.isChecked)centerOn(loc,false)}
    private fun centerOn(loc:Location,animate:Boolean=true){lastLocation=loc;val upd=CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(LatLng(loc.latitude,loc.longitude)).zoom(max(map?.cameraPosition?.zoom?:15.5,15.5)).bearing(if(loc.hasBearing())loc.bearing.toDouble() else map?.cameraPosition?.bearing?:0.0).build());if(animate)map?.animateCamera(upd,450)else map?.moveCamera(upd)}
    private fun updateLocationMarker(){val m=map?:return;locationMarker?.let{runCatching{m.removeMarker(it)}};locationMarker=null;val loc=lastLocation?:return;val style=prefs.getString("location_marker","arrow")!!;locationMarker=m.addMarker(MarkerOptions().position(LatLng(loc.latitude,loc.longitude)).icon(IconFactory.getInstance(this).fromBitmap(drawLocationIcon(style,if(loc.hasBearing())loc.bearing else 0f))).title("Your location"))}
    private fun drawLocationIcon(style:String,bearing:Float):Bitmap{val size=dp(54);val b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);val c=Canvas(b);val p=Paint(Paint.ANTI_ALIAS_FLAG);p.color=Color.argb(100,48,167,255);c.drawCircle(size/2f,size/2f,size*.43f,p);p.color=Color.WHITE;c.drawCircle(size/2f,size/2f,size*.31f,p);p.color=Color.rgb(48,167,255);c.drawCircle(size/2f,size/2f,size*.25f,p);if(style=="dot"){p.color=Color.WHITE;c.drawCircle(size/2f,size/2f,size*.07f,p)}else{val glyph=if(style=="car")LucideIconFactory.Glyph.CAR else LucideIconFactory.Glyph.ROUTE;val icon=LucideIconFactory.bitmap(this,glyph,(size*.55).toInt());c.save();c.rotate(bearing,size/2f,size/2f);c.drawBitmap(icon,(size-icon.width)/2f,(size-icon.height)/2f,null);c.restore()};return b}


    private fun saveCenter(key:String){ val c=map?.cameraPosition?.target?:return; prefs.edit().putString("${key}_lat",c.latitude.toString()).putString("${key}_lon",c.longitude.toString()).apply(); Toast.makeText(this,"Saved ${key.replaceFirstChar{it.uppercase()}}",Toast.LENGTH_SHORT).show() }
    private fun routeSaved(key:String){ val lat=prefs.getString("${key}_lat",null)?.toDoubleOrNull(); val lon=prefs.getString("${key}_lon",null)?.toDoubleOrNull(); if(lat==null||lon==null){Toast.makeText(this,"${key.replaceFirstChar{it.uppercase()}} is not saved yet",Toast.LENGTH_SHORT).show();return}; requestRoute(lat,lon,key.replaceFirstChar{it.uppercase()}) }
    private fun savePreferredFuel(p:OsmPoi){ prefs.edit().putString("preferred_fuel_name",p.name).putString("preferred_fuel_lat",p.lat.toString()).putString("preferred_fuel_lon",p.lon.toString()).apply(); Toast.makeText(this,"Preferred fuel: ${p.name}",Toast.LENGTH_SHORT).show(); logger.event("fuel_assist","preferred station saved",mapOf("name" to p.name,"osm" to "${p.osmType}/${p.osmId}")) }

    private fun markIssue(){val input=EditText(this).apply{hint="What happened?"};AlertDialog.Builder(this).setTitle("Field note").setView(input).setPositiveButton("Save"){_,_->val n=input.text.toString().trim();if(n.isNotBlank()){logger.userNote(n);Toast.makeText(this,"Note saved to local diagnostics",Toast.LENGTH_SHORT).show()}}.setNegativeButton("Cancel",null).show()}
    private fun showDiagnostics(){val fuel=VehicleState.fuelPercent?.let{"${it.toInt()}%"}?:"unavailable";val range=VehicleState.remainingRangeMeters?.let{formatMiles(it.toDouble())}?:"unavailable";val msg="BSA ${BuildConfig.VERSION_NAME}\n\nGPS: ${lastLocation?.let{"±${it.accuracy.toInt()} m • ${if(it.hasBearing())"${it.bearing.toInt()}°" else "no bearing"}"}?:"no fix"}\nPOIs loaded: ${allPois.size}\nAndroid Auto: ${if(VehicleState.androidAutoConnected)"connected" else "not connected"}\nFuel API: ${VehicleState.fuelCapability}\nFuel: $fuel\nRange: $range\n\nDiagnostics stay local until you explicitly export them.";AlertDialog.Builder(this).setTitle("Diagnostics").setMessage(msg).setPositiveButton("Export"){_,_->shareDiagnostics()}.setNeutralButton("Clear logs"){_,_->logger.clear()}.setNegativeButton("Close",null).show()}
    private fun shareDiagnostics(){val zip=logger.exportZip(prefs.getBoolean("log_precise",false),prefs.getBoolean("log_vehicle_values",false));val uri=FileProvider.getUriForFile(this,"$packageName.files",zip);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/zip";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Export BSA diagnostics"))}
    private fun showAbout(){AlertDialog.Builder(this).setTitle("About B.S. Awareness").setMessage("B.S. Awareness (BSA) is an experimental, open-source mapping and navigation client built around OpenStreetMap. It surfaces ordinary POIs such as fuel, food, shopping and services alongside mapped infrastructure such as surveillance cameras.\n\nThe app uses MapLibre for rendering, OpenFreeMap for the vector basemap, OpenStreetMap/Overpass for POIs, OSRM for basic road routing, and a Lucide-inspired/native stroke icon implementation with Lucide attribution.\n\nNavigation, side-of-travel descriptions and estimated camera coverage are advisory. Map data can be incomplete or wrong. Coverage graphics are estimates, not guarantees of observation or recognition.\n\nVersion ${BuildConfig.VERSION_NAME}").setPositiveButton("OK",null).show()}
    private fun showPrivacy(){AlertDialog.Builder(this).setTitle("Privacy").setMessage("BSA is local-first. There is no BSA account, advertising SDK, BSA analytics service, or BSA cloud telemetry in this alpha.\n\nYour location is used on-device for mapping, nearby POIs, navigation and alerts. Online services necessarily receive request data needed to answer queries: OpenFreeMap receives map-tile/style requests, Overpass receives the queried map area, Nominatim receives searches you submit, and OSRM receives route endpoints.\n\nAndroid Auto vehicle data is capability-gated. If fuel/range is unavailable, Fuel Assist stays dormant. BSA does not upload vehicle telemetry to a BSA server.\n\nDiagnostic logs remain on this device until you explicitly export/share them. Precise GPS values are excluded by default.").setPositiveButton("OK",null).show()}

    private fun openOsmObject(p:OsmPoi){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/${p.osmType}/${p.osmId}")))}
    private fun openOsmEditorAtMapCenter(){val c=map?.cameraPosition?.target?:return;startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(String.format(Locale.US,"https://www.openstreetmap.org/edit#map=19/%.6f/%.6f",c.latitude,c.longitude))))}
    private fun destinationPoint(lat:Double,lon:Double,bearing:Double,distance:Double):LatLng{val r=6371000.0;val br=Math.toRadians(bearing);val p1=Math.toRadians(lat);val l1=Math.toRadians(lon);val ad=distance/r;val p2=asin(sin(p1)*cos(ad)+cos(p1)*sin(ad)*cos(br));val l2=l1+atan2(sin(br)*sin(ad)*cos(p1),cos(ad)-sin(p1)*sin(p2));return LatLng(Math.toDegrees(p2),Math.toDegrees(l2))}
    private fun locationFor(p:OsmPoi)=Location("poi").apply{latitude=p.lat;longitude=p.lon};private fun distanceMeters(o:Location,p:OsmPoi)=o.distanceTo(locationFor(p));private fun distanceBetween(a:Double,b:Double,c:Double,d:Double):Float{val out=FloatArray(1);Location.distanceBetween(a,b,c,d,out);return out[0]};private fun normalizeBearing(v:Float)=((v%360f)+360f)%360f;private fun normalizeSigned(v:Float):Float{var x=normalizeBearing(v);if(x>180)x-=360;return x};private fun formatDistance(m:Float)=if(m<304.8f)"${m.toInt()} m" else String.format(Locale.US,"%.2f mi",m/1609.344f);private fun formatMiles(m:Double)=String.format(Locale.US,"%.1f mi",m/1609.344);private fun formatDuration(s:Double):String{val min=(s/60).roundToInt();return if(min<60)"$min min" else "${min/60}h ${min%60}m"};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();private fun status(s:String){statusText.text=s}

    override fun onStart(){super.onStart();mapView.onStart()};override fun onResume(){super.onResume();mapView.onResume();if(hasLocationPermission())startLocationUpdates(false)};override fun onPause(){runCatching{(getSystemService(LOCATION_SERVICE) as LocationManager).removeUpdates(this)};mapView.onPause();super.onPause()};override fun onStop(){mapView.onStop();super.onStop()};override fun onLowMemory(){super.onLowMemory();mapView.onLowMemory()};override fun onDestroy(){io.shutdownNow();mapView.onDestroy();super.onDestroy()};override fun onSaveInstanceState(outState:Bundle){super.onSaveInstanceState(outState);mapView.onSaveInstanceState(outState)}
}
