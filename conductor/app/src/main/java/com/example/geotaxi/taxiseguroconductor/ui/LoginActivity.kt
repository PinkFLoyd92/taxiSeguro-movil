package com.example.geotaxi.taxiseguroconductor.ui

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.geotaxi.taxiseguroconductor.API.endpoints.LoginAPI
import com.example.geotaxi.taxiseguroconductor.R
import com.example.geotaxi.taxiseguroconductor.data.DataHandler
import com.example.geotaxi.taxiseguroconductor.data.User
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    var username: EditText? = null
    var password: EditText? = null
    var loginApi: LoginAPI = LoginAPI()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        this.username = findViewById(R.id.username)
        this.password = findViewById(R.id.password)
        val loginBtn = findViewById<Button>(R.id.login_button)
        loginBtn.setOnClickListener { view -> run {
            this.eventLogin()
        }}

        val signupLinkBtn = findViewById<Button>(R.id.signup_link_btn)
        signupLinkBtn.setOnClickListener { view -> run {
            startActivity(Intent(this, SignupActivity::class.java))
        }}

    }

    fun eventLogin() {
        val serverCall : Call<JsonObject>? = loginApi.auth(this.username?.text.toString(), this.password?.text.toString().trim())
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
                            val name = response.body()?.get("name")?.asString
                            val role = response.body()?.get("role")?.asString
                            val intent = Intent(applicationContext, MainActivity::class.java)
                            intent.putExtra("client_id", clientId)
                            if(username != null) {
                                User.instance.username = username
                            }
                            if(name != null) {
                                User.instance.name = name
                            }
                            if(clientId != null) {
                                User.instance._id = clientId
                                DataHandler.saveUser(baseContext, clientId)
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