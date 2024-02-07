package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import java.text.SimpleDateFormat
import java.util.*

class checkOutActivity : AppCompatActivity() {

    private lateinit var locationClient: FusedLocationProviderClient

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_out)

        val auth = FirebaseAuth.getInstance()
        val currentTimeTextView: TextView = findViewById(R.id.currentTimeTextViewco)
        val authButton: Button = findViewById(R.id.authButtonco)

        locationClient = LocationServices.getFusedLocationProviderClient(this)

        val currentTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        dateFormat.timeZone = currentTimeZone
        val currentTime = dateFormat.format(Date())
        currentTimeTextView.text = "Current Time: $currentTime"

        authButton.setOnClickListener {
            startBioAuth()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Location services are not enabled. Please enable them to proceed.")
            .setCancelable(false)
            .setPositiveButton("Enable Now") { dialog, id ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun fetchLocation(locationCallback: (String?) -> Unit) {
        if (hasLocationPermissions()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                showToast("Location permissions not granted")
            } else {
                locationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            val latitude = location.latitude
                            val longitude = location.longitude
                            val locationString = "$latitude, $longitude"
                            locationCallback.invoke(locationString)
                        } else {
                            showToast("Failed to fetch location")
                            locationCallback.invoke(null)
                        }
                    }
                    .addOnFailureListener {
                        showToast("Failed to fetch location")
                        locationCallback.invoke(null)
                    }
            }
        } else {
            showToast("Location permissions not granted")
            locationCallback.invoke(null)
        }
    }


    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateCheckOutDetails(uid: String?, checkOutLocation: String?, checkOutTime: String?) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        fetchUsername(uid) { username ->
            if (username != null) {
                val db = FirebaseFirestore.getInstance()

                // Update checkout_time and checkout_location for today's date and username
                db.collection("attendance")
                    .whereEqualTo("Date", currentDate)
                    .whereEqualTo("Employee Name", username).whereEqualTo("Check-out Time", "")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            val attendanceId = document.id
                            val updateData = mutableMapOf<String, Any?>()
                            updateData["Check-Out Time"] = checkOutTime
                            if (checkOutLocation != null) {
                                updateData["Check-Out Location"] = checkOutLocation
                            }

                            db.collection("attendance")
                                .document(attendanceId)
                                .update(updateData)
                                .addOnSuccessListener {
                                    showToast("Check-Out successful")
                                    val intent = Intent(this, homeActivity::class.java)
                                    startActivity(intent)
                                }
                                .addOnFailureListener {
                                    showToast("Failed to update Check-Out details")
                                }
                        }
                    }
                    .addOnFailureListener {
                        showToast("Failed to query attendance records")
                    }
            } else {
                showToast("Failed to fetch username")
            }
        }
    }


    private fun fetchUsername(uid: String?, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("employeeDetails")
            .document(uid!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val username = documentSnapshot.getString("username")
                    callback.invoke(username)
                } else {
                    showToast("User document does not exist")
                    callback.invoke(null)
                }
            }
            .addOnFailureListener {
                showToast("Failed to fetch username")
                callback.invoke(null)
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentTime(): String {
        val currentTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        dateFormat.timeZone = currentTimeZone
        return dateFormat.format(Date())
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
            this.setTitle("Use your uniqueness to Check-Out")
        }

        prompt.build().authenticate(object : BiometricPromptCompat.AuthenticationCallback() {
            override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
                super.onSucceeded(confirmed)
                if (isLocationEnabled()) {
                    val auth = FirebaseAuth.getInstance()
                    val id = auth.currentUser?.uid

                    fetchLocation { location ->
                        val checkOutTime = getCurrentTime()

                        if (id != null && location != null) {
                            updateCheckOutDetails(id, location, checkOutTime)
                        } else {
                            showToast("Failed to get user ID or location")
                        }
                    }
                } else {
                    showLocationAlertDialog()
                }
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
}
