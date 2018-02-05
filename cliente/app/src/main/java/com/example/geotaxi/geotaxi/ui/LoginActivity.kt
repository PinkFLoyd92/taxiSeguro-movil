package com.example.geotaxi.geotaxi.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.geotaxi.geotaxi.API.endpoints.LoginAPI
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.data.User
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    var loginApi: LoginAPI = LoginAPI()
    var username: EditText? = null
    var password: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.login_button)
        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        loginBtn.setOnClickListener { view -> run {
            login()
        }}

        val signupLinkBtn = findViewById<Button>(R.id.signup_link_btn)
        signupLinkBtn.setOnClickListener { view -> run {
            startActivity(Intent(this, SignupActivity::class.java))
        }}
        login()

    }

    fun login() {
        val serverCall : Call<JsonObject>? = loginApi.auth("client1", "123456")
        //val serverCall : Call<JsonObject>? = loginApi.auth(this.username?.text.toString(), this.password?.text.toString().trim())
        if(serverCall != null){
            serverCall?.enqueue(object: Callback<JsonObject> {
                override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                    Log.d("RETROFIT", "Failed Post Request")
                    Toast.makeText(applicationContext, "Check your credentials", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {

                    if (response?.code() == 200) {
                        try {
                            val clientId = response.body()?.get("_id")?.asString
                            val username = response.body()?.get("username")?.asString
                            val role = response.body()?.get("role")?.asString
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            if(username != null) {
                                User.instance.username = username
                            }
                            if(clientId != null) {
                                User.instance._id = clientId
                                if(role != null)
                                    User.instance.role = role
                                Log.d("RETROFIT", clientId)
                            }
                            else {
                                Log.d("RETROFIT", "client_id is empty.")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.d("RETROFIT",String.format("exception on login: %s ", e.message))
                        }
                    }
                }
            })
        } else {
            Log.d("RETROFIT", "ServerCAll is null")
        }
    }

}