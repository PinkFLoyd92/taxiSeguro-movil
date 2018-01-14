package com.example.geotaxi.taxiseguroconductor.API

import android.util.Log
import com.example.geotaxi.taxiseguroconductor.config.Env
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.io.IOException

/**
 * Created by sebas on 1/13/18.
 */

class API {
    companion object {
        val client = OkHttpClient.Builder()
                .addInterceptor(HTTPInterceptor())
                .build()

        //route request to orsm server
        val retrofit = Retrofit.Builder()
                .baseUrl(Env.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        class HTTPInterceptor : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response? {
                val request = chain.request()

                val t1 = System.nanoTime()
                Log.d("RETROFIT",String.format("Sending request %s on %s%n%s",
                        request.url(), chain.connection(), request.headers()))
                val response = chain.proceed(request)
                val t2 = System.nanoTime()
                Log.d("activity", String.format("Received response for %s in %.1fms%n%s",
                        response.request().url(), (t2 - t1) / 1e6, response.headers()))
                return response
            }
        }
    }
}

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