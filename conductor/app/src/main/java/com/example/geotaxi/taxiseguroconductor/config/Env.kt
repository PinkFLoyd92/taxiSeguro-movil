package com.example.geotaxi.taxiseguroconductor.config

import android.os.Build
import android.provider.Settings.System.getString
import android.util.Log
import com.google.android.gms.location.LocationRequest

/**
 * Created by sebas on 1/13/18.
 */

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
                return "http://10.0.3.2"
            }
            return "http://192.168.100.34"
        }
        val OSRM_SERVER_URL = IP() + ":5000/route/v1/car/"
        val NOMINATIM_SERVER_URL = IP()  + ":80/nominatim/"
        val API_BASE_URL = IP()  + ":4000/v1/"
        val SOCKET_SERVER_URL = IP()  + ":9000/"
        val LR_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY
        val MY_PERMISSIONS_REQUEST_LOCATION = 1
        val REQUEST_CHECK_SETTINGS = 1
    }
}
