package com.example.geotaxi.geotaxi.Road

import android.util.Log
import com.example.geotaxi.geotaxi.API.endpoints.OSRMRoadAPI
import com.example.geotaxi.geotaxi.data.Route
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.*
import org.osmdroid.util.BoundingBox
import org.osmdroid.bonuspack.utils.PolylineEncoder
import org.osmdroid.util.GeoPoint
import retrofit2.Call

/**
 * Created by dieropal on 07/02/18.
 */
class RoadHandler{

    private val osrmRoadAPI: OSRMRoadAPI = OSRMRoadAPI()

    fun executeRoadTask(waypoints: ArrayList<GeoPoint>): ArrayList<out Road>?{
        val response = osrmRoadAPI.getOsrmRoutes(waypoints)
        if (response!= null && response.isSuccessful) {
            return getRoads(response.body()!!)

        }
        return null
    }

    fun getRoads(jString: String): ArrayList<Road>? {
        try {
            val jObject = JSONObject(jString)
            val jCode = jObject.getString("code")
            val jRoutes = jObject.getJSONArray("routes")
            val roads = arrayListOf<Road>()
            for (i in 0 until jRoutes.length()) {
                val road = Road()
                roads.add(road)
                road.mStatus = Road.STATUS_OK
                val jRoute = jRoutes.getJSONObject(i)
                val route_geometry = jRoute.getString("geometry")
                road.mRouteHigh = PolylineEncoder.decode(route_geometry, 10, false)
                road.mBoundingBox = BoundingBox.fromGeoPoints(road.mRouteHigh)
                road.mLength = jRoute.getDouble("distance") / 1000.0
                road.mDuration = jRoute.getDouble("duration")
                //legs:
                val jLegs = jRoute.getJSONArray("legs")
                for (l in 0 until jLegs.length()) {
                    //leg:
                    val jLeg = jLegs.getJSONObject(l)
                    val leg = RoadLeg()
                    road.mLegs.add(leg)
                    leg.mLength = jLeg.getDouble("distance")
                    leg.mDuration = jLeg.getDouble("duration")
                    //steps:
                    val jSteps = jLeg.getJSONArray("steps")
                    var lastNode: RoadNode? = null
                    var lastRoadName = ""
                    for (s in 0 until jSteps.length()) {
                        val jStep = jSteps.getJSONObject(s)
                        val jStepManeuver = jStep.getJSONObject("maneuver")
                        val jIntersections = jStep.getJSONArray("intersections")
                        var direction = jStepManeuver.getString("type")
                        if (direction == "turn" || direction == "ramp" || direction == "merge") {
                            val modifier = jStepManeuver.getString("modifier")
                            direction = direction + '-'.toString() + modifier
                        } else if (direction == "roundabout") {
                            val exit = jStepManeuver.getInt("exit")
                            direction = direction + '-'.toString() + exit
                        } else if (direction == "rotary") {
                            val exit = jStepManeuver.getInt("exit")
                            direction = "roundabout" + '-'.toString() + exit //convert rotary in roundabout...
                        }
                        val roadName = jStep.optString("name", "")
                        for (j in 0 until jIntersections.length()){
                            val node = RoadNode()
                            node.mLength = jStep.getDouble("distance") / 1000.0
                            node.mDuration = jStep.getDouble("duration")
                            val intersection = jIntersections.getJSONObject(j)
                            val location = intersection.getJSONArray("location")
                            val longitude = location[0] as Double
                            val latitude = location[1] as Double
                            node.mLocation = GeoPoint(latitude, longitude)
                            road.mNodes.add(node)
                        }//intersections

                    } //steps
                } //legs
            } //routes

            return roads

        } catch (e: JSONException) {
            e.printStackTrace()
            Log.d("server response", String.format("response exception: %s", e.message))

            return null
        }
    }

}