package com.example.geotaxi.geotaxi.map

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.CardView
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.ui.MainActivity
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Created by dieropal on 17/01/18.
 */
class MapHandler {
    private var activity: MainActivity? = null
    private var map : MapView? = null
    private var driverMarker: Marker? = null
    private var userMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var mapController: IMapController? = null
    private var roadOverlays = mutableListOf<Polyline>()
    private var roadChosen : Road? = null
    private var roadChosenIndex: Int = 0
    private var ROAD_COLORS: HashMap<String, Int> = hashMapOf()
    private var waypointMapMarkers: ArrayList<Marker> = arrayListOf()
    var waypointIcon: Drawable
    var destinationIcon: Drawable
    var onMapEventsOverlay: Boolean = true
    var overlaysEvents: MapEventsOverlay? = null

    constructor(activity: MainActivity,
                mapView: MapView?,
                driverIcon: Drawable?,
                userIcon: Drawable?,
                destinationIcon: Drawable?
    ) {
        val mRotationGestureOverlay =  RotationGestureOverlay(mapView)
        val userMarker = Marker(mapView)
        val driverMarker = Marker(mapView)
        val destinationMarker = Marker(mapView)
        val mapController = mapView?.controller

        mRotationGestureOverlay.isEnabled = true
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setMultiTouchControls(true)
        mapView?.overlays?.add(mRotationGestureOverlay)
        mapController?.setCenter(User.instance.position)
        mapController?.setZoom(17)
        userMarker.setIcon(userIcon)
        driverMarker.setIcon(driverIcon)
        destinationMarker.setIcon(destinationIcon)
        this.activity = activity
        this.map = mapView
        this.mapController = mapController
        this.driverMarker = driverMarker
        this.userMarker = userMarker
        this.destinationMarker = destinationMarker
        this.destinationIcon = destinationIcon!!
        this.waypointIcon = destinationIcon.constantState.newDrawable().mutate()
        this.waypointIcon.setColorFilter(ResourcesCompat.getColor(activity.resources, R.color.green, null),
                PorterDuff.Mode.SRC_IN)
        this.ROAD_COLORS = hashMapOf(
                "chosen" to ResourcesCompat.getColor(activity.resources, R.color.chosenRoute, null),
                "alternative" to ResourcesCompat.getColor(activity.resources, R.color.alternativeRoute, null)
                )
    }

    fun initRoadMapEventsOverlay() {
        destinationMarker?.isDraggable = true
        destinationMarker?.dragOffset = 8F
        destinationMarker?.setOnMarkerDragListener(object: Marker.OnMarkerDragListener{
            lateinit var startMarkerPosition: GeoPoint
            override fun onMarkerDragStart(marker: Marker?) {
                startMarkerPosition = marker!!.position
            }

            override fun onMarkerDrag(marker: Marker?) {

            }

            override fun onMarkerDragEnd(marker: Marker?) {
                if (onMapEventsOverlay) {
                    val endPos = marker?.position
                    clearMapOverlays()
                    val waypoints = arrayListOf<GeoPoint>(User.instance.position!!, endPos!!)
                    val roads = activity!!.roadHandler.executeRoadTask(waypoints)
                    if (roads != null && roads.isNotEmpty()
                            && roads[0].mStatus == Road.STATUS_OK) {
                        val points = RoadManager.buildRoadOverlay(roads[0]).points as ArrayList<GeoPoint>
                        Route.instance.roadPoints = points
                        Route.instance.currentRoad = roads[0]
                        Route.instance.roads = roads
                        Route.instance.end = marker.position
                        Route.instance.waypoints = waypoints
                        drawRoad(roads[0], User.instance.position!!, marker.position)
                        addDestMarker(marker.position)
                        activity!!.fabRoutes?.visibility = View.VISIBLE
                        activity!!.taxi_request?.visibility = View.VISIBLE
                    }
                } else {
                    marker?.position = startMarkerPosition
                }
            }
        })

        val mapEventsReceiver = object: MapEventsReceiver {
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (onMapEventsOverlay) {
                    clearMapOverlays()
                    map?.overlays?.add(destinationMarker)
                    val waypoints = arrayListOf<GeoPoint>(User.instance.position!!, p!!)
                    val roads = activity!!.roadHandler.executeRoadTask(waypoints)
                    if (roads != null && roads.isNotEmpty()
                            && roads[0].mStatus == Road.STATUS_OK) {
                        val points = RoadManager.buildRoadOverlay(roads[0]).points as ArrayList<GeoPoint>
                        if (points == null) {
                            Log.d("null", "POINTS NULL")
                        }
                        Route.instance.roadPoints = points
                        Route.instance.currentRoad = roads[0]
                        Route.instance.roads = roads
                        Route.instance.end = p
                        Route.instance.waypoints = waypoints
                        drawRoad(roads[0], User.instance.position!!, p)
                        addDestMarker(p)
                        activity!!.fabRoutes?.visibility = View.VISIBLE
                        activity!!.taxi_request?.visibility = View.VISIBLE
                        activity!!.findViewById<CardView>(R.id.search_address_info)
                                .visibility = View.GONE
                        activity!!.findViewById<CardView>(R.id.dest_marker_info)
                                .visibility = View.VISIBLE
                    }
                }
                return false
            }
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        overlaysEvents = MapEventsOverlay(mapEventsReceiver)
        map?.overlays?.add(overlaysEvents)
    }

