package com.example.geotaxi.geotaxi.API.endpoints

import android.content.Context
import android.os.AsyncTask
import com.example.geotaxi.geotaxi.config.Env
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint

/**
 * Created by dieropal on 17/01/18.
 */
class OSRMRoadAPI(val context: Context) {

    fun getRoad(startGp: GeoPoint, endGp: GeoPoint): Array<out Road> {
        val roadTask = RoadTask(startGp, endGp)
        return roadTask.execute().get()
    }

    //Class that use OSRM server in async mode for get a Road, from start to destination point
    inner class RoadTask(val startGp: GeoPoint, val endGp: GeoPoint) : AsyncTask<Context, Array<out Road>, Array<out Road>>() {

        override fun doInBackground(vararg params: Context?): Array<out Road> {
            val roadManager = OSRMRoadManager(context)
            val wayPoints = ArrayList<GeoPoint>(0)
            wayPoints.add(startGp)
            wayPoints.add(endGp)
            //uncomment for use own server
            roadManager.setService(Env.OSRM_SERVER_URL)
            return roadManager.getRoads(wayPoints)
        }
    }
}