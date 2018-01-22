package com.example.geotaxi.geotaxi.socket

import android.app.Activity
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.geotaxi.geotaxi.config.Env
import com.example.geotaxi.geotaxi.data.Driver
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.map.MapHandler
import com.example.geotaxi.geotaxi.ui.MainActivity
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.concurrent.ExecutionException

/**
 * Created by dieropal on 17/01/18.
 */
class SocketIOClientHandler(
        private val activity: MainActivity,
        private val mapHandler: MapHandler ) {

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
            socket.emit("CLIENT - IS IN ROUTE?", userInfo)
        }.on("ROUTE - POSITION DRIVER") { args ->
            if (Route.instance.status != "inactive") {
                Log.d("OBJECT driver POSITION", String.format("ROUTE - POSITION DRIVER"))
                try {
                    val obj = args[0] as JSONObject
                    val latitude = obj.getJSONObject("position").getString("latitude").toDouble()
                    val longitude = obj.getJSONObject("position").getString("longitude").toDouble()
                    val driverGeo = GeoPoint(latitude, longitude)
                    mapHandler.updateDriverIconOnMap(activity, driverGeo)
                    if (isFirstDriverPosition) {
                        activity.runOnUiThread {
                            mapHandler?.animateToLocation(location = driverGeo, zoomLevel = 17)
                        }
                        isFirstDriverPosition = false
                    }
                    Driver.instance.position = driverGeo
                }catch (exception: ExecutionException){
                    Log.d("error" , args.toString())
                }
            }
        }.on("DRIVER - CHOSEN") { args ->
            try {
                Log.d("OBJECT driver POSITION", String.format("DRIVER - CHOOSED"))
                val obj = args[0] as JSONObject
                val name =  obj.getString("name")
                val cedula = obj.getString("cedula")
                val mobile = obj.getString("mobile")
                val vehiclePlate = obj.getString("vehicle_plate")
                val vehicle_description = obj.getString("vehicle_description")

                Driver.instance.name = name
                Driver.instance.cedula = cedula
                Driver.instance.mobile = mobile
                Driver.instance.vehicle_plate = vehiclePlate
                Driver.instance.vehicle_description = vehicle_description

                activity.runOnUiThread {
                    activity.showBottomSheetDialog(name, vehiclePlate)
                }

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

            activity.runOnUiThread {
                mapHandler?.animateToLocation(location = User.instance.position, zoomLevel = 17)
                activity.enablePanicButton()
            }
        }.on("ROUTE - FINISH") {
            Log.d("OBJECT: ", "ROUTE HAS FINISHED")
            Route.instance.status = "inactive"
            Route.instance.road = null
            mapHandler?.clearMapOverlays()
            activity.runOnUiThread {
                mapHandler.closeDestinationWindowInfo()
                activity.setSearchLayoutVisibility(/*Visible default*/)
                Toast.makeText(activity, "La Ruta ha Finalizado", Toast.LENGTH_SHORT).show()
            }
            mapHandler?.updateUserIconOnMap(activity, User.instance.position!!)
        }
        socket.connect()
    }

    fun initRouteAtLaunch(obj: JSONObject) {
        val startLoc: Location? = Location("")
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
            activity.runOnUiThread{
                val ok = activity.executeRoadTask(endGp = Route.instance.end as GeoPoint, startGp = Route.instance.start as GeoPoint)
                if (ok) {
                    activity.setSearchLayoutVisibility(View.GONE)
                }
            }

        }catch (e : Exception) {
            Log.d("error", e.message)
        }
    }
}