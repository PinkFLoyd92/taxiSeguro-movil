package com.example.geotaxi.geotaxi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.DialogFragment
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.example.geotaxi.geotaxi.API.endpoints.GeocoderNominatimAPI
import com.example.geotaxi.geotaxi.API.endpoints.OSRMRoadAPI
import com.example.geotaxi.geotaxi.API.endpoints.RouteAPI
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.Road.RoadHandler
import com.example.geotaxi.geotaxi.Road.ScoreController
import com.example.geotaxi.geotaxi.chat.controller.ChatController
import com.example.geotaxi.geotaxi.chat.model.ChatList
import com.example.geotaxi.geotaxi.chat.view.ChatDialog
import com.example.geotaxi.geotaxi.chat.view.ChatView
import com.example.geotaxi.geotaxi.config.GeoConstant
import com.example.geotaxi.geotaxi.data.Driver
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.map.MapHandler
import com.example.geotaxi.geotaxi.socket.SocketIOClientHandler
import com.example.geotaxi.geotaxi.ui.adapter.AddressListViewAdapter
import com.example.geotaxi.geotaxi.utils.Utility
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.JsonObject
import com.stepstone.apprating.listener.RatingDialogListener
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Polyline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.co.chrisjenx.calligraphy.CalligraphyConfig


class MainActivity : AppCompatActivity(), ChatDialog.ChatDialogListener, RatingDialogListener{

