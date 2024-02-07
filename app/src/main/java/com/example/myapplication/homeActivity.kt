package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class homeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val buttonCheckIn = findViewById<Button>(R.id.button5)
        val buttonCheckOut = findViewById<Button>(R.id.button6)
        val buttonLogout = findViewById<Button>(R.id.btnLogout)

        buttonCheckIn.setOnClickListener{
            checkIn()
        }

        buttonCheckOut.setOnClickListener {
            checkOut()
        }

        fetchEmployeeDetails()

        buttonLogout.setOnClickListener {
            logoutUser()
        }
    }

    private fun fetchEmployeeDetails() {
        val employeeNameTextView = findViewById<TextView>(R.id.employeeNameTextView)
        val employeeEmailTextView = findViewById<TextView>(R.id.employeeEmailTextView)
        val employeePhoneTextView = findViewById<TextView>(R.id.employeePhoneTextView)

        val uid = auth.currentUser?.uid

        if (uid != null) {
            firestore.collection("employeeDetails").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val employeeName = document.getString("username")
                        val employeeEmail = document.getString("email")
                        val employeePhone = document.getString("phoneNumber")

                        employeeNameTextView.text = "Employee Name: $employeeName"
                        employeeEmailTextView.text = "Employee Email: $employeeEmail"
                        employeePhoneTextView.text = "Employee Phone NO: $employeePhone"
                    } else {
                        employeeNameTextView.text = "Employee Name: Not Found"
                        employeeEmailTextView.text = "Employee Email: Not Found"
                        employeePhoneTextView.text = "Employee Phone NO: Not Found"
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(this, loginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkIn(){
        val intent = Intent(this, checkInActivity::class.java)
        startActivity(intent)
    }
    private fun checkOut(){
        val intent = Intent(this, checkOutActivity::class.java)
        startActivity(intent)
    }
}




