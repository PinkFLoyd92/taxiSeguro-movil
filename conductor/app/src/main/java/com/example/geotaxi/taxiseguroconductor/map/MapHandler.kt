package com.example.geotaxi.taxiseguroconductor.map

import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.AsyncTask
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.view.View
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.taxiseguroconductor.config.Env
import com.example.geotaxi.taxiseguroconductor.data.Route
import com.example.geotaxi.taxiseguroconductor.data.User
import com.example.geotaxi.taxiseguroconductor.ui.MainActivity
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Created by sebas on 1/14/18.
 */

class MapHandler {
    private var activity: MainActivity? = null
    private var map : MapView? = null
    var driverMarker: Marker? = null
    var clientMarker: Marker? = null
    var destinationMarker: Marker? = null
    var mapController: IMapController? = null
    var mCurrentLocation: GeoPoint? = null
    var destPosition: GeoPoint? = null
    var alternativeRoutes: Array<out Road> = arrayOf()
    private var roadOverlays = mutableListOf<Polyline>()
    private var roadChosen : Road? = null
    private var roadChosenIndex: Int = 0
    private var ROAD_COLORS: HashMap<String, Int> = hashMapOf()

    constructor(activity: MainActivity,
                mapView: MapView?,
                driverMarker: Marker?,
                clientMarker: Marker?,
                destinationMarker: Marker?,
                mapController: IMapController?,
                mCurrentLocation: GeoPoint?
                ) {
        this.map = mapView
        this.driverMarker = driverMarker
        this.clientMarker = clientMarker
        this.destinationMarker = destinationMarker
        this.mapController = mapController
        this.mCurrentLocation = mCurrentLocation
        this.activity = activity
        this.ROAD_COLORS = hashMapOf(
                "chosen" to ResourcesCompat.getColor(activity.resources, R.color.chosenRoute, null),
                "alternative" to ResourcesCompat.getColor(activity.resources, R.color.alternativeRoute, null),
                "best" to ResourcesCompat.getColor(activity.resources, R.color.bestRoute, null)
        )
    }

    fun updateClientIconOnMap(location: GeoPoint) {
        activity?.runOnUiThread(object: Runnable {
            override fun run() {
                map?.overlays?.remove(clientMarker)
                clientMarker?.position = location
                clientMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map?.overlays?.add(clientMarker)
                map?.invalidate()
            }
        })
    }

