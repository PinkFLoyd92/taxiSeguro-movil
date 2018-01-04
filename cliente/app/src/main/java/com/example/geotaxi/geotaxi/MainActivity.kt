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
import android.location.Address
import android.os.AsyncTask
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.osmdroid.bonuspack.location.GeocoderNominatim
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Marker
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    val MIN_TIME: Long = 5000
    val MIN_DISTANCE: Float = 10F
    val MY_PERMISSIONS_REQUEST_LOCATION = 1
    val ROUTE_SERVICE_URL = "http://192.168.0.107:5000/route/v1/car/"
    val NOMINATIM_SERVICE_URL = "http://192.168.0.107:80/nominatim/"
    val locationListener = MyLocationListener()
    var startGp: GeoPoint = GeoPoint(-2.1811931,-79.8765573)//Guayaquil
    var endGp: GeoPoint? = null
    var locationName = ""
    var map: MapView? = null
    var addressRecyclerView: RecyclerView? = null
    var addressCardView: CardView? = null
    var taxi_request: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        setContentView(R.layout.activity_main)

        val searchEV = findViewById<EditText>(R.id.search)
        searchEV.setImeActionLabel("Buscar", KeyEvent.KEYCODE_ENTER)
        searchEV.setOnEditorActionListener(MyEditionActionListener())
        addressCardView = findViewById<CardView>(R.id.address_card_view)
        addressRecyclerView = findViewById<RecyclerView>(R.id.address_recycler_view)
        taxi_request = findViewById<Button>(R.id.taxi_request_button)
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        addressRecyclerView?.setHasFixedSize(true)
        // use a linear layout manager
        val mLayoutManager = LinearLayoutManager(this)
        addressRecyclerView?.setLayoutManager(mLayoutManager)

        map = findViewById<View>(R.id.map) as MapView
        map?.setTileSource(TileSourceFactory.MAPNIK)
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
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener)
            } else {

                Log.d("activity","gps not enable")
                enableLocationSettings()
            }

            val startPoint = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (startPoint !== null) {
                startGp = GeoPoint(startPoint)
                mapController?.setCenter(startGp)
            }
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
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener)
                        } else {

                            Log.d("activity","gps not enable")
                            enableLocationSettings()
                        }
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
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

    //Go to the device settings for let user enable gps location
    private fun enableLocationSettings() {
        val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(settingsIntent)
    }

    //Class that handle location changes
    inner class MyLocationListener : LocationListener {

        override fun onLocationChanged(location: Location) {
            val currentLocation = GeoPoint(location)
            startGp = currentLocation
        }

        override fun onProviderDisabled(provider: String) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    }

    //Class that handle user input on address search
    inner class MyEditionActionListener : TextView.OnEditorActionListener {
        override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if ((event?.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val searchEV = v as EditText
                locationName = searchEV.text.toString() + startGp.latitude +
                        ", " + startGp.longitude

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
            //geoNominatim.setService(NOMINATIM_SERVICE_URL)
            return geoNominatim.getFromLocationName(locationName,10)
        }

        override fun onPostExecute(result: List<Address>) {
            super.onPostExecute(result)
            if (result.size > 0) {
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
                wayPoints.add(startGp)
                wayPoints.add(endGp as GeoPoint)
                //uncomment for use own server
                //roadManager.setService(ROUTE_SERVICE_URL)
                val road = roadManager.getRoad(wayPoints)
                return road
            }

            return null
        }

        override fun onPostExecute(result: Road?) {
            super.onPostExecute(result)
            if (result != null) {
                drawRoad(result)
                taxi_request?.visibility = View.VISIBLE
            }
        }
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

        map?.controller?.setCenter(startGp)
    }

}
