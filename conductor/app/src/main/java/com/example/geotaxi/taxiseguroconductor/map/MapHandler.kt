package com.example.geotaxi.taxiseguroconductor.map

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
    var mCurrentLocation: GeoPoint? = null
    var mapController: IMapController? = null

    constructor(mapView: MapView?,
                driverMarker: Marker?,
                userMarker: Marker?,
                mCurrentLocation: GeoPoint?,
                mapController: IMapController?
                ) {
        this.map = mapView
        this.driverMarker = driverMarker
        this.userMarker = userMarker
        this.mCurrentLocation = mCurrentLocation
    }

    public fun updateUserIconOnMap() {
        map?.overlays?.remove(userMarker)
        userMarker?.position = this.mCurrentLocation
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(userMarker)
        this.mapController?.animateTo(this.mCurrentLocation)
        map?.invalidate()
    }

    public fun updateDriverIconOnMap() {
        map?.overlays?.remove(driverMarker)
        driverMarker?.position = this.mCurrentLocation
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(driverMarker)
        this.mapController?.animateTo(this.mCurrentLocation)
        map?.invalidate()
    }
}