    fun updateDriverIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(driverMarker)
        driverMarker?.position = location
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map?.overlays?.add(driverMarker)
        map?.invalidate()
    }

    fun addDestMarker() {
        destinationMarker?.position = destPosition
        destinationMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(destinationMarker)
    }

    fun animateToLocation(location: GeoPoint?, zoomLevel: Int) {
        if (location != null) {
            mapController?.animateTo(location)
            mapController?.zoomTo(17)
        }
    }

    fun executeRoadTask(activity: Activity, start: GeoPoint, end: GeoPoint){
        val roadTask = RoadTask(start, end)
        roadTask.execute(activity)
    }

    inner class RoadTask : AsyncTask<Context, Void, Array<out Road>?> {
        var start:GeoPoint? = null
        var end:GeoPoint? = null
        constructor(start: GeoPoint, end: GeoPoint) : super() {
            this.start = start
            this.end = end
        }

        override fun doInBackground(vararg params: Context?): Array<out Road>? {
            val roadManager = OSRMRoadManager(params[0])
            val wayPoints = ArrayList<GeoPoint>(0)
            if (this.end != null && this.start != null) {
                wayPoints.add(this.start as GeoPoint)
                wayPoints.add(this.end as GeoPoint)

                //uncomment for use own server
                roadManager.setService(Env.OSRM_SERVER_URL)

                return roadManager.getRoads(wayPoints)
            }
            return null
        }

        override fun onPostExecute(result: Array<out Road>?) {
            super.onPostExecute(result)
            if (result != null && result.isNotEmpty()
                    && result[0].mStatus == Road.STATUS_OK) {
                // currentRoad = result
                destPosition = end
                drawRoad(result[Route.instance.currentRoadIndex],start = this.start as GeoPoint)
                activity?.fabRoutes?.visibility = View.VISIBLE
                Route.instance.currentRoad = result[Route.instance.currentRoadIndex]
                Route.instance.start = this.start
                Route.instance.end = this.end
                Route.instance.roads = result
            }
        }
    }

    fun getAlternativeRoutes(start: GeoPoint, end: GeoPoint): Array<out Road>?{
        val roads = AlternativeRoutesTask(start, end)
        return roads.execute().get()
    }

    inner class AlternativeRoutesTask : AsyncTask<Context, Void, Array<out Road>?> {
        var start:GeoPoint? = null
        var end:GeoPoint? = null
        constructor(start: GeoPoint, end: GeoPoint) : super() {
            this.start = start
            this.end = end
        }
        override fun doInBackground(vararg params: Context?): Array<out Road>? {
            val roadManager = OSRMRoadManager(activity)
            val wayPoints = ArrayList<GeoPoint>(0)
            wayPoints.add(this.start as GeoPoint)
            wayPoints.add(this.end as GeoPoint)
            //uncomment for use own server
            roadManager.setService(Env.OSRM_SERVER_URL)
            return roadManager.getRoads(wayPoints)
        }
    }

    fun drawRoad(road: Road, start:GeoPoint) {
        var roadOverlay = RoadManager.buildRoadOverlay(road)
        roadOverlay.color = ROAD_COLORS["chosen"]!!
        map?.overlays?.add(roadOverlay)
        updateClientIconOnMap(User.instance.position!!)
        addDestMarker()
        val midIndex = if (road.mNodes.size%2 == 0) {
            (road.mNodes.size/2) - 1
        } else {
            ((road.mNodes.size + 1)/2) - 1
        }
        val infoPos = road.mNodes[midIndex].mLocation
        val duration = ("%.2f".format(road.mDuration/60)) + " min"
        val distance = ("%.2f".format(road.mLength)) + " km"
        roadOverlay.infoWindow = MyInfoWindow(R.layout.info_window, map!!,
                title = duration, description = distance)
        roadOverlay.infoWindow.view.setOnLongClickListener { v: View ->
            roadOverlay.infoWindow.close()
            true
        }
        roadOverlay.setOnClickListener{ polyline, mapView, eventPos ->
            roadOverlay.showInfoWindow(eventPos)
            true
        }
        roadOverlay.showInfoWindow(infoPos)
        map?.overlays?.add(destinationMarker)
        map?.zoomToBoundingBox(road.mBoundingBox, true)
        map?.invalidate()
    }

    fun drawRoads(roads: kotlin.Array<out Road>) {
        var roadIndex = 0
        var roadColor = ROAD_COLORS["chosen"]
        if (roadOverlays.isNotEmpty()) {
            roadOverlays.clear()
        }
        roads.forEach { road ->

            if (road.mNodes.isNotEmpty()) {

                val roadOverlay = RoadManager.buildRoadOverlay(road)
                roadOverlay.width = 6F

                val midIndex = if (road.mNodes.size%2 == 0) {
                    (road.mNodes.size/2) - 1
                } else {
                    ((road.mNodes.size + 1)/2) - 1
                }
                val infoPos = road.mNodes[midIndex].mLocation
                val duration = ("%.2f".format(road.mDuration/60)) + " min"
                val distance = ("%.2f".format(road.mLength)) + " km"

                val mInfoWin = MyInfoWindow(R.layout.info_window, map!!,
                        title = duration, description = distance)

                if (roadIndex != 0) {
                    roadColor = ROAD_COLORS["alternative"]
                    mInfoWin.setTittle("Alterna")
                    mInfoWin.showTittle()
                    map?.overlays?.add(roadOverlay)
                } else {
                    roadColor = ROAD_COLORS["best"]
                    mInfoWin.hideTittle()
                }
                roadOverlay.infoWindow = mInfoWin
                roadOverlay.infoWindow.view.setOnClickListener{ v: View  ->
                    onRoadChosen(roadOverlay, road)
                }
                roadOverlay.infoWindow.view.setOnLongClickListener { v: View ->
                    roadOverlay.infoWindow.close()
                    true
                }
                roadOverlay.setOnClickListener{ polyline, mapView, eventPos ->
                    onRoadChosen(roadOverlay, road)
                    roadOverlay.showInfoWindow(eventPos)
                    true
                }
                roadOverlays.add(roadOverlay)

                roadOverlay.color = roadColor!!
                roadOverlay.showInfoWindow(infoPos)
                map?.zoomToBoundingBox(road.mBoundingBox, true)
                map?.invalidate()
            }
            roadIndex += 1
        }
        map?.overlays?.add(roadOverlays[0])
    }

    private fun onRoadChosen(roadOverlay: Polyline, road: Road) {
        var indx = 0
        activity?.choose_route?.isEnabled = true
        roadOverlays.forEach {
            if (it != roadOverlay) {
                it.color = ROAD_COLORS["alternative"]!!
            } else {
                roadChosenIndex = indx
            }
            indx+= 1
        }
        map!!.overlays.remove(roadOverlay)
        roadOverlay.color = ROAD_COLORS["chosen"]!!
        map!!.overlays.add(roadOverlay)
        roadChosen = road
    }

    fun getRoadChosen(): Road? {
        return roadChosen
    }

    fun getRoadIndexChosen(): Int {
        return roadChosenIndex
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
        InfoWindow.closeAllInfoWindowsOn(map)
    }
    
}