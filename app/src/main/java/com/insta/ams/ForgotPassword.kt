package com.insta.ams

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPassword : AppCompatActivity() {

    private lateinit var emailField : EditText
    private lateinit var forgotButton: Button
    private lateinit var auth : FirebaseAuth
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        emailField = findViewById(R.id.emailField)
        forgotButton = findViewById(R.id.btnrst)

        auth = FirebaseAuth.getInstance()

        forgotButton.setOnClickListener {
            val mail = emailField.text.toString()
            auth.sendPasswordResetEmail(mail)
                .addOnSuccessListener {
                    Toast.makeText(this, "Please check email for password reset link", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, loginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Please Check Your Email", Toast.LENGTH_SHORT).show()
                }

        }

    }
}