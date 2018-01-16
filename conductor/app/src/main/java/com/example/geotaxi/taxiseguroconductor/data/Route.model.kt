package com.example.geotaxi.taxiseguroconductor.data

import org.osmdroid.util.GeoPoint

/**
 * Created by sebas on 1/16/18.
 */
class Route
private constructor() {
    var _id = ""
    var client = "" // id client
    var driver = "" // id driver
    var status = ""
    var start : GeoPoint?= null
    var end : GeoPoint?= null
    var waypoints : ArrayList<GeoPoint>? = null

    companion object {
        val instance = Route()
    }
}
