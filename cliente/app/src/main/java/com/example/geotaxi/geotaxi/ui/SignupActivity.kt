package com.example.geotaxi.geotaxi.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.geotaxi.geotaxi.API.endpoints.SignupAPI
import com.example.geotaxi.geotaxi.R
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    var signupApi: SignupAPI = SignupAPI()
    var name: EditText? = null
    var ID: EditText? = null
    var mobile: EditText? = null
    var username: EditText? = null
    var password: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val signupBtn = findViewById<Button>(R.id.signup_button)
        name = findViewById<EditText>(R.id.name)
        ID = findViewById<EditText>(R.id.id)
        mobile = findViewById<EditText>(R.id.mobile)
        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        signupBtn.setOnClickListener { view -> run {
            signup()
        }}

    }

    private fun signup() {
        val cName = name?.text.toString().trim()
        val cID = ID?.text.toString().trim()
        val cMobile = mobile?.text.toString().trim()
        val cUsername = username?.text.toString().trim()
        val cPassword = password?.text.toString().trim()

        val serverCall : Call<JsonObject>? = signupApi.createUser(cName, cID, cMobile,
                                                                    cUsername, cPassword)
        if(serverCall != null){
            serverCall?.enqueue(object: Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                    Log.d("server response", "Failed")
                    Toast.makeText(applicationContext, "fail to post on server", Toast.LENGTH_SHORT).show()

                }

                override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
                    Log.d("server response", String.format("Server response %s",
                            response.toString()))
                    if (response?.code() == 201) {
                        finish()
                    }
                }
            })
        } else {
            Log.d("RETROFIT", "ServerCAll is null")
        }

    }
}