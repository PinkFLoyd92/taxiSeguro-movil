package com.example.geotaxi.geotaxi.Road

import android.content.Context
import android.os.AsyncTask
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
import kotlin.collections.ArrayList

/**
 * Created by sebas on 2/21/18.
 */

class ScoreController(val activity: AppCompatActivity): ScoreChecker{
    override fun getScore(routePoints: ArrayList<GeoPoint>, getScoreAPICall: (points: ArrayList<GeoPoint>) -> Call<JsonObject>?) : Int?{
        val serverCall = getScoreAPICall(routePoints)
        val scoreTask = getScoreTask(serverCall)
        val response = scoreTask.execute().get() ?: return null
        if (response.code() in 200..209) {
            try {
                val score = response.body()?.get("score")?.asInt
                Log.d("Score", String.format("Score: %s", score))
                return score
            } catch (exception: Exception){
                Log.d("EXCEPTION", exception.message)
            }
        }
        return null
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

    inner class getScoreTask(val serverCall: Call<JsonObject>?) : AsyncTask<Context, Void, Response<JsonObject>?>() {

        override fun doInBackground(vararg params: Context?): Response<JsonObject>? {
            if (serverCall != null ) {
                return serverCall.execute()
            }
            return null
        }
    }
}