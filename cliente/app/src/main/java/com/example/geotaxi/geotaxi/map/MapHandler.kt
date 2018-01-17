package com.example.geotaxi.geotaxi.map

import android.graphics.drawable.Drawable
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

/**
 * Created by dieropal on 17/01/18.
 */
class MapHandler {
    var map : MapView? = null
    var driverMarker: Marker? = null
    var userMarker: Marker? = null
    var destinationMarker: Marker? = null
    var mapController: IMapController? = null

    constructor(mapView: MapView?,
                driverIcon: Drawable?,
                userIcon: Drawable?,
                destinationIcon: Drawable?
    ) {
        val mRotationGestureOverlay =  RotationGestureOverlay(mapView)
        val userMarker = Marker(mapView)
        val driverMarker = Marker(mapView)
        val destinationMarker = Marker(mapView)

        mRotationGestureOverlay.isEnabled = true
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setMultiTouchControls(true)
        mapView?.overlays?.add(mRotationGestureOverlay)
        userMarker?.setIcon(userIcon)
        driverMarker?.setIcon(driverIcon)
        destinationMarker?.setIcon(destinationIcon)

        this.map = mapView
        this.mapController = mapView?.controller
        this.driverMarker = driverMarker
        this.userMarker = userMarker
        this.destinationMarker = destinationMarker
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
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(driverMarker)
        map?.invalidate()
    }

    fun drawRoad(road: Road, userPos: GeoPoint, destinationPos: GeoPoint) {
        if (!road.mNodes.isEmpty()) {
            val roadOverlay = RoadManager.buildRoadOverlay(road)
            map?.overlays?.add(roadOverlay)
            updateUserIconOnMap(userPos)
            destinationMarker?.position = destinationPos
            destinationMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            destinationMarker?.snippet = ("%.2f".format(road.mDuration/60)) + " min"
            destinationMarker?.subDescription = ("%.2f".format(road.mLength)) + " km"
            map?.overlays?.add(destinationMarker)
            destinationMarker?.showInfoWindow()
            map?.zoomToBoundingBox(road.mBoundingBox, true)
            map?.invalidate()
        }

    }
}