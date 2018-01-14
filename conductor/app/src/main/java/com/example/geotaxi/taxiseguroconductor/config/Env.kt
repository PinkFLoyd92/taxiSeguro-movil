package com.example.geotaxi.taxiseguroconductor.config

/**
 * Created by sebas on 1/13/18.
 */

class Env {
    companion object {
        fun IP() : String {
            if (android.os.Build.MODEL.contains("google_sdk") ||
                    android.os.Build.MODEL.contains("Emulator")) {
                return "http://10.0.2.2"
            }
            return "http://192.168.100.34"
        }
        val OSRM_SERVER_URL = IP() + ":5000/route/v1/car/"
        val NOMINATIM_SERVER_URL = IP()  + ":80/nominatim/"
        val API_BASE_URL = IP()  + ":4000/v1/"
    }
}
