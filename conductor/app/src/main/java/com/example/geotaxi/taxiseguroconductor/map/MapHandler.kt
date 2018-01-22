package com.example.geotaxi.taxiseguroconductor.map

import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.AsyncTask
import android.util.Log
import com.example.geotaxi.taxiseguroconductor.config.Env
import com.example.geotaxi.taxiseguroconductor.data.Route
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Created by sebas on 1/14/18.
 */

class MapHandler {
    private var map : MapView? = null
    var driverMarker: Marker? = null
    var clientMarker: Marker? = null
    var mapController: IMapController? = null
    var mCurrentLocation: GeoPoint? = null

    constructor(mapView: MapView?,
                driverMarker: Marker?,
                clientMarker: Marker?,
                mapController: IMapController?,
                mCurrentLocation: GeoPoint?
                ) {
        this.map = mapView
        this.driverMarker = driverMarker
        this.clientMarker = clientMarker
        this.mapController = mapController
        this.mCurrentLocation = mCurrentLocation
    }

    public fun updateClientIconOnMap(location: GeoPoint, activity: Activity) {
        activity.runOnUiThread(object: Runnable {
            override fun run() {
                map?.overlays?.remove(clientMarker)
                clientMarker?.position = location
                clientMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map?.overlays?.add(clientMarker)
                // mapController?.animateTo(location)
                map?.invalidate()
            }
        })
    }

    public fun updateDriverIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(driverMarker)
        driverMarker?.position = location
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map?.overlays?.add(driverMarker)
        this.mapController?.animateTo(location)
        map?.invalidate()
    }

    public fun executeRoadTask(activity: Activity, start: GeoPoint, end: GeoPoint){
        val roadTask = RoadTask(start, end)
        roadTask.execute(activity)
    }

    inner class RoadTask : AsyncTask<Context, Void, Road> {
        var start:GeoPoint? = null
        var end:GeoPoint? = null
        constructor(start: GeoPoint, end: GeoPoint) : super() {
            this.start = start
            this.end = end
        }

        override fun doInBackground(vararg params: Context?): Road? {
            val roadManager = OSRMRoadManager(params[0])
            val wayPoints = ArrayList<GeoPoint>(0)
            if (this.end != null && this.start != null) {
                wayPoints.add(this.start as GeoPoint)
                wayPoints.add(this.end as GeoPoint)

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
                // currentRoad = result
                drawRoad(result,start = this.start as GeoPoint, end = this.end as GeoPoint)
                Route.instance.routeObj = result
                Route.instance.start = this.start
                Route.instance.end = this.end
            }
        }
    }

    private fun drawRoad(road: Road, start:GeoPoint, end:GeoPoint) {
        var roadOverlay = RoadManager.buildRoadOverlay(road)
        map?.overlays?.add(roadOverlay)
        // updateUserIconOnMap()
        val endMarker = Marker(map)
        //val markerIcon = ResourcesCompat.getDrawable(resources, R.drawable.location_marker, null)
        endMarker.position = end
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        Log.d("DRAWING ROAD...", "ROAD")
        // endMarker.setIcon(markerIcon)
        endMarker.snippet = ("%.2f".format(road.mDuration/60)) + " min"
        endMarker.subDescription = ("%.2f".format(road.mLength)) + " km"
        map?.overlays?.add(endMarker)
        endMarker.showInfoWindow()
        map?.zoomToBoundingBox(road.mBoundingBox, true)
        map?.invalidate()
    }

    fun initRouteAtLaunch(activity: Activity, obj: JSONObject) {
        val startLoc: Location? = Location("")
        val endLoc: Location? = Location("")
        try {
            Log.d("OBJECT", obj.toString())
            Route.instance._id = obj.getString("_id")
            Route.instance.client = obj.getJSONObject("client").getString("_id")
            Route.instance.driver = obj.getJSONObject("driver").getString("_id")
            startLoc?.longitude = obj.getJSONObject("start").getJSONArray("coordinates").get(0) as Double
            startLoc?.latitude = obj.getJSONObject("start").getJSONArray("coordinates").get(1)  as Double
            Route.instance.start = GeoPoint(startLoc)

            endLoc?.longitude = obj.getJSONObject("end").getJSONArray("coordinates").get(0) as Double
            endLoc?.latitude = obj.getJSONObject("end").getJSONArray("coordinates").get(1)  as Double
            Route.instance.end = GeoPoint(endLoc)

            this.executeRoadTask(activity = activity, end = Route.instance.end as GeoPoint, start = Route.instance.start as GeoPoint)
        }catch (e : Exception) {
            Log.d("error", e.message)
        }
    }

    fun clearMapOverlays() {
        map?.overlays?.clear()
    }
    
}