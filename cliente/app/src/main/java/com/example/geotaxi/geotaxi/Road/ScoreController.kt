package com.example.geotaxi.geotaxi.Road

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.data.Route
import com.google.gson.JsonObject
import com.stepstone.apprating.AppRatingDialog
import org.osmdroid.util.GeoPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * Created by sebas on 2/21/18.
 */

class ScoreController(val activity: AppCompatActivity): ScoreChecker{
    override fun getScore(route: Route, getScoreAPICall: (points: ArrayList<GeoPoint>) -> Call<JsonObject>?){
        val points = route.roadPoints!!
        val serverCall = getScoreAPICall(points)
        if(serverCall != null){
            serverCall.enqueue(object: Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                    Log.d("server response", "Failed on score")
                }
                override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {

                    if (response?.code()!! in 200..209) {
                        try {
                            val score = response.body()?.get("score")?.asInt
                            Log.d("Score", String.format("Score: %s", score))
                        } catch (exception: Exception){
                            Log.d("EXCEPTION", exception.message)
                        }
                    }
                }

            })
        } else {
            Log.d("RETROFIT", "ServerCAll is null")
        }
    }

    override fun setScoreAndEmit(routeId: String,
                                 score: Number,
                                 emitScore: (routeId: String, score: Number) -> Unit) {
        emitScore(routeId, score) // emit score and save it.
    }

    fun showDialog() {
        activity.runOnUiThread{
            AppRatingDialog.Builder()
                    .setPositiveButtonText("ACEPTAR")
                    .setNegativeButtonText("CANCELAR")
                    .setNoteDescriptions(Arrays.asList("Muy peligrosa", "Peligrosa", "Ok", "Segura", "Muy Segura"))
                    .setDefaultRating(2)
                    .setTitle("CALIFICACION DE RUTA")
                    .setDescription("CALIFIQUE LA RUTA PARA MEJORAR NUESTRO SERVICIO")
                    .setDefaultComment(" Calificacion de ruta ")
                    .setHint("Califique la ruta")
                    .setCommentBackgroundColor(R.color.colorPrimaryDark)
                    .create(activity)
                    .show()
        }
    }
}