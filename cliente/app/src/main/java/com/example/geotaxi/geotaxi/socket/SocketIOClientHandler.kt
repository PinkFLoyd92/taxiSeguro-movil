package com.example.geotaxi.geotaxi.socket

import android.app.Activity
import com.example.geotaxi.geotaxi.config.Env
import com.example.geotaxi.geotaxi.data.User
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket

/**
 * Created by dieropal on 17/01/18.
 */
class SocketIOClientHandler {
    val socket = IO.socket(Env.SOCKET_SERVER_URL)

    fun initConfiguration(activity: Activity) {
        val id_user: String = User.instance._id
        val role: String = User.instance.role
        socket.on(Socket.EVENT_CONNECT) {
            val userInfo = JsonObject()
            userInfo.addProperty("_id", id_user)
            userInfo.addProperty("role", role)
            socket.emit("SENDINFO", userInfo)
        }
        socket.connect()
    }
}