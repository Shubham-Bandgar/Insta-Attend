package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class checkOutActivity : AppCompatActivity() {
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var geoCoder: Geocoder

    companion object {
        private const val MY_PERMISSIONS_REQUEST_LOCATION = 1
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_out)
        val currentTimeTextView: TextView = findViewById(R.id.currentTimeTextViewco)
        val authButton: Button = findViewById(R.id.authButtonco)
        val currentTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        dateFormat.timeZone = currentTimeZone
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        currentTimeTextView.text = "Current Time: $currentTime"

        /********************************* Click Listener 30-01-2024 ******************************************/

        authButton.setOnClickListener {
            startBioAuth()
        }
    }

    /************************** Location Enable Check 30-01-2024 ************************************************/

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /*************************** Location alert Dialogue 30-01-2024 *********************************/

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

    /********** Function to fetch location (Alternative : We can use Geocoder Function) 30-01-2024 ************************************/

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
                        geoCoder = Geocoder(this, Locale.getDefault())
                        if (location != null) {
                            val latitude: Double = location.latitude
                            val longitude: Double = location.longitude
                            val locationString : List<Address>? =
                                geoCoder.getFromLocation(latitude, longitude, 1)
                            val add : String = locationString?.get(0)?.getAddressLine(0) ?:""
                            locationCallback.invoke(add)
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

    /******************************  Location permission check 30-01-2024  ***************************************/


    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*************** Change updateCheckOut Function to add Status of presentee and Duration Fields 09-02-2024 ************/

    private fun updateCheckOutDetails(uid: String?, checkOutLocation: String?, checkOutTime: String?) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        fetchUsername(uid) { username ->
            if (username != null) {
                val db = FirebaseFirestore.getInstance()
                db.collection("attendance")
                    .whereEqualTo("Date", currentDate)
                    .whereEqualTo("Employee Name", username)
                    .whereEqualTo("Check-Out Time", "")
                    .whereEqualTo("Duration", "")
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents) {
                            val attendanceId = document.id
                            val checkInTime = document.getString("Check-In Time")

                            if (checkInTime != null) {
                                val duration = calculateDuration(checkInTime, checkOutTime)
                                val updateData = mutableMapOf<String, Any?>()
                                val status = calculateStatus(duration)
                                updateData["Check-Out Time"] = checkOutTime
                                updateData["Duration"] = duration
                                updateData["Status"] = status

                                if (checkOutLocation != null) {
                                    updateData["Check-Out Location"] = checkOutLocation
                                }

                                db.collection("attendance")
                                    .document(attendanceId)
                                    .update(updateData)
                                    .addOnSuccessListener {
                                        showToast("Check-Out successful")
                                        val intent = Intent(this, LoadingActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        showToast("Failed to update Check-Out details")
                                    }
                            } else {
                                showToast("Check-In Time not found")
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

    /********************** Created Function to Decide Status Based on Duration 09-02-2024 *********************************/

    private fun calculateStatus(duration: String): String {

        val totalMinutes = duration.split(":")[0].toInt() * 60 + duration.split(":")[1].toInt()

        return when {
            totalMinutes > 390 -> "Present"
            totalMinutes in 270..390 -> "Half Day"
            else -> "Absent"
        }
    }


    /**************************** Created Function to calculate Duration 09-02-2024 ***************************************/

    private fun calculateDuration(checkInTime: String, checkOutTime: String?): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startDate = dateFormat.parse(checkInTime)
        val endDate = dateFormat.parse(checkOutTime)

        val diff = endDate.time - startDate.time
        val hours = diff / (60 * 60 * 1000)
        val minutes = (diff % (60 * 60 * 1000)) / (60 * 1000)

        return String.format("%02d:%02d", hours, minutes)
    }

    /*********** Created Function to fetch username from employeeDetails Collection to use as Condition in update query **************/

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

    /*********************************Created function to show toast message 02-02-2024 **************************************/

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /*********************************** Created Function to get current time 02-02-2024 ****************************************/

    private fun getCurrentTime(): String {
        val currentTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        dateFormat.timeZone = currentTimeZone
        return dateFormat.format(Date())
    }

    /********************************* Function to Execute Local Authentication 05-02-2024 *****************************************/

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
