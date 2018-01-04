package com.example.geotaxi.geotaxi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<Button>(R.id.login_button)
        loginBtn.setOnClickListener { view -> run {
            startActivity(Intent(this, MainActivity::class.java))
        }}

        val signupLinkBtn = findViewById<Button>(R.id.signup_link_btn)
        signupLinkBtn.setOnClickListener { view -> run {
            startActivity(Intent(this, SignupActivity::class.java))
        }}

    }
}