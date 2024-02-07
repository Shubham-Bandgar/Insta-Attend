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

class checkInActivity : AppCompatActivity() {

    private lateinit var locationClient: FusedLocationProviderClient

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)

        val auth = FirebaseAuth.getInstance()
        val currentTimeTextView: TextView = findViewById(R.id.currentTimeTextView)
        val authButton: Button = findViewById(R.id.authButton)

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

    private fun fetchLocationAndSaveAttendance(uid: String?, activityType: String) {
        fetchLocation { location ->
            if (location != null) {
                saveAttendance(uid, activityType, location)
            } else {
                showToast("Location is null")
            }
        }
    }

    private fun fetchLocation(locationCallback: (location: String?) -> Unit) {
        if (hasLocationPermissions()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val locationString = "$latitude, $longitude"
                        locationCallback.invoke(locationString)
                    } else {
                        locationCallback.invoke(null)
                    }
                }
                .addOnFailureListener {
                    showToast("Failed to fetch location")
                    locationCallback.invoke(null)
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

    private fun saveAttendance(
        uid: String?,
        activityType: String,
        location: String?,
        checkOutTime: String? = null,
        checkOutLocation: String? = null
    ) {
        fetchEmployeeName(uid) { employeeName ->
            if (employeeName != null) {
                val currentDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime =
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                val db = FirebaseFirestore.getInstance()

                val attendanceDetails = hashMapOf(
                    "Employee Name" to employeeName,
                    "Date" to currentDate,
                    "Status" to activityType,
                    "Check-In Time" to currentTime,
                    "Check-In Location" to location,
                    "Check-Out Time" to "",
                    "Check-Out Location" to ""
                )
                if (activityType == "Check-Out") {
                    attendanceDetails["Check-Out Time"] = checkOutTime ?: currentTime
                    attendanceDetails["Check-Out Location"] = checkOutLocation ?: location
                }

                db.collection("attendance")
                    .add(attendanceDetails)
                    .addOnSuccessListener {
                        showToast("$activityType successful")
                        val intent = Intent(this, homeActivity::class.java)
                        startActivity(intent)

                    }
                    .addOnFailureListener {
                        showToast("Failed to record $activityType details")
                    }
            } else {
                showToast("Failed to fetch employee name")
            }
        }
    }

    private fun fetchEmployeeName(uid: String?, callback: (String?) -> Unit) {
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
            this.setTitle("Use your uniqueness to Check-In")
        }

        prompt.build().authenticate(object : BiometricPromptCompat.AuthenticationCallback() {
            override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
                super.onSucceeded(confirmed)
                if (isLocationEnabled()) {
                    val auth = FirebaseAuth.getInstance()
                    val id = auth.currentUser?.uid
                    fetchLocationAndSaveAttendance(id, "Check-in")
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
