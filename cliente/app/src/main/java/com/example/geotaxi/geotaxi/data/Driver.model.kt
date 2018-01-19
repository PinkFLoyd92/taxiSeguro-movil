package com.example.geotaxi.geotaxi.data

import org.osmdroid.util.GeoPoint

/**
 * Created by dieropal on 18/01/18.
 */
class Driver
private constructor() {
    var _id = ""
    var username = ""
    var role = ""
    var name = ""
    var cedula = ""
    var mobile = ""
    var vehicle_plate = ""
    var vehicle_description = ""
    var position : GeoPoint? = null

    companion object {
        val instance = Driver()
    }
}