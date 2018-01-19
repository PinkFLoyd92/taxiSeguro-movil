package com.example.geotaxi.geotaxi.config

import com.google.android.gms.location.LocationRequest

/**
 * Created by dieropal on 17/01/18.
 */
class GeoConstant {
    companion object {
        val LOCATION_REQUEST_INTERVAL: Long = 5000
        val LR_FASTEST_INTERVAL: Long = 5000
        val LR_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY
        val MY_PERMISSIONS_REQUEST_LOCATION = 1
        val REQUEST_CHECK_SETTINGS = 1
    }
}