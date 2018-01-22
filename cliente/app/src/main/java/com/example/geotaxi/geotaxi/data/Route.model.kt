package com.example.geotaxi.geotaxi.data

import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint

/**
 * Created by dieropal on 17/01/18.
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
    var road: Road? = null
    companion object {
        val instance = Route()
    }
}