    fun initCustomRoadMapEventsOverlay() {
        val mapEventsReceiver = object: MapEventsReceiver {
            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (onMapEventsOverlay) {
                    val waypoint = Marker(map)
                    waypoint.setIcon(destinationIcon)
                    if (waypointMapMarkers.isNotEmpty()) {
                        changePreviousIconsToWaypoint()
                    }
                    waypoint.position = p
                    waypoint.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    waypoint.isDraggable = true
                    waypoint.dragOffset = 5F
                    waypoint.setOnMarkerDragListener(CustomRouteDragListener())
                    waypointMapMarkers.add(waypoint)
                    waypoint.infoWindow = object:InfoWindow(R.layout.transparent_view, map){
                        override fun onOpen(item: Any?) {
                        }

                        override fun onClose() {
                        }
                    }
                    Route.instance.waypoints.add(p!!)
                    val roads = activity!!.roadHandler.executeRoadTask(Route.instance.waypoints)
                    if (roads != null && roads.isNotEmpty()
                            && roads[0].mStatus == Road.STATUS_OK) {
                        val points = RoadManager.buildRoadOverlay(roads[0]).points as ArrayList<GeoPoint>
                        Route.instance.roadPoints = points
                        Route.instance.currentRoad = roads[0]
                        Route.instance.roads = roads
                        Route.instance.end = p
                        clearMapOverlays()
                        drawRoad(roads[0], User.instance.position!!, p)
                        addwaypointMarkers()
                        activity!!.findViewById<Button>(R.id.ok_customRoute_action)
                                .isEnabled = true
                        activity!!.findViewById<Button>(R.id.ok_customRoute_action)
                                .setTextColor(ResourcesCompat.getColor(activity!!.resources, R.color.colorAccent, null))
                    }
                }
                return false
            }
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        overlaysEvents = MapEventsOverlay(mapEventsReceiver)
        map?.overlays?.add(overlaysEvents)
    }

    private fun changePreviousIconsToWaypoint() {
        for (wm in waypointMapMarkers) {
            wm.setIcon(waypointIcon)
        }
    }

    private fun addwaypointMarkers() {
        if (waypointMapMarkers.isNotEmpty()) {
            for (wm in waypointMapMarkers) {
                map?.overlays?.add(wm)
            }
        }
    }

    fun clearWaypointMarkers() {
        waypointMapMarkers.clear()
    }

    private inner class CustomRouteDragListener : Marker.OnMarkerDragListener {
        var draggedMarkerIndex = 0
        var draggedMapMarkerIndex = 0
        var removeArea = activity!!.findViewById<View>(R.id.fab_remove_area)
        override fun onMarkerDragStart(marker: Marker?) {
            if (onMapEventsOverlay) {
                removeArea.visibility = View.VISIBLE
                activity!!.findViewById<View>(R.id.fab_mlocation)
                        .visibility = View.GONE
                activity!!.findViewById<View>(R.id.customizing_route_actions)
                        .visibility = View.GONE
                marker?.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker?.showInfoWindow()
                draggedMapMarkerIndex = waypointMapMarkers.indexOf(marker)
                if (Route.instance.waypoints.size > 1) {
                    draggedMarkerIndex = Route.instance.waypoints.indexOf(marker!!.position)
                    Route.instance.waypoints.remove(marker!!.position)
                }
            }
        }

        override fun onMarkerDrag(marker: Marker?) {
            if (onMapEventsOverlay) {
                marker?.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                marker?.showInfoWindow()
            }
        }

        override fun onMarkerDragEnd(marker: Marker?) {
            if (onMapEventsOverlay) {
                if (isInsideViewArea(marker!!.infoWindow.view, removeArea)){
                    clearMapOverlays()
                    if (draggedMapMarkerIndex == waypointMapMarkers.size-1 &&
                            draggedMapMarkerIndex > 0) {
                     waypointMapMarkers[draggedMapMarkerIndex-1].setIcon(destinationIcon)
                    }
                    waypointMapMarkers.remove(marker)
                    if (waypointMapMarkers.isEmpty()) {
                        val okCustomRoute = activity!!.findViewById<Button>(R.id.ok_customRoute_action)
                        okCustomRoute.isEnabled = false
                        okCustomRoute.setTextColor(ResourcesCompat.getColor(activity!!.resources, R.color.gray, null))
                    } else {
                        Route.instance.end = waypointMapMarkers.last().position
                    }
                } else {
                    Route.instance.waypoints.add(draggedMarkerIndex, marker.position!!)
                }
                if (waypointMapMarkers.isNotEmpty()) {
                    val roads = activity!!.roadHandler.executeRoadTask(Route.instance.waypoints)
                    if (roads != null && roads.isNotEmpty()
                            && roads[0].mStatus == Road.STATUS_OK) {
                        val points = RoadManager.buildRoadOverlay(roads[0]).points as ArrayList<GeoPoint>
                        Route.instance.roadPoints = points
                        Route.instance.currentRoad = roads[0]
                        Route.instance.roads = roads
                        if (draggedMapMarkerIndex == waypointMapMarkers.size-1) {
                            Route.instance.end = marker.position
                        }
                        clearMapOverlays()
                        drawRoad(roads[0], User.instance.position!!, marker.position)
                        addwaypointMarkers()
                    }
                }
                removeArea.visibility = View.GONE
                activity!!.findViewById<View>(R.id.fab_mlocation)
                        .visibility = View.VISIBLE
                activity!!.findViewById<View>(R.id.customizing_route_actions)
                        .visibility = View.VISIBLE
            } else {
                marker?.position = Route.instance.end
            }
        }
    }

