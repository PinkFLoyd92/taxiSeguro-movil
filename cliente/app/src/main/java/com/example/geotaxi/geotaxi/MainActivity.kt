package com.example.geotaxi.geotaxi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.location.Location
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.Address
import android.os.AsyncTask
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    var mCurrentLocation: GeoPoint = GeoPoint(-2.1811931,-79.8765573)//Guayaquil
    var endGp: GeoPoint? = null
    var locationName = ""
    var map: MapView? = null
    var addressRecyclerView: RecyclerView? = null
    var addressCardView: CardView? = null
    var taxi_request: Button? = null
    var serverAPI: ServerAPI? = null
    var currentRoad: Road? = null
    var clientId: String? = null
    val socket = IO.socket(Env.API_SOCKET_URL)
    var routeId: String? = ""
    var mFusedLocationClient: FusedLocationProviderClient? = null
    var mLocationRequest: LocationRequest? = null
    var mLocationCallback: LocationCallback? = null
    var onRoute = false
    var userMarker: Marker? = null
    var actMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        setContentView(R.layout.activity_main)

        addressCardView = findViewById(R.id.address_card_view)
        addressRecyclerView = findViewById(R.id.address_recycler_view)
        taxi_request = findViewById(R.id.taxi_request_button)
        map = findViewById(R.id.map)
        val searchEV = findViewById<EditText>(R.id.search)
        val search = findViewById<EditText>(R.id.search)
        val userIcon = ResourcesCompat.getDrawable(resources, R.drawable.user_location, null)
        val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
        val geocoderBtn = findViewById<ImageButton>(R.id.geocoder_btn)
        val mapController = map?.controller

        searchEV.setImeActionLabel("Buscar", KeyEvent.KEYCODE_ENTER)
        searchEV.setOnEditorActionListener(MyEditionActionListener())
        taxi_request?.setOnClickListener { requestTaxi() }
        setOnTouchListener(search)

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        addressRecyclerView?.setHasFixedSize(true)
        // use a linear layout manager
        val mLayoutManager = LinearLayoutManager(this)
        addressRecyclerView?.layoutManager = mLayoutManager
        // add rotation gesture
        val mRotationGestureOverlay =  RotationGestureOverlay(map)
        mRotationGestureOverlay.isEnabled = true
        map?.setTileSource(TileSourceFactory.MAPNIK)
        map?.setMultiTouchControls(true)
        map?.overlays?.add(mRotationGestureOverlay)
        userMarker = Marker(map)
        userMarker?.setIcon(userIcon)

        fab.setOnClickListener {
            mapController?.animateTo(mCurrentLocation)
            mapController?.zoomTo(17)
        }

        var mIntent = intent
        clientId = mIntent.getStringExtra("client_id")

        geocoderBtn.setOnClickListener{
            locationName = search.text.toString().trim()
            if (locationName !== "")
                executeFromLocationNameTask()
        }

        // check access location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                showPermissionExplanation()
                Toast.makeText(this, "Permiso fue denegado", Toast.LENGTH_SHORT).show()

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        Env.MY_PERMISSIONS_REQUEST_LOCATION)
            }

        } else {
            initLocationrequest()
        }

        socketConnect()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener(search: EditText) {
        search.setOnTouchListener(View.OnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2

            if(event?.action == MotionEvent.ACTION_UP) {
                if(event.rawX >= (search.right - search.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                    // your action here
                    search.setText("")
                    addressCardView?.visibility = View.GONE
                    return@OnTouchListener true
                }
            }
            false
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Env.MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        initLocationrequest()
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
                    finish()

                }
                return
            }
        }
    }

    private fun showPermissionExplanation() {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Información del permiso")
        alertDialog.setMessage("Es necesario el permiso de localización precisa para usar la applicación")

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Aceptar") {
            dialog, which -> run {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    Env.MY_PERMISSIONS_REQUEST_LOCATION)
        }
        }
        alertDialog.show()
    }

    private fun initLocationrequest() {
        createLocationRequest()
        mLocationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("activity",String.format("locations: %s ", "" + locationResult.locations.size))

                onLocationChanged(locationResult.lastLocation)
            }
        }
        //get current location settings
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest!!)
        //check whether the current location settings are satisfied
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        //Prompt the User to Change Location Settings
        task.addOnSuccessListener(this) {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            locationRequest()
        }
        task.addOnFailureListener(this) { p0 ->
            if (p0 is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().

                    p0.startResolutionForResult(this@MainActivity,
                            Env.REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException ) {
                    Log.d("activity",String.format("exception on start resolution for results: %s ", sendEx.message))
                }
            }
        }

    }

    //Getting a result from MainActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Check which request we're responding to
        if (requestCode == Env.REQUEST_CHECK_SETTINGS) {
            // Make sure the request was successful
            if (resultCode == Activity.RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                locationRequest()
            }
        }
    }
    private  fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval = Env.LOCATION_REQUEST_INTERVAL
        mLocationRequest?.fastestInterval = Env.LR_FASTEST_INTERVAL
        mLocationRequest?.priority = Env.LR_PRIORITY
        mLocationRequest?.smallestDisplacement = 0F
    }
    @SuppressLint("MissingPermission")
    private fun locationRequest() {
        try {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            mFusedLocationClient?.lastLocation
                    ?.addOnSuccessListener(this) { location ->
                        // Got last known location. In some situations this can be null.
                        if (location != null) {
                            Log.d("activity",String.format("last location"))
                            onLocationChanged(location)
                        }
                    }
            startLocationUpdates()
            val mapController = map?.controller
            mapController?.animateTo(mCurrentLocation)
            mapController?.zoomTo(17)
        } catch (e: Exception) {
            Log.d("activity",String.format("exception on location request: %s ", e.message))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {

            mFusedLocationClient?.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */)
            Log.d("activity",String.format("start updates"))
        } catch (e: Exception) {
            Log.d("activity",String.format("exception when start location updates: %s ", e.message))
        }
    }

    private fun onLocationChanged(location: Location) {
        val currentLocation = GeoPoint(location)
        mCurrentLocation = currentLocation
        updateUserIconOnMap()
        if (onRoute) {
            val data = JsonObject()
            val pos = JsonObject()
            pos.addProperty("longitude", currentLocation.longitude)
            pos.addProperty("latitude", currentLocation.latitude)
            data.add("position", pos)
            data.addProperty("route_id", routeId)
            try {
                socket.emit("POSITION", data)
            } catch (e: Exception) {
                Log.d("activity",String.format("exception on location change: %s ", e.message))
            }
        }
    }

    private fun updateUserIconOnMap() {
        map?.overlays?.remove(userMarker)
        userMarker?.position = mCurrentLocation
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(userMarker)
        map?.invalidate()
    }

    private fun socketConnect() {
        try {
            socket.on(Socket.EVENT_CONNECT) {
                val userInfo = JsonObject()
                userInfo.addProperty("_id", clientId)
                userInfo.addProperty("role", "client")
                socket.emit("SENDINFO", userInfo)
            }.on("DANGER") { TODO()
            }.on(Socket.EVENT_DISCONNECT) {
                Log.d("activity", "socket disconnected")
            }
            socket.connect()
        } catch (e: Exception) {
            Log.d("activity",String.format("exception on socket: %s ", e.message))

        }

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_alert)?.isEnabled = false
        menu?.findItem(R.id.action_alert)?.icon
                ?.mutate()?.setTint(Color.GRAY)
        actMenu = menu
        return super.onPrepareOptionsMenu(menu)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        if (id == R.id.action_alert) {

            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle("Alerta")
            alertDialog.setMessage("Enviar alerta de auxilio?");
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancelar"
            ) { dialog, wich -> dialog.dismiss() }

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Aceptar") {
                dialog, which -> run {

                val data = JsonObject()
                data.addProperty("route_id", routeId)
                try {
                    socket.emit("ALERT", data)
                } catch (e: Exception) {
                    Log.d("activity",String.format("exception on emit alert: %s ", e.message))
                }
            }
            }
            alertDialog.show()
        }
        return super.onOptionsItemSelected(item)
    }

    //Class that handle user input on address search
    inner class MyEditionActionListener : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

            if (event != null && (event?.keyCode == KeyEvent.KEYCODE_ENTER) ||
                    (actionId == EditorInfo.IME_ACTION_DONE)) {
                val searchEV = v as EditText
                /*locationName = searchEV.text.toString() + mCurrentLocation.latitude +
                    ", " + mCurrentLocation.longitude*/
                locationName = searchEV.text.toString().trim()
                if (locationName == "") return false
                executeFromLocationNameTask()
                return true
            }

            return false
        }
    }

    private fun executeFromLocationNameTask() {
        val flnTask = FromLocationNameTask()
        flnTask.execute()
    }

    //Class that use geocoder Nominatim server in async mode for get location from address name
    inner class FromLocationNameTask : AsyncTask<Context, Void, List<Address>>() {
        override fun doInBackground(vararg params: Context?): List<Address> {
            val geoNominatim = GeocoderNominatim(Locale.getDefault(), System.getProperty("http.agent"))
            //uncomment for use own server
            // geoNominatim.setService(Env.NOMINATIM_SERVER_URL)
            var addresses = listOf<Address>()
            try {
                addresses = geoNominatim.getFromLocationName(locationName,10, -1.97166,
                        -80.08278,-2.31474,
                        -79.46686)
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "Ocurrió un error al buscar la dirección", Toast.LENGTH_LONG).show()
                return addresses
            }

            return addresses
        }

        override fun onPostExecute(result: List<Address>) {
            super.onPostExecute(result)
            if (result.isNotEmpty()) {
                endGp = GeoPoint(result.get(0).latitude, result.get(0).longitude)
                // specify an adapter
                val mAdapter = AddressListViewAdapter(result, MyRVClickListener())
                addressRecyclerView?.setAdapter(mAdapter)
                addressCardView?.visibility = View.VISIBLE
                taxi_request?.visibility = View.GONE

            } else {
                Log.d("activity","fail to match location name")
            }
        }
    }

    //Class that handle click action on an item of the list view of addresses
    inner class MyRVClickListener : View.OnClickListener {

        override fun onClick(v: View?) {
            val geoPointStr = v?.findViewById<TextView>(R.id.location_tv)?.text.toString()
            var geoPointList = geoPointStr.split(",").map { it.trim() }
            endGp = GeoPoint((geoPointList[0]).toDouble(), geoPointList[1].toDouble())
            addressCardView?.visibility = View.GONE
            map?.overlays?.clear()
            //calculate and draw road on map
            executeRoadTask()
        }

    }

    private  fun executeRoadTask(){
        val roadTask = RoadTask()
        roadTask.execute(applicationContext)
    }

    //Class that use OSRM server in async mode for get a Road, from start to destination point
    inner class RoadTask : AsyncTask<Context, Void, Road>() {

        override fun doInBackground(vararg params: Context?): Road? {
            val roadManager = OSRMRoadManager(params[0])
            val wayPoints = ArrayList<GeoPoint>(0)
            if (endGp != null) {
                wayPoints.add(mCurrentLocation)
                wayPoints.add(endGp as GeoPoint)
                //uncomment for use own server
                roadManager.setService(Env.OSRM_SERVER_URL)
                val road = roadManager.getRoad(wayPoints)
                return road
            }

            return null
        }

        override fun onPostExecute(result: Road?) {
            super.onPostExecute(result)
            if (result != null) {
                currentRoad = result
                drawRoad(result)
                val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
                fab.visibility = View.GONE
                taxi_request?.visibility = View.VISIBLE
            }
        }
    }

    private fun drawRoad(road: Road) {
        var roadOverlay = RoadManager.buildRoadOverlay(road)
        map?.overlays?.add(roadOverlay)
        updateUserIconOnMap()
        val endMarker = Marker(map)
        val markerIcon = ResourcesCompat.getDrawable(resources, R.drawable.location_marker, null)
        endMarker.position = endGp
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        endMarker.setIcon(markerIcon)
        endMarker.snippet = ("%.2f".format(road.mDuration/60)) + " min"
        endMarker.subDescription = ("%.2f".format(road.mLength)) + " km"
        map?.overlays?.add(endMarker)
        endMarker.showInfoWindow()
        map?.zoomToBoundingBox(road.mBoundingBox, true)
        map?.invalidate()
    }

    private fun requestTaxi() {
        val client = OkHttpClient.Builder()
                .addInterceptor(MyInterceptor())
                .build()

        //route request to orsm server
        val retrofit = Retrofit.Builder()
                .baseUrl(Env.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        serverAPI = retrofit.create(ServerAPI::class.java)
        val jsonObject = getRoadJsonObject()
        val serverCall = serverAPI?.createRoute(jsonObject)

        serverCall?.enqueue(object: Callback<JsonObject>{
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                Log.d("server response", "Failed")
                Toast.makeText(applicationContext, "fail to post on server", Toast.LENGTH_SHORT).show()

            }

            override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
               Log.d("server response", String.format("Server response %s",
                       response.toString()))
                if (response?.code()!! >= 200 && response?.code()!! < 210) {
                    try {
                        routeId = response.body()?.get("_id")?.asString
//                        val routePayload : JsonObject = JsonObject()
//                        routePayload.addProperty("Start", mCurrentLocation.toDoubleString())
//                        routePayload.addProperty("End", endGp!!.toDoubleString())
//                        routePayload.addProperty("routeId", routeId)
//                        routePayload.addProperty("clientId", clientId)
//                        socket.emit("REQUEST ROAD", routePayload)
                        onRoute = true
                        Log.d("activity",String.format("id route response %s ", response.body().toString()))
                        findViewById<LinearLayout>(R.id.edit_lLayout)
                                .visibility = View.GONE
                        taxi_request?.visibility = View.GONE
                        val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
                        fab.visibility = View.VISIBLE

                        val mapController = map?.controller
                        mapController?.animateTo(mCurrentLocation)
                        mapController?.zoomTo(17)

                        actMenu?.findItem(R.id.action_alert)?.isEnabled = true
                        actMenu?.findItem(R.id.action_alert)?.icon
                                ?.mutate()?.setTint(Color.parseColor("#E7291E"))

                        Toast.makeText(applicationContext, "Server response OK", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.d("activity",String.format("exception on request taxi: %s ", e.message))
                    }
                }
            }

        })
    }

    private fun getRoadJsonObject(): JsonObject {
        val nodes = currentRoad?.mNodes
        val json = JsonObject()
        val jsonPoints = JsonArray()
        val start = JsonObject()
        val end = JsonObject()
        val coorStart = JsonArray()
        val coorEnd = JsonArray()
        coorStart.add(mCurrentLocation.longitude)
        coorStart.add(mCurrentLocation.latitude)
        coorEnd.add(endGp?.longitude)
        coorEnd.add(endGp?.longitude)
        start.addProperty("type", "Point")
        start.add("coordinates", coorStart)
        end.addProperty("type", "Point")
        end.add("coordinates", coorEnd)
        if(nodes!=null){
            for(n in nodes.iterator()){
                val jsonArr = JsonArray()
                jsonArr.add(n.mLocation.longitude)
                jsonArr.add(n.mLocation.latitude)
                jsonPoints.add(jsonArr)
            }
        }
        json.addProperty("client", clientId)
        json.add("start", start)
        json.add("end", end)
        json.add("points", jsonPoints)

        return json
    }

    inner class MyInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response? {
            val request = chain.request()

            val t1 = System.nanoTime()
            Log.d("activity",String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()))

            val response = chain.proceed(request)

            val t2 = System.nanoTime()
            Log.d("activity", String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6, response.headers()))

            return response
        }
    }


}
