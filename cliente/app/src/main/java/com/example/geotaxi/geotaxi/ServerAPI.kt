package com.example.geotaxi.geotaxi

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.*

interface ServerAPI {
    @Headers( "Content-Type: application/json" )
    @POST("routes/")
    fun createRoute(
            @Body body: JsonObject
            ): Call<JsonObject>

    @Headers( "Content-Type: application/json" )
    @POST("users/")
    fun createUser(
            @Body body: JsonObject
    ): Call<JsonObject>

    @Headers( "Content-Type: application/json" )
    @POST("users/auth")
    fun authUser(
            @Body body: JsonObject
    ): Call<JsonObject>

}