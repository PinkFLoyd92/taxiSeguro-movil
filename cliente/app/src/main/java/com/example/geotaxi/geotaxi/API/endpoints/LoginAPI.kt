package com.example.geotaxi.geotaxi.API.endpoints

import android.util.Log
import com.example.geotaxi.geotaxi.API.API
import com.example.geotaxi.geotaxi.API.ServerAPI
import com.google.gson.JsonObject
import retrofit2.Call

/**
 * Created by dieropal on 17/01/18.
 */
class LoginAPI {

    fun auth(username: String, password: String): Call<JsonObject>? {
        val retrofit = API.retrofit
        val serverAPI = retrofit.create(ServerAPI::class.java)
        val jsonObject = userToJSON("client1", "123456")
        if (jsonObject == null) {
            Log.d("RETROFIT", "ERROR IN AUTH, EMPTY JSON OBJECT")
            return null
        } else {
            return serverAPI?.authUser(jsonObject)
        }
    }

    fun userToJSON(username : String, password : String): JsonObject? {
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