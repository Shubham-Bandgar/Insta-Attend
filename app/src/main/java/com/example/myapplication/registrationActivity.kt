package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dev.skomlach.biometric.compat.AuthenticationFailureReason
import dev.skomlach.biometric.compat.AuthenticationResult
import dev.skomlach.biometric.compat.BiometricApi
import dev.skomlach.biometric.compat.BiometricAuthRequest
import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricManagerCompat
import dev.skomlach.biometric.compat.BiometricPromptCompat
import dev.skomlach.biometric.compat.BiometricType

class registrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        val txtButton = findViewById<TextView>(R.id.textView3)

        txtButton.setOnClickListener{
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val buttonNavigate = findViewById<Button>(R.id.button5)
        buttonNavigate.setOnClickListener {
            startBioAuth()
        }
    }

    private fun registerUser(){
        val usernameEditText = findViewById<EditText>(R.id.editTextText5)
        val emailEditText = findViewById<EditText>(R.id.editTextTextEmailAddress4)
        val passwordEditText = findViewById<EditText>(R.id.editTextTextPassword5)
        val confirmPasswordEditText = findViewById<EditText>(R.id.editTextTextPassword6)
        val phoneNumberEditText = findViewById<EditText>(R.id.phoneNumberEditText)

        val username = usernameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val phoneNumber = phoneNumberEditText.text.toString().trim()

        if (password == confirmPassword) {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid
                        val employeeDetails = hashMapOf(
                            "username" to username,
                            "email" to email,
                            "phoneNumber" to phoneNumber,
                            "isEnrolled" to false
                        )

                        if (uid != null) {
                            firestore.collection("employeeDetails")
                                .document(uid)
                                .set(employeeDetails)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Registration successful",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this, loginActivity::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to store employee details",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                    } else {
                        Toast.makeText(
                            this,
                            "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
        }
    }
    private fun startBioAuth() {
        val faceId = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_ANY,
            BiometricConfirmation.ANY
        )

        if (!BiometricManagerCompat.isHardwareDetected(faceId) ||
            !BiometricManagerCompat.hasEnrolled(faceId)
        ) {
            return
        }

        val prompt = BiometricPromptCompat.Builder(this).apply {
            this.setTitle("Register your biometric")
        }

        prompt.build().authenticate(object : BiometricPromptCompat.AuthenticationCallback() {
            override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
                super.onSucceeded(confirmed)
                registerUser()
            }

            override fun onCanceled() {
                showToast("Authentication Cancelled")
            }

            override fun onFailed(
                reason: AuthenticationFailureReason?,
                dialogDescription: CharSequence?
            ) {
                showToast("You are not the person we know !")
            }
        })
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