    fun isInsideViewArea(smallV: View, bigV: View): Boolean {
        var sLocation = intArrayOf(0,0)
        var bLocation = intArrayOf(0,0)
        val sWidth = smallV.width
        val sHeight = smallV.height
        val bWidth = bigV.width
        val bHeight = bigV.height

        smallV.getLocationOnScreen(sLocation)
        bigV.getLocationOnScreen(bLocation)

        val sxCenter = sLocation[0] + sWidth/2
        val syCenter = sLocation[1] + sHeight/2

        if (sxCenter >= bLocation[0] && sxCenter <= (bLocation[0] + bWidth) &&
            syCenter >= bLocation[1] && syCenter <= (bLocation[1] + bHeight)) {
            return true
        }
        return false
    }

    fun updateUserIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(userMarker)
        userMarker?.position = location
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(userMarker)
        map?.invalidate()
    }

    fun updateDriverIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(driverMarker)
        driverMarker?.position = location
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map?.overlays?.add(driverMarker)
        map?.invalidate()
    }

    fun animateToLocation(location: GeoPoint?, zoomLevel: Int) {
        if (location != null) {
            mapController?.animateTo(location)
            mapController?.zoomTo(17)
        }
    }

    fun drawRoad(road: Road, userPos: GeoPoint, destinationPos: GeoPoint) {
        if (roadOverlays.isNotEmpty()) {
            roadOverlays.clear()
        }
        if (!road.mNodes.isEmpty()) {
            val roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay.color = ROAD_COLORS["chosen"]!!
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
            updateUserIconOnMap(userPos)
            //addDestMarker(destinationPos)
            map?.overlays?.add(roadOverlay)
            roadOverlay.showInfoWindow(infoPos)
            map?.zoomToBoundingBox(road.mBoundingBox, true)
            map?.invalidate()
            roadOverlays.add(roadOverlay)
        }
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

                if (roadIndex != Route.instance.currentRoadIndex) {
                    roadColor = ROAD_COLORS["alternative"]
                    mInfoWin.setTittle("Alterna")
                    mInfoWin.showTittle()
                    map?.overlays?.add(roadOverlay)
                } else {
                    roadColor = ROAD_COLORS["chosen"]
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
                roadOverlay.showInfoWindow(infoPos)//open(roadOverlay, infoPos, 2,2)
                map?.zoomToBoundingBox(road.mBoundingBox, true)
                map?.invalidate()
            }
            roadIndex += 1
        }
        map?.overlays?.add(roadOverlays[Route.instance.currentRoadIndex])
    }

    fun drawRoadOverlay(roadOverlay: Polyline, duration: Double, driverRequest: Boolean = false) {

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
        if (driverRequest) {
            mInfoWin.setTittle("Alterna")
            mInfoWin.showTittle()
            roadOverlay.color = ROAD_COLORS["alternative"]!!
        } else {
            roadOverlay.color = ROAD_COLORS["chosen"]!!
        }
        roadOverlay.infoWindow = mInfoWin
        map?.overlays?.add(roadOverlay)
        addDestMarker(Route.instance.end!!)
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
    fun addDestMarker(destPos: GeoPoint) {
        destinationMarker?.position = destPos
        destinationMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(destinationMarker)
    }

    fun resetToRoadMapOverlays() {
        map?.overlays?.clear()
        InfoWindow.closeAllInfoWindowsOn(map)
        initRoadMapEventsOverlay()
    }

    fun resetToCustomRoadMapOverlays() {
        map?.overlays?.clear()
        InfoWindow.closeAllInfoWindowsOn(map)
        initCustomRoadMapEventsOverlay()
    }

    fun clearMapOverlays() {
        removeMapOverlay(destinationMarker!!)
        removeMapOverlay(driverMarker!!)
        if (roadOverlays.isNotEmpty()) {
            roadOverlays.forEach { roadOverlay ->
                removeMapOverlay(roadOverlay)
            }
        }
        if (waypointMapMarkers.isNotEmpty()) {
            for (wm in waypointMapMarkers) {
                removeMapOverlay(wm)
            }
        }
        InfoWindow.closeAllInfoWindowsOn(map)
    }

    fun removeMapOverlay(overlay: Overlay) {
        map?.overlays?.remove(overlay)
    }

}