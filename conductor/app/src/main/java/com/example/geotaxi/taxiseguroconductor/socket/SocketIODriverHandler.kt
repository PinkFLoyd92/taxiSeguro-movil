package com.example.geotaxi.taxiseguroconductor.socket

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.support.v7.widget.CardView
import android.util.Log
import android.view.View
import android.widget.Button
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.taxiseguroconductor.config.Env
import com.example.geotaxi.taxiseguroconductor.data.DataHandler
import com.example.geotaxi.taxiseguroconductor.ui.MainActivity
import com.google.gson.JsonObject
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.util.concurrent.ExecutionException
import org.json.JSONObject



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
    public fun initConfiguration(activity: Activity) {
        val id_user : String = DataHandler.getUserID(activity.baseContext)
        val role : String = DataHandler.getUserRole(activity.baseContext)
        socket.on(Socket.EVENT_CONNECT) {
            val userInfo = JsonObject()
            userInfo.addProperty("_id", id_user)
            userInfo.addProperty("role", role)
            socket.emit("SENDINFO", userInfo)
        }.on("ROUTE REQUEST", object: Emitter.Listener{
            // Aqui llega la informacion de la ruta, puede o no rechazar la solicitud de ruta.
            override fun call(vararg args: Any?) {
                try {
                    val obj = args[0] as JSONObject
                    Log.d("OBJECT: ", obj.toString())
                    activity.runOnUiThread(object: Runnable {
                        override fun run() {
                            activity.findViewById<CardView>(R.id.card_view_confirm_client).visibility = View.VISIBLE
                        }
                    })
                }catch (exception: ExecutionException){
                    Log.d("error" , args.toString())
                }
            }
        }).on("ROUTE - POSITION CLIENT", object: Emitter.Listener {
            override fun call(vararg args: Any?) {
                try {
                    val obj = args[0] as JSONObject
                    Log.d("OBJECT: ", obj.toString())
/*                    activity.runOnUiThread(object: Runnable {
                        override fun run() {
                            activity.findViewById<CardView>(R.id.card_view_confirm_client).visibility = View.VISIBLE
                        }
                    })*/
                }catch (exception: ExecutionException){
                    Log.d("error" , args.toString())
                }
            }

        }).on("ROUTE - FINISH", object: Emitter.Listener {
            override fun call(vararg args: Any?) {
                val obj = args[0] as JSONObject
                Log.d("OBJECT: ", obj.toString())
            }
        })
        socket.connect()
    }
}