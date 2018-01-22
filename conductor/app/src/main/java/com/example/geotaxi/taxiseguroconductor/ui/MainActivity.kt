package com.example.geotaxi.taxiseguroconductor.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.taxiseguroconductor.config.Env
import com.example.geotaxi.taxiseguroconductor.config.GeoConstant
import com.example.geotaxi.taxiseguroconductor.config.GeoConstant.Companion.MY_PERMISSIONS_REQUEST_LOCATION
import com.example.geotaxi.taxiseguroconductor.data.Route
import com.example.geotaxi.taxiseguroconductor.data.User
import com.example.geotaxi.taxiseguroconductor.map.MapHandler
import com.example.geotaxi.taxiseguroconductor.socket.SocketIODriverHandler
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.gson.JsonObject
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {
    var mCurrentLocation: GeoPoint? = GeoPoint(-2.1811931,-79.8765573)// Default Position
    var map: MapView? = null
    var mapHandler: MapHandler? = null
    var sockethandler = SocketIODriverHandler()
    var mFusedLocationClient : FusedLocationProviderClient? = null
    var mLocationRequest : LocationRequest? = null
    var mLocationCallback : LocationCallback? = null
    var driverIcon : Drawable? = null
    var userIcon : Drawable? = null
    var driverMarker: Marker? = null
    var clientMarker: Marker? = null
    var onRoute = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        setContentView(R.layout.activity_main)
        val mapController = this.initMap()
        driverIcon = ResourcesCompat.getDrawable(resources, R.mipmap.ic_driver, null)
        userIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_user, null)
        driverMarker = Marker(this.map)
        driverMarker?.setIcon(driverIcon)
        clientMarker = Marker(this.map)
        clientMarker?.setIcon(userIcon)
        mapHandler = MapHandler(mapView = map, clientMarker = clientMarker, driverMarker = driverMarker, mapController = mapController, mCurrentLocation = mCurrentLocation)
        sockethandler.initConfiguration(this, mapHandler as MapHandler)

        // check access location permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionExplanation()
                Toast.makeText(this, "PERMISSION DENIED", Toast.LENGTH_SHORT).show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_LOCATION)
            }
        } else {
            initLocationrequest()
        }
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
                Toast.makeText(this, "Alerta enviada", Toast.LENGTH_SHORT).show()
                //Do alert actions here
            }
            }
            alertDialog.show()
        }
        return super.onOptionsItemSelected(item)
    }
    fun initMap() : IMapController? {
        this.map = findViewById<View>(R.id.map) as MapView
        this.map?.setTileSource(TileSourceFactory.MAPNIK)
        this.map?.setMultiTouchControls(true)
        val mapController = this.map?.controller
        mapController?.setCenter(this.mCurrentLocation)
        mapController?.setZoom(17)
        return mapController
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == Env.REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationRequest()
            }
        }
    }

    private  fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest?.interval = GeoConstant.LOCATION_REQUEST_INTERVAL
        mLocationRequest?.fastestInterval = GeoConstant.LR_FASTEST_INTERVAL
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
        Log.d("MAIN - LOCATION",String.format("locations: %s ", "" + location.toString()))
        mCurrentLocation?.latitude = location.latitude
        mCurrentLocation?.longitude = location.longitude
        mCurrentLocation?.altitude = location.altitude
        User.instance.position = GeoPoint(location)
        if(mapHandler != null) {
            mapHandler?.updateDriverIconOnMap(this.mCurrentLocation as GeoPoint)
        }else {
            Log.d("ERROR - LOCATION",String.format("locations: %s ", "" + location.toString()))
        }
        if (Route.instance.routeObj != null) {
            val data = JsonObject()
            val pos = JsonObject()
            pos.addProperty("longitude", this.mCurrentLocation?.longitude)
            pos.addProperty("latitude", this.mCurrentLocation?.latitude)
            data.add("position", pos)
            data.addProperty("route_id", Route.instance._id)
            data.addProperty("role", User.instance.role)
            data.addProperty("userId", User.instance._id)
            try {
                sockethandler.socket.emit("POSITION", data)
                this.mapHandler?.updateDriverIconOnMap(mCurrentLocation as GeoPoint)
            } catch (e: Exception) {
                Log.d("activity",String.format("exception on location change: %s ", e.message))
            }
        }
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

    private fun initLocationrequest(){
        createLocationRequest()
        mLocationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationChanged(locationResult.lastLocation)
            }

            override fun onLocationAvailability(p0: LocationAvailability?) {
                super.onLocationAvailability(p0)
                Log.d("activity",String.format("availability %s", p0?.toString()))
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
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("activity", String.format("exception on start resolution for results: %s ", sendEx.message))
                }
            }
        }
    }

    public fun acceptRoute(view : View) {
        this.findViewById<CardView>(R.id.card_view_confirm_client).visibility = View.GONE
    }

}