    lateinit var chatController : ChatController
    lateinit var scoreController: ScoreController
    private var chatList: ChatList = ChatList()
    var geocoderApi: GeocoderNominatimAPI = GeocoderNominatimAPI()
    var roadApi: OSRMRoadAPI? = null
    var routeAPI: RouteAPI = RouteAPI()
    var startGp: GeoPoint = GeoPoint(-2.1811931,-79.8765573)//Guayaquil
    var locationName = ""
    var mapHandler: MapHandler? = null
    var addressRecyclerView: RecyclerView? = null
    var addressCardView: CardView? = null
    var taxi_request: Button? = null
    var choose_route: Button? = null
    var fabRoutes: FloatingActionButton? = null
    lateinit var routeChangeDialog: View
    lateinit var driverInfoDialog: View
    var sockethandler : SocketIOClientHandler? = null
    var mFusedLocationClient: FusedLocationProviderClient? = null
    var mLocationRequest: LocationRequest? = null
    var mLocationCallback: LocationCallback? = null
    var actMenu: Menu? = null
    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navButton: ImageView
    lateinit var avatarView: CircleImageView
    var canSendPosition = true
    lateinit var roadHandler: RoadHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                    .setDefaultFontPath("fonts/open-sans/OpenSans-Bold.ttf")
                    .setFontAttrId(R.attr.fontPath)
                    .build())

            val ctx = applicationContext
            //important! set your user agent to prevent getting banned from the osm servers
            Configuration.getInstance().userAgentValue = packageName
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
            setContentView(R.layout.activity_main)
            toolbar = findViewById(R.id.toolbar)
            roadApi = OSRMRoadAPI()
            addressCardView = findViewById(R.id.address_card_view)
            addressRecyclerView = findViewById(R.id.address_recycler_view)
            taxi_request = findViewById(R.id.taxi_request_button)
            choose_route = findViewById(R.id.choose_route_btn)
            fabRoutes = findViewById(R.id.fab_routes)
            drawerLayout = findViewById(R.id.drawer_layout)
            avatarView = findViewById<CircleImageView>(R.id.circleView)
            avatarView.setImageBitmap(Utility.getBitmapFromText("Usuario Sebas", "D", 250, 250))

            val map = findViewById<MapView>(R.id.map)
            val searchEV = findViewById<EditText>(R.id.search)
            val search = findViewById<EditText>(R.id.search)
            val userIcon = ResourcesCompat.getDrawable(resources, R.drawable.user_location, null)
            val driverIcon = ResourcesCompat.getDrawable(resources, R.mipmap.taxi_icon, null)
            val destinationIcon = ResourcesCompat.getDrawable(resources, R.drawable.location_marker, null)
            val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
            val customRouteBtn = findViewById<ImageButton>(R.id.custom_route_btn)
            val cancelRouteActionBtn = findViewById<Button>(R.id.cancel_route_action)
            val selectingRouteCV = findViewById<CardView>(R.id.selecting_route)
            val messageLauncher = findViewById<LinearLayout>(R.id.slider_messages)
            val removeCustomRouteInfo = findViewById<ImageButton>(R.id.remove_customRoute_info)
            val okCustomRoute = findViewById<Button>(R.id.ok_customRoute_action)
            val cancelCustomRoute = findViewById<Button>(R.id.cancel_customRoute_action)
            val removeAddressInfo = findViewById<ImageButton>(R.id.remove_address_info)
            val removeMarkerInfo = findViewById<ImageButton>(R.id.remove_destMarker_info)
            removeAddressInfo.setOnClickListener {
                findViewById<CardView>(R.id.search_address_info)
                        .visibility = View.GONE
            }
            removeMarkerInfo.setOnClickListener {
                findViewById<CardView>(R.id.dest_marker_info)
                        .visibility = View.GONE
            }
            okCustomRoute.setOnClickListener {
                if (mapHandler!!.overlaysEvents != null) {
                    mapHandler!!.removeMapOverlay(mapHandler!!.overlaysEvents!!)
                }
                mapHandler!!.initRoadMapEventsOverlay()
                setSearchLayoutVisibility()
                findViewById<LinearLayout>(R.id.custom_route_ly)
                        .visibility = View.GONE
                findViewById<CardView>(R.id.customizing_route_actions)
                        .visibility = View.GONE
                fabRoutes?.visibility = View.VISIBLE
                taxi_request?.visibility = View.VISIBLE
            }
            cancelCustomRoute.setOnClickListener {
                mapHandler!!.resetToRoadMapOverlays()
                setSearchLayoutVisibility()
                findViewById<LinearLayout>(R.id.custom_route_ly)
                        .visibility = View.GONE
                findViewById<CardView>(R.id.customizing_route_actions)
                        .visibility = View.GONE
            }
            removeCustomRouteInfo.setOnClickListener {
                findViewById<CardView>(R.id.custom_route_cv)
                        .visibility = View.GONE
            }
            chatController = ChatController(
                    chatScene = ChatView.ChatScene(activity = this,
                            chatList = this.chatList),
                    activity = this,
                    chatList = this.chatList )
            scoreController = ScoreController(activity = this)
            messageLauncher.setOnClickListener {
                this.startChatDialog()
            }

            // scoreController.showDialog()

            setSupportActionBar(toolbar)
            navButton = findViewById(R.id.nav_button)
            navButton.setOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }

            User.instance.position = startGp
            driverInfoDialog = findViewById(R.id.driver_info_dialog)
            val driverOkBtn = driverInfoDialog.findViewById<Button>(R.id.driver_info_okBtn)
            driverOkBtn.setOnClickListener{
                driverInfoDialog.visibility = View.GONE
            }
            routeChangeDialog = findViewById(R.id.route_change_dialog)
            searchEV.setImeActionLabel("Buscar", KeyEvent.KEYCODE_ENTER)
            searchEV.setOnEditorActionListener(MyEditionActionListener())
            setOnTouchListener(search)

            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            addressRecyclerView?.setHasFixedSize(true)
            // use a linear layout manager
            val mLayoutManager = LinearLayoutManager(this)
            addressRecyclerView?.layoutManager = mLayoutManager
            // add rotation gesture

            customRouteBtn.setOnClickListener {
                if (Route.instance.waypoints.isNotEmpty()) {
                    Route.instance.waypoints.clear()
                }
                if (mapHandler!!.overlaysEvents != null) {
                    mapHandler!!.removeMapOverlay(mapHandler!!.overlaysEvents!!)
                }
                mapHandler?.clearWaypointMarkers()
                mapHandler?.resetToCustomRoadMapOverlays()
                okCustomRoute.isEnabled = false
                okCustomRoute.setTextColor(ResourcesCompat.getColor(resources, R.color.gray, null))
                mapHandler?.onMapEventsOverlay = true
                mapHandler?.clearMapOverlays()
                setSearchLayoutVisibility(visibility = View.GONE)
                findViewById<LinearLayout>(R.id.custom_route_ly)
                        .visibility = View.VISIBLE
                findViewById<CardView>(R.id.customizing_route_actions)
                        .visibility = View.VISIBLE
                fabRoutes!!.visibility = View.GONE
                taxi_request?.visibility = View.GONE
                findViewById<CardView>(R.id.search_address_info)
                        .visibility = View.GONE
                findViewById<CardView>(R.id.dest_marker_info)
                        .visibility = View.GONE
                Route.instance.waypoints.add(User.instance.position!!)
            }
            mapHandler = MapHandler(this, mapView = map, userIcon = userIcon,
                    driverIcon = driverIcon, destinationIcon = destinationIcon )
            mapHandler!!.initRoadMapEventsOverlay()
            roadHandler = RoadHandler()
            sockethandler = SocketIOClientHandler(this, mapHandler!!)
            sockethandler!!.initConfiguration()
            chatController.socketHandler = sockethandler!!
            taxi_request?.setOnClickListener { requestTaxi() }
            choose_route?.setOnClickListener {
                mapHandler?.onMapEventsOverlay = true
                selectingRouteCV.visibility = View.GONE
                taxi_request?.visibility = View.VISIBLE
                fab.visibility = View.VISIBLE
                fabRoutes?.visibility = View.VISIBLE
                setSearchLayoutVisibility(View.VISIBLE)
                routeChosen()
            }
            cancelRouteActionBtn.setOnClickListener{
                mapHandler?.onMapEventsOverlay = true
                fab.visibility = View.VISIBLE
                fabRoutes?.visibility = View.VISIBLE
                taxi_request?.visibility = View.VISIBLE
                choose_route?.isEnabled = false
                selectingRouteCV.visibility = View.GONE
                setSearchLayoutVisibility(View.VISIBLE)
                mapHandler?.clearMapOverlays()
                mapHandler?.drawRoad(Route.instance.currentRoad!!, User.instance.position!!, Route.instance.end!!)
                mapHandler?.addDestMarker(Route.instance.end!!)
            }

            fabRoutes?.setOnClickListener {
                if (Route.instance.roads!!.size > 1) {
                    choose_route?.setTextColor(
                            ResourcesCompat.getColor(resources!!, R.color.dark_gray, null)
                    )
                    choose_route?.isEnabled = false
                    mapHandler?.onMapEventsOverlay = false
                    fab.visibility = View.GONE
                    fabRoutes?.visibility = View.GONE
                    selectingRouteCV.visibility = View.VISIBLE
                    mapHandler?.clearMapOverlays()
                    mapHandler?.updateUserIconOnMap(User.instance.position!!)
                    mapHandler?.addDestMarker(Route.instance.end!!)
                    mapHandler?.drawRoads(Route.instance.roads!!)
                    taxi_request?.visibility = View.GONE
                    setSearchLayoutVisibility(View.GONE)
                } else {
                    Toast.makeText(this, "No se encontraron rutas alternativas disponibles", Toast.LENGTH_LONG).show()
                }
            }
            fab.setOnClickListener {
                mapHandler?.animateToLocation(location = User.instance.position, zoomLevel = 17)
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
                            GeoConstant.MY_PERMISSIONS_REQUEST_LOCATION)
                }

            } else {
                initLocationrequest()
            }

    }

    fun showDriverInfoDialog(name: String, vehiclePlate: String ) {
        val nameTv = driverInfoDialog.findViewById<TextView>(R.id.driver_name)
        val vehiclePlateTv = driverInfoDialog.findViewById<TextView>(R.id.vehicle_plate)
        nameTv.text = name
        vehiclePlateTv.text = vehiclePlate
        driverInfoDialog.visibility = View.VISIBLE
    }

    fun showRouteChangeDialog(roadOverlay: Polyline, duration: Double, routeIndex: Int) {
        val okRouteBtn = routeChangeDialog.findViewById<Button>(R.id.route_ok)
        val cancelRouteBtn = routeChangeDialog.findViewById<Button>(R.id.route_cancel)
        cancelRouteBtn?.setOnClickListener{
            mapHandler?.clearMapOverlays()
            mapHandler?.drawRoad(Route.instance.currentRoad!!, User.instance.position!!, Route.instance.end!!)
            mapHandler?.addDestMarker(Route.instance.end!!)
            routeChangeDialog.visibility = View.GONE
            val route = JSONObject()
            route.put("routeId", Route.instance._id)
            route.put("driverId", Driver.instance._id)
            sockethandler?.socket?.emit("ROUTE CHANGE - RESULT", "cancel", route)
        }
        okRouteBtn?.setOnClickListener{
            allowRouteChange(roadOverlay, duration, routeIndex)
            routeChangeDialog.visibility = View.GONE
        }
        routeChangeDialog.visibility = View.VISIBLE
    }

    private fun allowRouteChange(roadOverlay: Polyline, duration: Double, routeIndex: Int) {
        val points = roadOverlay.points as ArrayList<GeoPoint>
        val serverCall = routeAPI.createRoute(
                            location = User.instance.position!!, destination = Route.instance.end!!, client = User.instance._id,
                            points = points, routeIndex = routeIndex, status = "active", taxiRequest = false, driver = Driver.instance._id,
                            supersededRoute = Route.instance._id, waypoints = null, duration = duration)
        if(serverCall != null){
            serverCall.enqueue(object: Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                    Log.d("server response", "Failed")
                    Toast.makeText(applicationContext, "Ocurrió un error", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {

                    if (response?.code()!! in 200..209) {
                        val routeId = response.body()?.get("_id")?.asString
                        if (routeId != null) {
                            Route.instance._id = routeId
                            /*Route.instance.currentRoad = newRoads[routeIndex]
                            Route.instance.roads = newRoads*/
                            Route.instance.currentRoadIndex = routeIndex
                            Route.instance.roadPoints = points
                            mapHandler?.clearMapOverlays()
                            mapHandler?.drawRoadOverlay(roadOverlay, duration)
                            //mapHandler?.drawRoad(newRoads[routeIndex], User.instance.position!!, Route.instance.end!!)
                        }
                    }
                }
            })
        }
    }
    fun enablePanicButton() {
        actMenu?.findItem(R.id.action_alert)?.isEnabled = true
        /*actMenu?.findItem(R.id.action_alert)?.icon
                ?.mutate()?.setTint(Color.parseColor("#E7291E"))*/
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
            mapHandler?.animateToLocation(location = User.instance.position, zoomLevel = 17)
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
        User.instance.position = currentLocation
        mapHandler?.updateUserIconOnMap(currentLocation)
        if (Route.instance.status in listOf("active", "pending") && canSendPosition) {
            val data = JsonObject()
            val pos = JsonObject()
            pos.addProperty("longitude", currentLocation.longitude)
            pos.addProperty("latitude", currentLocation.latitude)
            data.add("position", pos)
            data.addProperty("route_id", Route.instance._id)
            data.addProperty("role", User.instance.role)
            data.addProperty("userId", User.instance._id)
            try {
                sockethandler!!.socket.emit("POSITION", data)
            } catch (e: Exception) {
                Log.d("activity",String.format("exception on location change: %s ", e.message))
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_alert)?.isEnabled = false
        /* menu?.findItem(R.id.action_alert)?.icon
                 ?.mutate()?.setTint(Color.GRAY)*/
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
                data.addProperty("role", "client")
                try {
                    sockethandler!!.socket.emit("PANIC BUTTON", data)
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
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val searchET = findViewById<EditText>(R.id.search)
                imm.hideSoftInputFromWindow(searchET.windowToken, 0)
                fillAddressesRecyclerView()
                return true
            }

            return false
        }
    }

    private fun fillAddressesRecyclerView() {
        val addresses = geocoderApi.fromLocationName(locationName)
        if (addresses.isNotEmpty()) {
            Route.instance.end = GeoPoint(addresses.get(0).latitude, addresses.get(0).longitude)
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
            Route.instance.end = GeoPoint((geoPointList[0]).toDouble(), geoPointList[1].toDouble())
            addressCardView?.visibility = View.GONE
            mapHandler?.clearMapOverlays()
            //calculate and draw road on map
            val waypoints = arrayListOf<GeoPoint>(User.instance.position!!, Route.instance.end!!)
            val roads = roadHandler.executeRoadTask(waypoints)
            if (roads != null && roads.isNotEmpty()
                    && roads[0].mStatus == Road.STATUS_OK) {
                val points = RoadManager.buildRoadOverlay(roads[0]).points as ArrayList<GeoPoint>
                Route.instance.roadPoints = points
                Route.instance.currentRoad = roads[0]
                Route.instance.roads = roads
                Route.instance.waypoints = waypoints
                mapHandler?.drawRoad(roads[0], User.instance.position!!, Route.instance.end!!)
                mapHandler?.addDestMarker(Route.instance.end!!)
                fabRoutes?.visibility = View.VISIBLE
                taxi_request?.visibility = View.VISIBLE
            }
        }

    }


    private fun requestTaxi() {
        val getScore = {
            points: ArrayList<GeoPoint> ->
            routeAPI.getScore(points)
        }
        scoreController.getScore(Route.instance, getScore)
        val waitingDriver = findViewById<CardView>(R.id.waiting_driver_cv)
        waitingDriver.visibility = View.VISIBLE
        val serverCall =
                routeAPI.createRoute(
                        location = User.instance.position!!, destination = Route.instance.end!!, client = User.instance._id,
                        points = Route.instance.roadPoints, routeIndex = Route.instance.currentRoadIndex, status = "pending",
                        taxiRequest = true, driver = null, supersededRoute = null, waypoints = Route.instance.waypoints,
                        duration = Route.instance.currentRoad!!.mDuration)
        if(serverCall != null){
            canSendPosition = false
            serverCall.enqueue(object: Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                    Log.d("server response", "Failed")
                    Toast.makeText(applicationContext, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
                }
                override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {

                    if (response?.code()!! in 200..209) {
                        try {
                            val routeId = response.body()?.get("_id")?.asString
                            if (routeId != null) {
                                sockethandler?.isFirstDriverPosition = true
                                mapHandler?.onMapEventsOverlay = false
                                Route.instance._id = routeId
                                canSendPosition = true
                                sockethandler!!.socket.emit("JOIN ROUTE", Route.instance._id)
                                Route.instance.status = "active"
                                Log.d("activity",String.format("id route response %s ", response.body().toString()))
                                setSearchLayoutVisibility(View.GONE)
                                taxi_request?.visibility = View.GONE
                                fabRoutes?.visibility = View.GONE
                                val fab = findViewById<FloatingActionButton>(R.id.fab_mlocation)
                                fab.visibility = View.VISIBLE
                            }

                            Toast.makeText(applicationContext, "Solicitud enviada", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.d("activity",String.format("exception on request taxi: %s ", e.message))
                        }
                    } else {
                        sockethandler!!.socket.emit("ROUTE DELETE", User.instance._id)
                        waitingDriver.visibility  = View.GONE
                        Toast.makeText(applicationContext, "No hay conductores disponibles", Toast.LENGTH_SHORT).show()
                    }
                }

            })
        } else {
            Log.d("RETROFIT", "ServerCAll is null")
        }
    }

    private fun routeChosen() {
        val roadChosen = mapHandler?.getRoadChosen()
        if (roadChosen != null) {
            Log.d("road", "Road chosen not null")
            Route.instance.currentRoad = roadChosen
            val points = RoadManager.buildRoadOverlay(roadChosen).points as ArrayList<GeoPoint>
            Route.instance.roadPoints = points
        }
        Route.instance.currentRoadIndex = mapHandler?.getRoadIndexChosen()!!
        mapHandler?.clearMapOverlays()
        mapHandler?.drawRoad(Route.instance.currentRoad!!, User.instance.position!!, Route.instance.end!!)
        mapHandler?.addDestMarker(Route.instance.end!!)
    }

    fun setSearchLayoutVisibility(visibility: Int = View.VISIBLE) {
        findViewById<CardView>(R.id.address_search_layout)
                .visibility = visibility
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)){
            drawerLayout.closeDrawer(Gravity.LEFT)
        }
        else if (drawerLayout.isDrawerOpen(GravityCompat.END)){
            drawerLayout.closeDrawer(Gravity.RIGHT)
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
        dialog.dismiss()
    }

    private fun startChatDialog() {
        chatController.onStart()
    }

    override fun onNegativeButtonClicked() {
    }

    override fun onNeutralButtonClicked() {
    }

    override fun onPositiveButtonClicked(rate: Int, comment: String) {
        scoreController.setScoreAndEmit(Route.instance._id, rate, sockethandler!!.emitScore)
    }
}
