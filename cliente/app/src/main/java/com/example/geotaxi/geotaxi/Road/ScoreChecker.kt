package com.example.geotaxi.geotaxi.Road

import com.example.geotaxi.geotaxi.data.Route
import com.google.gson.JsonObject
import org.osmdroid.util.GeoPoint
import retrofit2.Call

/**
 * Created by sebas on 2/20/18.
 */

interface ScoreChecker {
    fun getScore(routePoints: ArrayList<GeoPoint>, getScoreAPICall: (points: ArrayList<GeoPoint>) -> Call<JsonObject>?): Int?
    fun setScoreAndEmit(routeId: String, score: Number, emitScore: (routeId: String, score: Number) -> Unit)
}