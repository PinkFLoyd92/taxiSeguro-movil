package com.example.geotaxi.geotaxi.API.endpoints

import android.util.Log
import com.example.geotaxi.geotaxi.API.API
import com.example.geotaxi.geotaxi.API.ServerAPI
import com.google.gson.JsonObject
import retrofit2.Call

/**
 * Created by dieropal on 17/01/18.
 */
class SignupAPI {

    fun createUser(name: String, ID: String, mobile: String
                    , username: String, password: String): Call<JsonObject>? {
        val retrofit = API.retrofit
        val serverAPI = retrofit.create(ServerAPI::class.java)
        val jsonObject = userToJSON(name, ID, mobile, username, password)
        if (jsonObject == null) {
            Log.d("RETROFIT", "ERROR IN AUTH, EMPTY JSON OBJECT")
            return null
        } else {
            return serverAPI?.createUser(jsonObject)
        }
    }

    private fun userToJSON(name: String, ID: String, mobile: String
                           , username: String, password: String): JsonObject {
        val json = JsonObject()
        val role = "client"
        json.addProperty("name", name)
        json.addProperty("cedula", ID)
        json.addProperty("mobile", mobile)
        json.addProperty("username", username)
        json.addProperty("password", password)
        json.addProperty("role", role)

        return json
    }
}