package com.example.geotaxi.geotaxi.data

import org.osmdroid.util.GeoPoint

/**
 * Created by dieropal on 17/01/18.
 */
class User
private constructor() {
    var _id = ""
    var username = ""
    var role = ""
    var position : GeoPoint? = null

    companion object {
        val instance = User()
    }
}