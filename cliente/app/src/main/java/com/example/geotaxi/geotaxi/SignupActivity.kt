package com.example.geotaxi.geotaxi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class SignupActivity : AppCompatActivity() {
    var serverAPI: ServerAPI? = null
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
            signupOnServer()
            //startActivity(Intent(this, MainActivity::class.java))
        }}

    }

    private fun signupOnServer() {
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
        val serverCall = serverAPI?.createUser(jsonObject)

        serverCall?.enqueue(object: Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                Log.d("server response", "Failed")
                Toast.makeText(applicationContext, "fail to post on server", Toast.LENGTH_SHORT).show()

            }

            override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
                Log.d("server response", String.format("Server response %s",
                        response.toString()))
                if (response?.code() == 201) {
                    try {
                        val clientId = response.body()?.get("_id")?.asString
                        //Log.d("activity",String.format("id response %s ", clientId))
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        intent.putExtra("client_id", clientId)
                        Toast.makeText(applicationContext, "Server response OK", Toast.LENGTH_SHORT).show()
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.d("activity",String.format("exception on signup: %s ", e.message))
                    }

                }
            }
        })
    }

    private fun getUserJsonObject(): JsonObject {
        val json = JsonObject()
        val cName = name?.text.toString()
        val cPassword = password?.text.toString().trim()
        val role = "client"
        json.addProperty("name", cName)
        json.addProperty("password", cPassword)
        json.addProperty("role", role)

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