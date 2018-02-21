package com.example.geotaxi.geotaxi.socket

import android.support.v7.widget.CardView
import android.util.Log
import android.view.View
import android.widget.Toast
import co.intentservice.chatui.models.ChatMessage
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.chat.ChatMapped
import com.example.geotaxi.geotaxi.config.Env
import com.example.geotaxi.geotaxi.data.Driver
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.map.MapHandler
import com.example.geotaxi.geotaxi.ui.MainActivity
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.util.*
import java.util.concurrent.ExecutionException

/**
 * Created by dieropal on 17/01/18.
 */
class SocketIOClientHandler(
        private val activity: MainActivity,
        private val mapHandler: MapHandler) {

    val socket = IO.socket(Env.SOCKET_SERVER_URL)
    var isFirstDriverPosition = true

    fun initConfiguration() {
        val id_user: String = User.instance._id
        val role: String = User.instance.role
        socket.on(Socket.EVENT_CONNECT) {
            val userInfo = JsonObject()
            userInfo.addProperty("_id", id_user)
            userInfo.addProperty("role", role)
            socket.emit("SENDINFO", userInfo)
            socket.emit("CHAT - GET MONITORS", null) // remover esto.
            socket.emit("CLIENT - IS IN ROUTE?", userInfo)
        }.on("ROUTE - POSITION DRIVER") { args ->
                    if (Route.instance.status != "inactive") {
                        try {
                            val obj = args[0] as JSONObject
                            val latitude = obj.getJSONObject("position").getString("latitude").toDouble()
                            val longitude = obj.getJSONObject("position").getString("longitude").toDouble()
                            val driverGeo = GeoPoint(latitude, longitude)
                            activity.runOnUiThread {
                                mapHandler.updateDriverIconOnMap(driverGeo)
                            }
                            if (isFirstDriverPosition) {
                                activity.runOnUiThread {
                                    val waitingDriver = activity.findViewById<CardView>(R.id.waiting_driver_cv)
                                    waitingDriver.visibility = View.GONE
                                    mapHandler?.animateToLocation(location = driverGeo, zoomLevel = 17)
                                    activity.showDriverInfoDialog(Driver.instance.name, Driver.instance.vehicle_plate)
                                }
                                isFirstDriverPosition = false
                            }
                            Driver.instance.position = driverGeo
                        }catch (exception: ExecutionException){
                            Log.d("error" , args.toString())
                        }
                    }

                }.on("MONITOR - CONNECTED") { args ->
                    if (args[0] == null)
                        return@on
                    val obj = args[0] as JSONObject
                    Log.d("obj", obj.toString())
                    val id = obj.getString("_id")
                    val username =  obj.getString("username")
                    val role =  obj.getString("role")
                    /*if(Route.instance.status == "active" && !activity.chatController
                                    .isMonitorAlreadyCreated(id)) {*/
                    Log.d("information", obj.toString())
                    activity.runOnUiThread {
                        activity.chatController
                                .addMonitor(id_user = id,
                                        username = username,
                                        role =  role)
                    }

                }.on("MONITOR - DISCONNECTED") { args ->
                    if (args[0] == null)
                        return@on
                    val obj = args[0] as JSONObject
                    Log.d("obj", obj.toString())
                    val id = obj.getString("_id")
                    activity.runOnUiThread {
                        activity.chatController
                                .removeMonitor(id_user = id)
                    }
                }.on("ROUTE - CHAT") { args ->
                    if (args[0] == null)
                        return@on
                    val obj = args[0] as JSONObject
                    val route_id =  obj.getString("route_id")
                    val role =  obj.getString("role")
                    val message_text = obj.getJSONObject("message")
                    val from :String = message_text.getString("from")
                    val text :String = message_text.getString("text")
                    val date: Date = Date(message_text.getLong("date"))

                    val chatMessage: ChatMessage = ChatMessage(text,
                            message_text.getLong("date"), ChatMessage.Type.RECEIVED)
                    val chatMapped:ChatMapped? = activity.chatController.chatList.chats.find {
                        it.monitor_id == from
                    }
                    if(chatMapped != null) {
                        activity.chatController.chatList.selectedChat = chatMapped
                        chatMapped.messages.add(chatMessage)
                        activity.chatController.tryToAddMessage(chatMessage)

                    }
                    else {
                        Log.d("ROUTECHAT", "No existe ese monitor")
                    }

                } .on("CHAT - MONITORS") { args ->
            if (args[0] == null)
                return@on
            val obj = args[0] as JSONObject
            Log.d("obj", obj.toString())
            val id = obj.getString("_id")
            val username =  obj.getString("username")
            val role =  obj.getString("role")

            /*if(Route.instance.status == "active" && !activity.chatController
                            .isMonitorAlreadyCreated(id)) {*/

            Log.d("information", obj.toString())
            activity.runOnUiThread {
                activity.chatController
                        .addMonitor(id_user = id,
                                username = username,
                                role =  role)
            }
        }
                .on("DRIVER - CHOSEN") { args ->
                    try {
                        val obj = args[0] as JSONObject
                        val id = obj.getString("_id")
                        val name =  obj.getString("name")
                        val cedula = obj.getString("cedula")
                        val mobile = obj.getString("mobile")
                        val vehiclePlate = obj.getString("vehicle_plate")
                        val vehicle_description = obj.getString("vehicle_description")

                        Driver.instance._id = id
                        Driver.instance.name = name
                        Driver.instance.cedula = cedula
                        Driver.instance.mobile = mobile
                        Driver.instance.vehicle_plate = vehiclePlate
                        Driver.instance.vehicle_description = vehicle_description


                    }catch (exception: ExecutionException){
                        Log.d("error" , args.toString())
                    }
                }.on("CLIENT - IS IN ROUTE") { args ->
                    /*
                    *  here we expect to receive the route's information, including the user's information.
                    * */
                    try {
                        val obj = args[0] as JSONObject // here we have the route Object
                        initRouteAtLaunch(obj)
                    } catch (e: Exception) {
                        Log.d("error", e.message)
                    }
                }.on("ROUTE - START") { args ->
                    Log.d("SOCKET", String.format("ROUTE - START"))
                    val obj = args[0] as JSONObject
                    val routeStatus = obj.getString("routeStatus")

                    Route.instance.status = routeStatus
                    socket.emit("CHAT - GET MONITORS", null)
                    activity.runOnUiThread {
                        mapHandler.animateToLocation(location = User.instance.position, zoomLevel = 17)
                        activity.enablePanicButton()
                    }
                }.on("ROUTE CHANGE - REQUEST") {args ->
                    val obj = args[0] as JSONObject
                    val duration = obj.getDouble("duration")
                    val routeIndex = obj.getInt("routeIndex")
                    val jArray = obj.getJSONArray("points")
                    val points = arrayListOf<GeoPoint>()
                    (0 until jArray.length())
                            .map { jArray.getJSONObject(it) }
                            .mapTo(points) {
                                GeoPoint(it.get("latitude") as Double,
                                        it.get("longitude") as Double)
                            }
                    activity.runOnUiThread {

                        if (points.isNotEmpty()) {
                            val roadOverlay = Polyline()
                            roadOverlay.points = points
                            mapHandler.drawRoadOverlay(roadOverlay, duration, driverRequest = true)
                            activity.showRouteChangeDialog(roadOverlay, duration, routeIndex)
                        }
                    }
                }.on("ROUTE - FINISH") {
                    Log.d("OBJECT: ", "ROUTE HAS FINISHED")
                    Route.instance.status = "inactive"
                    Route.instance.currentRoad = null
                    mapHandler.isChoosingDestination = true
                    activity.runOnUiThread {
                        mapHandler.resetMapOverlays()
                        activity.setSearchLayoutVisibility(/*Visible default*/)
                        Toast.makeText(activity, "La Ruta ha Finalizado", Toast.LENGTH_SHORT).show()
                        mapHandler.updateUserIconOnMap(User.instance.position!!)
                    }

                    // ask the score to user and emit the score
                    activity.scoreController.showDialog()
                }
        socket.connect()
    }

    fun emitMessage(chatMessage: ChatMessage) {
        val chatMapped: ChatMapped = activity.chatController.chatList.selectedChat

        val messageInfo = JsonObject()
        val message = JsonObject()
        messageInfo.addProperty("route_id", Route.instance._id)
        messageInfo.addProperty("role", User.instance.role)

        message.addProperty("from", User.instance._id)
        message.addProperty("position", "left")
        message.addProperty("type", "text")
        message.addProperty("value", chatMessage.message)
        message.addProperty("text", chatMessage.message)
        message.addProperty("date", chatMessage.timestamp)
        message.addProperty("to", chatMapped.monitor_id)
        messageInfo.add("message", message)

        socket.emit("CHAT - SEND FROM USER", messageInfo)
    }
    fun initRouteAtLaunch(obj: JSONObject) {
        /*val startLoc: Location? = Location("")
        val endLoc: Location? = Location("")
        try {
            Log.d("OBJECT", obj.toString())
            Route.instance.status = obj.getString("status")
            Route.instance._id = obj.getString("_id")
            Route.instance.client = obj.getJSONObject("client").getString("_id")
            Route.instance.driver = obj.getJSONObject("driver").getString("_id")
            startLoc?.longitude = obj.getJSONObject("start").getJSONArray("coordinates").get(0) as Double
            startLoc?.latitude = obj.getJSONObject("start").getJSONArray("coordinates").get(1)  as Double
            Route.instance.start = GeoPoint(startLoc)

            endLoc?.longitude = obj.getJSONObject("end").getJSONArray("coordinates").get(0) as Double
            endLoc?.latitude = obj.getJSONObject("end").getJSONArray("coordinates").get(1)  as Double
            Route.instance.end = GeoPoint(endLoc)
            val waypoints = Route.instance.waypoints
            activity.runOnUiThread{
                val roads = activity.roadHandler.executeRoadTask(endGp = Route.instance.end as GeoPoint, startGp = Route.instance.start as GeoPoint)
                if (roads != null && roads.isNotEmpty()) {
                    activity.setSearchLayoutVisibility(View.GONE)
                }
            }

        }catch (e : Exception) {
            Log.d("error", e.message)
        }*/
    }

    fun emitScore(routeId: String, score: Number) {
        val info = JsonObject()
        info.addProperty("route_id", routeId)
        info.addProperty("score", score)
        socket.emit("SCORE - EMITTED", info)
    }

    val emitScore = {
        routeId: String, score: Number
        ->
        emitScore(routeId, score)
    }
}