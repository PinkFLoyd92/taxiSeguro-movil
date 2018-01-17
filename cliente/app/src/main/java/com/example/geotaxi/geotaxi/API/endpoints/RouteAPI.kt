package com.example.geotaxi.geotaxi.API.endpoints

import com.example.geotaxi.geotaxi.API.API
import com.example.geotaxi.geotaxi.API.ServerAPI
import com.example.geotaxi.geotaxi.data.User
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import retrofit2.Call

/**
 * Created by dieropal on 17/01/18.
 */
class RouteAPI {

    fun requestTaxi(location: GeoPoint, destination: GeoPoint,
                    waypoints: ArrayList<RoadNode>) : Call<JsonObject>? {
        val retrofit = API.retrofit
        val serverAPI = retrofit.create(ServerAPI::class.java)
        val jsonObject = roadToJson(location, destination, waypoints)
        return serverAPI?.createRoute(jsonObject)
    }

    private fun roadToJson(location: GeoPoint, destination: GeoPoint, waypoints: ArrayList<RoadNode>): JsonObject {
        val json = JsonObject()
        val jsonPoints = JsonArray()
        val start = JsonObject()
        val end = JsonObject()
        val coorStart = JsonArray()
        val coorEnd = JsonArray()
        coorStart.add(location.longitude)
        coorStart.add(location.latitude)
        coorEnd.add(destination?.longitude)
        coorEnd.add(destination?.latitude)
        start.addProperty("type", "Point")
        start.add("coordinates", coorStart)
        end.addProperty("type", "Point")
        end.add("coordinates", coorEnd)
        if(waypoints!=null){
            for(n in waypoints.iterator()){
                val jsonArr = JsonArray()
                jsonArr.add(n.mLocation.longitude)
                jsonArr.add(n.mLocation.latitude)
                jsonPoints.add(jsonArr)
            }
        }
        json.addProperty("client", User.instance._id)
        json.add("start", start)
        json.add("end", end)
        json.add("points", jsonPoints)

        return json
    }
}