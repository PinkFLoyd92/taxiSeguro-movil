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
import android.graphics.Color
import android.provider.Settings
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.IOException
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    val MIN_TIME: Long = 10000
    val MIN_DISTANCE: Float = 10F
    val MY_PERMISSIONS_REQUEST_LOCATION = 1
    val locationListener = MyLocationListener()
    val startGp = GeoPoint(-2.2284,-79.8990)
    val endGp = GeoPoint(-2.1478,-79.9655)
    var map: MapView? = null
    var osrmAPI: OsrmAPI? = null


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
            val mapController = map?.controller
            val startPoint = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (startPoint !== null) {
                val currentLocation = GeoPoint(startPoint)
                mapController?.setCenter(startGp)
                mapController?.setZoom(17)
            }

            getRoute()
        }

    }

    fun getRoute() {
        val client = OkHttpClient.Builder()
                .addInterceptor(MyInterceptor())
                .build()

        //route request to orsm server
        val retrofit = Retrofit.Builder()
                .baseUrl("http://router.project-osrm.org/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(client)
                .build()
        osrmAPI = retrofit.create(OsrmAPI::class.java)

        val osrmCall = osrmAPI?.getRoute(
                startGp.longitude.toString(),
                startGp.latitude.toString(),
                endGp.longitude.toString(),
                endGp.latitude.toString())

        osrmCall?.enqueue(MyCallback())
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

                        getRoute()
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

    inner class MyCallback : Callback<String> {

        override fun onResponse(call: Call<String>?, response: Response<String>?) {
            drawBestRoute(response)
        }

        override fun onFailure(call: Call<String>?, t: Throwable) {

            Log.d("activity",String.format("Failure %s",t.message))
        }
    }

    private fun drawBestRoute(response: Response<String>?) {
        val startMarker = Marker(map)
        startMarker.setPosition(startGp)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.getOverlays()?.add(startMarker)

        val endMarker = Marker(map)
        endMarker.setPosition(endGp)
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.getOverlays()?.add(endMarker)

        var line = Polyline()
        line.width = 10f
        line.color = Color.argb(90, 0,0,254)
        line.setGeodesic(true)

        var path = ArrayList<GeoPoint>(0)
        val root = JSONObject(response?.body())
        val routes = root.getJSONArray("routes")
        val route = routes.getJSONObject(0)
        val legs = route.getJSONArray("legs")
        val leg = legs.getJSONObject(0)
        val steps = leg.getJSONArray("steps")
        for (i in 0..(steps.length()-1)){
            val step = steps.getJSONObject(i)
            val intersections = step.getJSONArray("intersections")
            for (j in 0..(intersections.length()-1)){
                val intersection = intersections.getJSONObject(j)
                val location = intersection.getJSONArray("location")
                val longitude = location[0] as Double
                val latitude = location[1] as Double
                path.add(GeoPoint(latitude, longitude))

            }
        }

        line.points = path
        map?.overlays?.add(line)
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
