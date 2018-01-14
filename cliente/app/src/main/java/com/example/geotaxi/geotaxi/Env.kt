package com.example.geotaxi.geotaxi

import com.google.android.gms.location.LocationRequest

class Env {
    companion object {
        const val IP = "http://192.168.0.111"
        const val OSRM_SERVER_URL = IP+":5000/route/v1/car/"
        const val NOMINATIM_SERVER_URL = IP+":80/nominatim/"
        const val API_BASE_URL = IP+":4000/v1/"
        const val API_SOCKET_URL = IP+":9000"
        val LOCATION_REQUEST_INTERVAL: Long = 1000
        val LR_FASTEST_INTERVAL: Long = 1000
        val LR_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY
        val MY_PERMISSIONS_REQUEST_LOCATION = 1
        val REQUEST_CHECK_SETTINGS = 1
    }
}