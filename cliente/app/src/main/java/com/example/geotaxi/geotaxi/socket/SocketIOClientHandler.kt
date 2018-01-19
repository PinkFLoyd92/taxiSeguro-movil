package com.example.geotaxi.geotaxi.socket

import android.app.Activity
import android.location.Location
import android.support.design.widget.BottomSheetDialog
import android.util.Log
import android.widget.TextView
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.config.Env
import com.example.geotaxi.geotaxi.data.Driver
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.map.MapHandler
import com.example.geotaxi.geotaxi.ui.MainActivity
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_signup.view.*
import kotlinx.android.synthetic.main.sheet_dialog.view.*
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.nio.file.DirectoryIteratorException
import java.util.concurrent.ExecutionException

/**
 * Created by dieropal on 17/01/18.
 */
class SocketIOClientHandler(
        private val activity: MainActivity,
        private val mapHandler: MapHandler,
        private val bSheetDialog: BottomSheetDialog ) {

    val socket = IO.socket(Env.SOCKET_SERVER_URL)

    fun initConfiguration() {
        val id_user: String = User.instance._id
        val role: String = User.instance.role
        socket.on(Socket.EVENT_CONNECT) {
            val userInfo = JsonObject()
            userInfo.addProperty("_id", id_user)
            userInfo.addProperty("role", role)
            socket.emit("SENDINFO", userInfo)
        }.on("ROUTE - POSITION DRIVER") { args ->
            try {
                val obj = args[0] as JSONObject
                val latitude = obj.getJSONObject("position").getString("latitude").toDouble()
                val longitude = obj.getJSONObject("position").getString("longitude").toDouble()
                val driverGeo = GeoPoint(latitude, longitude)
                mapHandler.updateDriverIconOnMap(activity, driverGeo)
                Driver.instance.position = driverGeo
            }catch (exception: ExecutionException){
                Log.d("error" , args.toString())
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
        }
        socket.connect()
    }
}