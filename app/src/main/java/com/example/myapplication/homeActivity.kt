package com.example.myapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class homeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val LOCATION_PERMISSION_REQUEST_CODE=123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //floating button
        val floatingActionButton = findViewById<FloatingActionButton>(R.id.backtohome)

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
        }

        //logo image
        val imageView=findViewById<ImageView>(R.id.imageView2)
        imageView.setOnClickListener {
            val intent=Intent(this,preLogActivity::class.java)
            startActivity(intent)
        }

        if (auth.currentUser==null){
            return
        }
        if (checkLocationPermission()){
            initializeUI()
        }else{
            requestLocationPermission()
        }


    }

    private fun initializeUI(){
        val buttonCheckIn = findViewById<Button>(R.id.button5)
        val buttonCheckOut = findViewById<Button>(R.id.button6)
        val seeDetails=findViewById<TextView>(R.id.seeDetails)

        seeDetails.setOnClickListener {
            val intent=Intent(this,DetailActivity::class.java)
            startActivity(intent)
        }

        buttonCheckIn.setOnClickListener{
            checkIn()
        }

        buttonCheckOut.setOnClickListener {
            checkOut()
        }

        fetchEmployeeDetails()

    }
    private fun fetchEmployeeDetails() {

        val uid = auth.currentUser?.uid

        val employeeNameTextView = findViewById<TextView>(R.id.employeeNameTextView)
        val employeeCircleTextView = findViewById<TextView>(R.id.CircleTextView)

        if (uid != null) {
            firestore.collection("employeeDetails").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val employeeName = document.getString("username")
                        val employeeCircle = document.getString("employeeType")
                        employeeNameTextView.text = "Employee Name: $employeeName"
                        employeeCircleTextView.text = "Circle: $employeeCircle"


                    } else {
                        employeeNameTextView.text = "Employee Name: Not Found"
                        employeeCircleTextView.text = "Circle: Not Found"
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
        }

    }

    private fun checkIn(){
        val intent = Intent(this, checkInActivity::class.java)
        startActivity(intent)
    }

    private fun checkOut(){
        val intent = Intent(this, checkOutActivity::class.java)
        startActivity(intent)
    }

    private fun checkLocationPermission():Boolean{
        return ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED

    }

    private fun requestLocationPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),LOCATION_PERMISSION_REQUEST_CODE)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResult: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResult.isNotEmpty() && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                initializeUI()
            } else {
                Toast.makeText(this, "Please enable location from settings", Toast.LENGTH_LONG).show()
            }
        }
    }

}