package com.example.geotaxi.taxiseguroconductor.socket

import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import com.example.geotaxi.taxiseguroconductor.ui.MainActivity
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.util.concurrent.ExecutionException

/**
 * Created by sebas on 1/11/18.
 * Class used to handle all the logic that is handled by the websockets.
 */
class SocketIODriverHandler {
    val socket = IO.socket(MainActivity.IP_SOCKET_SERVER)

    public fun initConfiguration(activity: Activity) {
        socket.on(Socket.EVENT_CONNECT, object: Emitter.Listener {
            override fun call(vararg args: Any?) {
                Log.d("Connected", "Socket conectado")

            }

        }).on("CONDUCTOR_ASIGNADO", object: Emitter.Listener{
            // Aqui llega la informacion de la ruta, puede o no rechazar la solicitud de ruta.
            override fun call(vararg args: Any?) {
                try {
                    Log.d("Status" , args as String)
                    val alertDialog = AlertDialog.Builder(activity).create()
                    alertDialog.setTitle("NUEVA RUTA")
                    alertDialog.setMessage(args as String);
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancelar"
                    ) { dialog, wich ->
                        dialog.dismiss()
                        //socket.emit()
                    }

                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Aceptar") {
                        dialog, which -> run {
                        //Do alert actions here
                    }
                    }
                    alertDialog.show()
                }catch (exception: ExecutionException){
                    Log.d("error" , args.toString())
                }
            }
        }). on("CONDUCTOR_RECHAZADO", object: Emitter.Listener{
            // Aqui llega la informacion de la ruta
            override fun call(vararg args: Any?) {
                try {
                    Log.d("Status" , args as String)
                }catch (exception: ExecutionException){
                    Log.d("error" , args.toString())
                }
            }
        })
        socket.connect()
    }
}