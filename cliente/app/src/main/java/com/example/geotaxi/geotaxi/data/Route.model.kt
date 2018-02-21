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
    var roadPoints: ArrayList<GeoPoint>? = null
    var waypoints: ArrayList<GeoPoint> = arrayListOf()
    var currentRoad: Road? = null
    var roads: ArrayList<out Road>? = null
    var currentRoadIndex = 0
    var duration: Double = 0.0

    companion object {
        val instance = Route()
    }
}