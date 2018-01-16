package com.example.geotaxi.taxiseguroconductor.map

import android.util.Log
import org.osmdroid.api.IMapController
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Created by sebas on 1/14/18.
 */

class MapHandler {
    private var map : MapView? = null
    var driverMarker: Marker? = null
    var userMarker: Marker? = null
    var mapController: IMapController? = null

    constructor(mapView: MapView?,
                driverMarker: Marker?,
                userMarker: Marker?,
                mapController: IMapController?
                ) {
        this.map = mapView
        this.driverMarker = driverMarker
        this.userMarker = userMarker
        this.mapController = mapController
    }

    public fun updateUserIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(userMarker)
        userMarker?.position = location
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(userMarker)
        this.mapController?.animateTo(location)
        map?.invalidate()
    }

    public fun updateDriverIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(driverMarker)
        driverMarker?.position = location
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(driverMarker)
        this.mapController?.animateTo(location)
        map?.invalidate()
    }
}