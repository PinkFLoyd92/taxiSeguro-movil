package com.example.geotaxi.geotaxi.API.endpoints

import com.example.geotaxi.geotaxi.API.API
import com.example.geotaxi.geotaxi.API.ServerAPI
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import retrofit2.Call
/**
 * Created by dieropal on 17/01/18.
 */
class RouteAPI {
    private val retrofit = API.retrofit
    private val serverAPI = retrofit.create(ServerAPI::class.java)

    fun createRoute(location: GeoPoint, destination: GeoPoint, client: String,
                    points: ArrayList<GeoPoint>?, routeIndex: Int, status: String,
                    taxiRequest: Boolean, driver: String?, supersededRoute: String?,
                    waypoints: ArrayList<GeoPoint>?, duration: Double) : Call<JsonObject>? {

        val jsonObject = roadToJson(location, destination, client, points, routeIndex,
                                    status, taxiRequest, driver, supersededRoute, waypoints,
                                    duration)
        return serverAPI?.createRoute(jsonObject)
    }

    private fun roadToJson(location: GeoPoint?, destination: GeoPoint?, client: String?,
                           points: ArrayList<GeoPoint>?, routeIndex: Int?, status: String,
                           taxiRequest: Boolean, driver: String?, supersededRoute: String?,
                           waypoints: ArrayList<GeoPoint>?, duration: Double): JsonObject {
        val json = JsonObject()

        if (location != null) {
            val start = JsonObject()
            val coorStart = JsonArray()
            coorStart.add(location.longitude)
            coorStart.add(location.latitude)
            start.addProperty("type", "Point")
            start.add("coordinates", coorStart)
            json.add("start", start)
        }
        if (destination != null) {
            val end = JsonObject()
            val coorEnd = JsonArray()
            coorEnd.add(destination?.longitude)
            coorEnd.add(destination?.latitude)
            end.addProperty("type", "Point")
            end.add("coordinates", coorEnd)
            json.add("end", end)
        }
        if(points!=null){
            val jsonPoints = JsonArray()
            for(n in points.iterator()){
                val jsonArr = JsonArray()
                jsonArr.add(n.longitude)
                jsonArr.add(n.latitude)
                jsonPoints.add(jsonArr)
            }
            json.add("points", jsonPoints)
        }
        if(waypoints!=null){
            val jsonWaypoints = JsonArray()
            for(n in waypoints.iterator()){
                val jsonArr = JsonArray()
                jsonArr.add(n.longitude)
                jsonArr.add(n.latitude)
                jsonWaypoints.add(jsonArr)
            }
            json.add("waypoints", jsonWaypoints)
        }
        if (client != null) {
            json.addProperty("client", client)
        }

        if (routeIndex != null) {
            json.addProperty("route_index", routeIndex)
        }

        if (status != null) {
            json.addProperty("status", status)
        }

        if (driver != null) {
            json.addProperty("driver", driver)
        }

        if (supersededRoute != null) {
            json.addProperty("supersededRoute", supersededRoute)
        }

        if (taxiRequest != null) {
            json.addProperty("taxiRequest", taxiRequest)
        }

        json.addProperty("duration", duration)

        return json
    }
}