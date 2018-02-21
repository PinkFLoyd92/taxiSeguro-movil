package com.example.geotaxi.geotaxi.API.endpoints

import android.content.Context
import android.os.AsyncTask
import com.example.geotaxi.geotaxi.API.OSRMAPI
import com.example.geotaxi.geotaxi.API.OsrmAPI
import org.osmdroid.util.GeoPoint
import retrofit2.Response

/**
 * Created by dieropal on 17/01/18.
 */
class OSRMRoadAPI {
    private val osrmretrofit = OSRMAPI.retrofit
    private val osrmAPI = osrmretrofit.create(OsrmAPI::class.java)

    fun getOsrmRoutes(waypoints: ArrayList<GeoPoint>): Response<String>? {
        val waypointsStr = waypointsAsString(waypoints)
        val roadTask = RoadTask(waypointsStr)
        return roadTask.execute().get()
    }

    fun waypointsAsString(waypoints: ArrayList<GeoPoint>): String {
        var waypointsStr = ""
        for (wp in waypoints) {
            val long = wp.longitude.toString()
            val lat = wp.latitude.toString()
            waypointsStr += "$long,$lat;"
        }
        return waypointsStr.trimEnd(';')
    }

    inner class RoadTask(val waypoints: String) : AsyncTask<Context, Void, Response<String>?>() {

        override fun doInBackground(vararg params: Context?): Response<String>? {
            val serverCall = osrmAPI?.getOsrmRoutes(waypoints)
            if (serverCall != null ) {
                return serverCall.execute()
            }
            return null
        }
    }
}