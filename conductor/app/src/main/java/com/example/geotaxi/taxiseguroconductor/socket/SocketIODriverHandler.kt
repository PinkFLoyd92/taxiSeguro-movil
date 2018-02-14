package com.example.geotaxi.taxiseguroconductor.socket

import android.location.Location
import android.support.v7.widget.CardView
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.taxiseguroconductor.config.Env
import com.example.geotaxi.taxiseguroconductor.data.Client
import com.example.geotaxi.taxiseguroconductor.data.DataHandler
import com.example.geotaxi.taxiseguroconductor.data.Route
import com.example.geotaxi.taxiseguroconductor.data.User
import com.example.geotaxi.taxiseguroconductor.map.MapHandler
import com.example.geotaxi.taxiseguroconductor.ui.MainActivity
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import java.util.concurrent.ExecutionException
import org.json.JSONObject
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint


/**
 * Created by sebas on 1/11/18.
 * Class used to handle all the logic that is handled by the websockets.
 */
class SocketIODriverHandler {
    public val socket = IO.socket(Env.SOCKET_SERVER_URL)

    /* Configuration for the driver
    *   @EVENT_CONNECT: We emit the userInfo, we pick the ID from
    *   the SharedPreferences.
    * */
    public fun initConfiguration(activity: MainActivity, mapHandler: MapHandler) {
        val id_user : String = DataHandler.getUserID(activity.baseContext)
        val role : String = DataHandler.getUserRole(activity.baseContext)
        socket.on(Socket.EVENT_CONNECT) {
            val userInfo = JsonObject()
            userInfo.addProperty("_id", id_user)
            userInfo.addProperty("role", role)
            socket.emit("SENDINFO", userInfo)
            socket.emit("DRIVER - IS IN ROUTE?", userInfo)
        } .on("DRIVER - IS IN ROUTE") { args ->
            /*
            *  here we expect to receive the route's information, including the user's information.
            * */
            val obj = args[0] as JSONObject // here we have the route Object
            mapHandler.initRouteAtLaunch(activity, obj)
        }.on("ROUTE REQUEST") { args ->
            // Aqui llega la informacion de la ruta, el conductor siempre acepta la peticion.
            try {
                val obj = args[0] as JSONObject
                val startLoc: Location = Location("")
                val endLoc: Location = Location("")
                val clientLoc : Location = Location("")
                var clientName :String? = null
                Log.d("OBJECT: ", obj.toString())
                // Executing mapHandler task
                try {
                    clientName = obj.getJSONObject("user").getString("name")
                    Client.instance._id = obj.getJSONObject("user").getString("_id")
                    Client.instance.mobile = obj.getJSONObject("user").getString("mobile")
                }catch(e: org.json.JSONException){
                    Log.d("error", e.message)
                    clientName = ""
                }
                if(clientName != null) {
                    Client.instance.name = clientName
                }
                val routeId : String = obj.getJSONObject("route").getString("_id")
                Route.instance._id = routeId
                Route.instance.status = "pending"
                Route.instance.currentRoadIndex = obj.getJSONObject("route").getInt("route_index")
                clientLoc.latitude  = obj.getJSONObject("user").getJSONObject("location").getJSONArray("coordinates").get(1) as Double
                clientLoc.longitude  = obj.getJSONObject("user").getJSONObject("location").getJSONArray("coordinates").get(0) as Double
                startLoc.latitude  = obj.getJSONObject("route").getJSONObject("start").getJSONArray("coordinates").get(1) as Double
                startLoc.longitude  = obj.getJSONObject("route").getJSONObject("start").getJSONArray("coordinates").get(0) as Double
                endLoc.latitude  = obj.getJSONObject("route").getJSONObject("end").getJSONArray("coordinates").get(1) as Double
                endLoc.longitude  = obj.getJSONObject("route").getJSONObject("end").getJSONArray("coordinates").get(0) as Double
                val start = GeoPoint(startLoc)
                val end = GeoPoint(endLoc)
                Route.instance.start = start
                Route.instance.end = end
                mapHandler.destPosition = end
                val clientGeo = GeoPoint(clientLoc)
                Client.instance.position = clientGeo
                val roads = activity.roadHandler.executeRoadTask(start, end) // creating the road.
                if (roads != null) {
                    val points = RoadManager.buildRoadOverlay(roads[Route.instance.currentRoadIndex]).points
                    Route.instance.currentRoad = roads[Route.instance.currentRoadIndex]
                    Route.instance.roads = roads
                    Route.instance.waypoints = points as ArrayList<GeoPoint>
                    socket.emit("JOIN ROUTE", routeId)
                    activity.runOnUiThread {
                        mapHandler.drawRoad(roads[Route.instance.currentRoadIndex], Route.instance.start!!)
                        activity.fabRoutes?.visibility = View.VISIBLE
                        activity.findViewById<TextView>(R.id.input_nombre_cliente).text = Client.instance.name
                        activity.findViewById<TextView>(R.id.input_mobile_cliente).text = Client.instance.mobile
                        activity.findViewById<CardView>(R.id.card_view_confirm_client).visibility = View.VISIBLE
                    }
                }

            }catch (exception: ExecutionException){
                Log.d("error" , args.toString())
            }
        }.on("ROUTE - POSITION CLIENT") { args ->
            if (Route.instance.status != "inactive") {
                try {
                    val obj = args[0] as JSONObject
                    Log.d("OBJECT CLIENT POSITION", "ROUTE - POSITION CLIENT")
                    val clientLoc: Location = Location("")
                    clientLoc.latitude = obj.getJSONObject("position").getString("latitude").toDouble()
                    clientLoc.longitude = obj.getJSONObject("position").getString("longitude").toDouble()
                    val clientGeo : GeoPoint = GeoPoint(clientLoc)
                    mapHandler.updateClientIconOnMap(clientGeo)
                    Client.instance.position = clientGeo
                }catch (exception: ExecutionException){
                    Log.d("error" , args.toString())
                }
            }
        }.on("ROUTE CHANGE - RESULT") { args ->
            activity.runOnUiThread {
                activity.progressBarConfirmation?.visibility = View.GONE
                activity.fabRoutes?.visibility = View.VISIBLE
                val status = args[0] as String
                if (status == "ok") {
                    val route = args[1] as JSONObject
                    val roadIndexChosen = mapHandler.getRoadIndexChosen()
                    val points = RoadManager.buildRoadOverlay(mapHandler.getRoadChosen()).points
                    Route.instance._id = route.getString("_id")
                    Route.instance.currentRoad = mapHandler.getRoadChosen()
                    Route.instance.currentRoadIndex = roadIndexChosen
                    Route.instance.waypoints = points as ArrayList<GeoPoint>
                    Route.instance.roads = mapHandler.alternativeRoutes
                    Toast.makeText(activity, "Solicitud de cambio aceptada", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "Solicitud de cambio negada", Toast.LENGTH_LONG).show()
                    mapHandler.clearMapOverlays()
                    mapHandler.drawRoad(Route.instance.currentRoad!!, User.instance.position!!)

                }
            }
        }.on("ROUTE - FINISH") {
            Route.instance.status = "inactive"
            activity.runOnUiThread {
                mapHandler.clearMapOverlays()
                mapHandler?.updateDriverIconOnMap(User.instance.position!!)
                Toast.makeText(activity, "La Ruta ha Finalizado", Toast.LENGTH_SHORT).show()
            }
        }
        socket.connect()
    }
}