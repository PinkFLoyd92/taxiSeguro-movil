package com.example.geotaxi.geotaxi.config

import android.os.Build
import android.util.Log

class Env {
    companion object {
        fun IP() : String {
            val buildDetails: String = (Build.FINGERPRINT + Build.DEVICE + Build.MODEL + Build.BRAND + Build.PRODUCT + Build.MANUFACTURER + Build.HARDWARE).toLowerCase();
            Log.d("INFO_PHONE", buildDetails);
            if (android.os.Build.MODEL.contains("google_sdk") ||
                    android.os.Build.MODEL.contains("Emulator") || buildDetails.contains("generic")
                    ||  buildDetails.contains("unknown")
                    ||  buildDetails.contains("emulator")
                    ||  buildDetails.contains("sdk")
                    ||  buildDetails.contains("genymo")
                    ||  buildDetails.contains("x86") // this includes vbox86
                    ||  buildDetails.contains("goldfish")
                    ||  buildDetails.contains("test-keys")) {
                return "http://10.0.2.2"
            }
            return "http://192.168.0.30"
        }
        val OSRM_SERVER_URL = IP() + ":5000/route/v1/car/"
        val NOMINATIM_SERVER_URL = IP() + ":80/nominatim/"
        val API_BASE_URL = IP() + ":4000/v1/"
        val SOCKET_SERVER_URL = IP() + ":9000/"
        val OSRM_ROUTES = IP() + ":5000/"
    }
}