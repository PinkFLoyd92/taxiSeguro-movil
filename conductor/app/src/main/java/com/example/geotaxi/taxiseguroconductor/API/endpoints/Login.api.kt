package com.example.geotaxi.taxiseguroconductor.API.endpoints

import android.content.Intent
import android.util.Log
import com.example.geotaxi.taxiseguroconductor.API.API
import com.example.geotaxi.taxiseguroconductor.API.ServerAPI
import com.google.gson.JsonObject
import junit.framework.Assert
import okhttp3.OkHttpClient
import retrofit2.Call

/**
 * Created by sebas on 1/13/18.
 */

class LoginAPI {

    public fun auth(username : String, password : String) : Call<JsonObject>? {
        val client = API.client
        val retrofit = API.retrofit

        val serverAPI = retrofit.create(ServerAPI::class.java)
        // val jsonObject = userToJSON(username, password)
        val jsonObject = userToJSON("q", "q")
        if(jsonObject == null) {
            Log.d("RETROFIT", "ERROR IN AUTH, EMPTY JSON OBJECT")
            return null
        } else {
            return serverAPI?.authUser(jsonObject)
        }
    }

    public fun userToJSON(username : String, password : String): JsonObject? {
        try {
            val json = JsonObject()
            val cUsername = username
            val cPassword = password
            json.addProperty("username", cUsername)
            json.addProperty("password", cPassword)
            return json
        } catch(e : Exception) {
            Log.d("RETROFIT", "Something went wrong." + e.toString())
            return null
        }
    }
}