package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity4 : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main4)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val buttonNavigate = findViewById<Button>(R.id.button5)
        buttonNavigate.setOnClickListener {
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

                            // Creating COllection of employeeDetails
                            val employeeDetails = hashMapOf(
                                "username" to username,
                                "email" to email,
                                "phoneNumber" to phoneNumber
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
                                        val intent = Intent(this, MainActivity2::class.java)
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
    }
}
