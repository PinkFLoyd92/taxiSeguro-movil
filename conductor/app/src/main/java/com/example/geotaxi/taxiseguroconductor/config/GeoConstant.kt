package com.example.geotaxi.taxiseguroconductor.config

import com.google.android.gms.location.LocationRequest

/**
 * Created by sebas on 1/14/18.
 */

class GeoConstant {

    companion object {
        val MIN_TIME: Long = 5000
        val INTERVAL: Long = 5000
        val FASTEST_INTERVAL: Long = 2000
        val LOCATION_REQUEST_INTERVAL: Long = 1000
        val LR_FASTEST_INTERVAL: Long = 1000
        val LR_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY
        val MY_PERMISSIONS_REQUEST_LOCATION = 1
        val REQUEST_CHECK_SETTINGS = 1
    }
}