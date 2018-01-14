package com.example.geotaxi.geotaxi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class LoginActivity : AppCompatActivity() {
    var serverAPI: ServerAPI? = null
    var username: EditText? = null
    var password: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.login_button)
        username = findViewById<EditText>(R.id.username)
        password = findViewById<EditText>(R.id.password)
        loginBtn.setOnClickListener { view -> run {
            loginOnServer()
        }}

        val signupLinkBtn = findViewById<Button>(R.id.signup_link_btn)
        signupLinkBtn.setOnClickListener { view -> run {
            startActivity(Intent(this, SignupActivity::class.java))
        }}

    }

    private fun loginOnServer() {
        val client = OkHttpClient.Builder()
                .addInterceptor(MyInterceptor())
                .build()

        //route request to orsm server
        val retrofit = Retrofit.Builder()
                .baseUrl(Env.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        serverAPI = retrofit.create(ServerAPI::class.java)
        val jsonObject = getUserJsonObject()
        val serverCall = serverAPI?.authUser(jsonObject)

        serverCall?.enqueue(object: Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                Log.d("server response", "Failed")
                Toast.makeText(applicationContext, "fail to post on server", Toast.LENGTH_SHORT).show()

            }

            override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {

                if (response?.code() == 200) {
                    try {
                        val clientId = response.body()?.get("_id")?.asString
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.putExtra("client_id", clientId)
                        Toast.makeText(applicationContext, "Server response OK", Toast.LENGTH_SHORT).show()
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.d("activity",String.format("exception on login: %s ", e.message))
                    }
                }
            }
        })
    }

    private fun getUserJsonObject(): JsonObject {
        val json = JsonObject()
        val cUsername = username?.text.toString()
        val cPassword = password?.text.toString().trim()

        json.addProperty("username", cUsername)
        json.addProperty("password", cPassword)

        return json
    }

    inner class MyInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response? {
            val request = chain.request()

            val t1 = System.nanoTime()
            Log.d("activity",String.format("Sending request %s on %s%n%s",
                    request.url(), chain.connection(), request.headers()))

            val response = chain.proceed(request)

            val t2 = System.nanoTime()
            Log.d("activity", String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6, response.headers()))

            return response
        }
    }
}