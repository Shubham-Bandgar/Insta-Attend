package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class preLogActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prelog)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val buttonNavigate = findViewById<Button>(R.id.Login)
        val buttonNavig = findViewById<Button>(R.id.Register)

        buttonNavigate.setOnClickListener {
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
        }

        buttonNavig.setOnClickListener {
            val intent = Intent(this, registrationActivity::class.java)
            startActivity(intent)
        }
        if (auth.currentUser != null) {
            val uid = auth.currentUser!!.uid
            checkEnrollmentStatus(uid)
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
                showToast("User not found in database")
            }
        }.addOnFailureListener { e ->
            showToast("Error checking enrollment status: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        // Implement showToast method as needed
    }
}
