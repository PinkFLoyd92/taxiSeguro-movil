package com.example.geotaxi.geotaxi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import android.preference.PreferenceManager
import android.view.View
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.location.LocationManager
import android.location.Location
import android.location.LocationListener
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.content.Intent
import android.os.AsyncTask
import android.provider.Settings
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Marker
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    val MIN_TIME: Long = 10000
    val MIN_DISTANCE: Float = 10F
    val MY_PERMISSIONS_REQUEST_LOCATION = 1
    val ROUTE_SERVICE_URL = "http://192.168.0.115:5000/route/v1/car/"
    val locationListener = MyLocationListener()
    val startGp = GeoPoint(-2.17825,-79.82591)
    val endGp = GeoPoint(-2.14045,-79.86407)
    var map: MapView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_main)
        map = findViewById<View>(R.id.map) as MapView
        map?.setTileSource(TileSourceFactory.MAPNIK)
        map?.setBuiltInZoomControls(true)
        map?.setMultiTouchControls(true)
        val mapController = map?.controller
        mapController?.setCenter(startGp)
        mapController?.setZoom(17)
        // check access location permission
        if (ContextCompat.checkSelfPermission(this,

                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        MY_PERMISSIONS_REQUEST_LOCATION)
            }

        } else {

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (isGPSEnable) {
                //Request location updates:
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener)
            } else {

                Log.d("activity","gps not enable")
                enableLocationSettings()
            }

            val startPoint = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (startPoint !== null) {
                val currentLocation = GeoPoint(startPoint)
            }

            //get and draw road on the map
            executeRoadTask()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        if (isGPSEnable) {

                            //Request location updates:
                            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener)
                        } else {

                            Log.d("activity","gps not enable")
                            enableLocationSettings()
                        }

                        //get and draw road on the map
                        executeRoadTask()
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
        }
    }

    private fun enableLocationSettings() {
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(settingsIntent)
    }

    private  fun executeRoadTask(){
        val roadTask = RoadTask()
        roadTask.execute(applicationContext)
    }
    
    private fun drawRoad(road: Road) {
        var roadOverlay = RoadManager.buildRoadOverlay(road)
        map?.overlays?.add(roadOverlay)
        val startMarker = Marker(map)
        startMarker.setPosition(startGp)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.getOverlays()?.add(startMarker)

        val endMarker = Marker(map)
        endMarker.setPosition(endGp)
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.getOverlays()?.add(endMarker)
    }

    inner class MyLocationListener : LocationListener {

        override fun onLocationChanged(location: Location) {
            val currentLocation = GeoPoint(location)
            val mapController = map?.controller

                mapController?.setCenter(currentLocation)
            mapController?.setZoom(17)

        }

        override fun onProviderDisabled(provider: String) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    }

    inner class RoadTask : AsyncTask<Context, Void, Road>() {
        override fun doInBackground(vararg params: Context?): Road {
            val roadManager = OSRMRoadManager(params[0])
            val wayPoints = ArrayList<GeoPoint>(0)
            wayPoints.add(startGp)
            wayPoints.add(endGp)
            roadManager.setService(ROUTE_SERVICE_URL)
            val road = roadManager.getRoad(wayPoints)
            return road
        }

        override fun onPostExecute(result: Road) {
            super.onPostExecute(result)
            drawRoad(result)
        }
    }
}
