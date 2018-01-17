package com.example.geotaxi.taxiseguroconductor.data

import org.osmdroid.util.GeoPoint

/**
 * Created by sebas on 1/14/18.
 */
class User
private constructor() {
    var _id = ""
    var username = ""
    var role = ""
    var name = ""
    var position : GeoPoint? = null

    companion object {
        val instance = User()
    }
}