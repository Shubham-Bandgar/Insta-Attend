package com.example.myapplication
import android.annotation.SuppressLint
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

class loginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @SuppressLint("WrongViewCast", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val loginButton = findViewById<Button>(R.id.Login1)
        val registerButton = findViewById<TextView>(R.id.btnreg)
        val forgotButton = findViewById<TextView>(R.id.forgot)

        forgotButton.setOnClickListener {
            val intent = Intent(this, ForgotPassword::class.java)
            startActivity(intent)
            finish()
        }

        loginButton.setOnClickListener {
            startBioAuth()
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, registrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkEnrollmentStatus(uid: String) {
        val userDocRef = firestore.collection("employeeDetails").document(uid)

        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val isEnrolled = documentSnapshot.getBoolean("isEnrolled") ?: false

                if (isEnrolled) {
                    val intent = Intent(this, homeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, enrollmentActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                Toast.makeText(this, "User not found in database", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error checking enrollment status: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logUser(){
        val usernameEditText = findViewById<EditText>(R.id.editTextText5)
        val passwordEditText = findViewById<EditText>(R.id.editTextTextPassword5)
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Both username and password are required", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(username, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.uid?.let { uid ->
                            val verify = auth.currentUser?.isEmailVerified
                            if(verify == true){
                                checkEnrollmentStatus(uid)
                                showToast("We are logging you in")
                            }
                            else{
                                showToast("Please verify your Email")
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
            this.setTitle("Use your uniqueness to enter the app")
        }

        prompt.build().authenticate(object : BiometricPromptCompat.AuthenticationCallback() {
            override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
                super.onSucceeded(confirmed)
                logUser()
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
