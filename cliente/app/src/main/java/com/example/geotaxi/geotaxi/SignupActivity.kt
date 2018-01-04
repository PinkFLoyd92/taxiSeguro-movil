package com.example.geotaxi.geotaxi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val signupBtn = findViewById<Button>(R.id.signup_button)
        signupBtn.setOnClickListener { view -> run {
            startActivity(Intent(this, MainActivity::class.java))
        }}

    }
}