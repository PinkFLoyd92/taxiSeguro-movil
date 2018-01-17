package com.example.geotaxi.geotaxi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.example.geotaxi.geotaxi.API.endpoints.GeocoderNominatimAPI
import com.example.geotaxi.geotaxi.API.endpoints.OSRMRoadAPI
import com.example.geotaxi.geotaxi.API.endpoints.RouteAPI
import com.example.geotaxi.geotaxi.AddressListViewAdapter
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.config.GeoConstant
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.map.MapHandler
import com.example.geotaxi.geotaxi.socket.SocketIOClientHandler
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.JsonObject
import org.osmdroid.bonuspack.routing.Road
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    var geocoderApi: GeocoderNominatimAPI = GeocoderNominatimAPI()
    var roadApi: OSRMRoadAPI? = null
    var routeAPI: RouteAPI = RouteAPI()
    var mCurrentLocation: GeoPoint = GeoPoint(-2.1811931,-79.8765573)//Guayaquil
    var endGp: GeoPoint? = null
    var locationName = ""
    var mapHandler: MapHandler? = null
    var addressRecyclerView: RecyclerView? = null
    var addressCardView: CardView? = null
    var taxi_request: Button? = null
    var currentRoad: Road? = null
    var sockethandler = SocketIOClientHandler()
    var mFusedLocationClient: FusedLocationProviderClient? = null
    var mLocationRequest: LocationRequest? = null
    var mLocationCallback: LocationCallback? = null
    var onRoute = false
    var actMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        setContentView(R.layout.activity_main)
        roadApi = OSRMRoadAPI(this)
        addressCardView = findViewById(R.id.address_card_view)
        addressRecyclerView = findViewById(R.id.address_recycler_view)
        taxi_request = findViewById(R.id.taxi_request_button)
        val map = findViewById<MapView>(R.id.map)
        val searchEV = findViewById<EditText>(R.id.search)
        val search = findViewById<EditText>(R.id.search)
        val userIcon = ResourcesCompat.getDrawable(resources, R.drawable.user_location, null)
        val destinationIcon = ResourcesCompat.getDrawable(resources, R.drawable.location_marker, null)
        val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
        val geocoderBtn = findViewById<ImageButton>(R.id.geocoder_btn)

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

        fab.setOnClickListener {
            mapHandler?.mapController?.animateTo(mCurrentLocation)
            mapHandler?.mapController?.zoomTo(17)
        }

        geocoderBtn.setOnClickListener{
            locationName = search.text.toString().trim()
            if (locationName !== "")
                fillAddressesRecyclerView()
        }
        mapHandler = MapHandler(mapView = map, userIcon = userIcon,
                                    driverIcon = userIcon, destinationIcon = destinationIcon )
        mapHandler?.mapController?.animateTo(mCurrentLocation)
        mapHandler?.mapController?.zoomTo(17)
        sockethandler.initConfiguration(this)

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
                        GeoConstant.MY_PERMISSIONS_REQUEST_LOCATION)
            }

        } else {
            initLocationrequest()
        }

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
            GeoConstant.MY_PERMISSIONS_REQUEST_LOCATION -> {
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
                    GeoConstant.MY_PERMISSIONS_REQUEST_LOCATION)
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
                            GeoConstant.REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException ) {
                    Log.d("activity",String.format("exception on start resolution for results: %s ", sendEx.message))
                }
            }
        }

    }

    //Getting a result from MainActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // Check which request we're responding to
        if (requestCode == GeoConstant.REQUEST_CHECK_SETTINGS) {
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
        mLocationRequest?.interval = GeoConstant.LOCATION_REQUEST_INTERVAL
        mLocationRequest?.fastestInterval = GeoConstant.LR_FASTEST_INTERVAL
        mLocationRequest?.priority = GeoConstant.LR_PRIORITY
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
            mapHandler?.mapController?.animateTo(mCurrentLocation)
            mapHandler?.mapController?.zoomTo(17)
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
        mapHandler?.updateUserIconOnMap(currentLocation)
        if (onRoute) {
            val data = JsonObject()
            val pos = JsonObject()
            pos.addProperty("longitude", currentLocation.longitude)
            pos.addProperty("latitude", currentLocation.latitude)
            data.add("position", pos)
            data.addProperty("route_id", Route.instance._id)
            data.addProperty("role", User.instance.role)
            try {
                sockethandler.socket.emit("POSITION", data)
            } catch (e: Exception) {
                Log.d("activity",String.format("exception on location change: %s ", e.message))
            }
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
                data.addProperty("route_id", Route.instance._id)
                try {
                    sockethandler.socket.emit("PANIC BUTTON", data)
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
                locationName = searchEV.text.toString().trim()
                if (locationName == "") return false
                fillAddressesRecyclerView()
                return true
            }

            return false
        }
    }

    private fun fillAddressesRecyclerView() {
        val addresses = geocoderApi!!.fromLocationName(locationName)
        if (addresses.isNotEmpty()) {
            endGp = GeoPoint(addresses.get(0).latitude, addresses.get(0).longitude)
            // specify an adapter
            val mAdapter = AddressListViewAdapter(addresses, MyRVClickListener())
            addressRecyclerView?.setAdapter(mAdapter)
            addressCardView?.visibility = View.VISIBLE
            taxi_request?.visibility = View.GONE

        } else {
            Toast.makeText(this, "No se encontró la dirección", Toast.LENGTH_LONG).show()
            Log.d("activity","fail to match location name")
        }
    }



    //Class that handle click action on an item of the list view of addresses
    inner class MyRVClickListener : View.OnClickListener {

        override fun onClick(v: View?) {
            val geoPointStr = v?.findViewById<TextView>(R.id.location_tv)?.text.toString()
            var geoPointList = geoPointStr.split(",").map { it.trim() }
            endGp = GeoPoint((geoPointList[0]).toDouble(), geoPointList[1].toDouble())
            addressCardView?.visibility = View.GONE
            mapHandler?.map?.overlays?.clear()
            //calculate and draw road on map
            executeRoadTask()
        }

    }

    private  fun executeRoadTask(){
        val roadRusult = roadApi?.getRoad(mCurrentLocation, endGp!!)
        if (roadRusult != null) {
            currentRoad = roadRusult
            mapHandler?.drawRoad(roadRusult, mCurrentLocation, endGp!!)
            val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
            fab.visibility = View.GONE
            taxi_request?.visibility = View.VISIBLE
        }
    }

    private fun requestTaxi() {

        val serverCall = routeAPI.requestTaxi(mCurrentLocation, endGp!!, currentRoad!!.mNodes)
        if(serverCall != null){

            serverCall?.enqueue(object: Callback<JsonObject> {

                override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                    Log.d("server response", "Failed")
                    Toast.makeText(applicationContext, "fail to post on server", Toast.LENGTH_SHORT).show()

                }

                override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
                   Log.d("server response", String.format("Server response %s",
                           response.toString()))
                    if (response?.code()!! in 200..209) {
                        try {
                            val routeId = response.body()?.get("_id")?.asString
                            sockethandler.socket.emit("JOIN ROUTE", Route.instance._id)
                            if (routeId != null) {
                                Route.instance._id = routeId
                            }
                            onRoute = true
                            Log.d("activity",String.format("id route response %s ", response.body().toString()))
                            findViewById<LinearLayout>(R.id.edit_lLayout)
                                    .visibility = View.GONE
                            taxi_request?.visibility = View.GONE
                            val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
                            fab.visibility = View.VISIBLE

                            mapHandler?.mapController?.animateTo(mCurrentLocation)
                            mapHandler?.mapController?.zoomTo(17)

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
        } else {
            Log.d("RETROFIT", "ServerCAll is null")
        }
    }

}
