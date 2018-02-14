package com.example.geotaxi.taxiseguroconductor.data

import org.osmdroid.util.GeoPoint

/**
 * Created by sebas on 1/17/18.
 */
class Client
private constructor() {
    var _id = ""
    var username = ""
    var name = ""
    var role = ""
    var mobile = ""
    var position : GeoPoint? = null

    companion object {
        val instance = Client()
    }
}
