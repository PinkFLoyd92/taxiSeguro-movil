package com.example.geotaxi.taxiseguroconductor.map

import android.location.Location
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.view.View
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.taxiseguroconductor.data.Route
import com.example.geotaxi.taxiseguroconductor.data.User
import com.example.geotaxi.taxiseguroconductor.ui.MainActivity
import org.json.JSONObject
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.BoundingBox
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
    var alternativeRoutes: ArrayList<out Road> = arrayListOf()
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

    fun drawRoads(roads: ArrayList<out Road>) {
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
                    roadColor = ROAD_COLORS["chosen"]
                    mInfoWin.setTittle("Optima")
                    mInfoWin.setTittleColor(ROAD_COLORS["chosen"]!!)
                    mInfoWin.showTittle()
                    //mInfoWin.hideTittle()
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

    fun drawRoadOverlay(roadOverlay: Polyline, duration: Double) {

        roadOverlay.width = 6F
        val midIndex = if (roadOverlay.points.size%2 == 0) {
            (roadOverlay.points.size/2) - 1
        } else {
            ((roadOverlay.points.size + 1)/2) - 1
        }
        val infoPos = roadOverlay.points[midIndex]
        val durationStr = ("%.2f".format(duration/60)) + " min"
        val mInfoWin = MyInfoWindow(R.layout.info_window, map!!,
                title = durationStr, description = "")
        roadOverlay.color = ROAD_COLORS["chosen"]!!
        roadOverlay.infoWindow = mInfoWin
        map?.overlays?.add(roadOverlay)
        addDestMarker()
        roadOverlay.showInfoWindow(infoPos)
        val mBoundingBox = BoundingBox.fromGeoPoints(roadOverlay.points)
        map?.zoomToBoundingBox(mBoundingBox, true)
        map?.invalidate()
        roadOverlays.add(roadOverlay)

    }

    private fun onRoadChosen(roadOverlay: Polyline, road: Road) {
        var indx = 0
        activity?.choose_route?.isEnabled = true
        activity?.choose_route?.setTextColor(
                ResourcesCompat.getColor(activity?.resources!!, R.color.colorAccent, null)
        )
        activity?.choose_route?.isEnabled = true
        roadOverlays.forEach {
            if (it != roadOverlay) {
                it.color = ROAD_COLORS["alternative"]!!
            } else {
                roadChosenIndex = indx
            }
            indx+= 1
        }
        roadOverlay.color = ROAD_COLORS["chosen"]!!
        map!!.overlays.add(roadOverlay)
        map!!.overlays.remove(roadOverlay)
        roadChosen = road
    }

    fun getRoadChosen(): Road? {
        return roadChosen
    }

    fun getRoadIndexChosen(): Int {
        return roadChosenIndex
    }

    fun initRouteAtLaunch(activity: MainActivity, obj: JSONObject) {
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

            val roads = activity.roadHandler.executeRoadTask(Route.instance.start!!, Route.instance.end!!)
            if (roads != null) {
                destPosition = GeoPoint(endLoc)
                drawRoad(roads[Route.instance.currentRoadIndex], Route.instance.start!!)
                activity?.fabRoutes?.visibility = View.VISIBLE
                val points = RoadManager.buildRoadOverlay(roads[Route.instance.currentRoadIndex]).points
                Route.instance.currentRoad = roads[Route.instance.currentRoadIndex]
                Route.instance.waypoints = points as ArrayList<GeoPoint>
                Route.instance.roads = roads
            }
        }catch (e : Exception) {
            e.printStackTrace()
        }
    }

    fun clearMapOverlays() {
        map?.overlays?.clear()
        InfoWindow.closeAllInfoWindowsOn(map)
    }
    
}