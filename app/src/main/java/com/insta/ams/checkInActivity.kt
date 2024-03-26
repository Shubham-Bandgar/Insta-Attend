package com.insta.ams

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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

/********** Global variables which are accessible by all over the project ******/
var circleName: String? = null
var plusCode: String? = null
var fencing: Boolean? = null
val authCred = FirebaseAuth.getInstance()
class checkInActivity : AppCompatActivity() {

    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var geoCoder : Geocoder


    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)
        fetchCircleField()
        val currentTimeTextView: TextView = findViewById(R.id.currentTimeTextView)
        val authButton: Button = findViewById(R.id.authButton)
        val image = findViewById<ImageView>(R.id.imageView2)
        image.setOnClickListener{
            val intent = Intent(this, homeActivity::class.java)
            startActivity(intent)
            finish()
        }
        val floatingActionButton = findViewById<FloatingActionButton>(R.id.backtohome)

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, homeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }


        fetchGeoLocation(authCred.currentUser?.uid){ geoFencing->
           fencing = geoFencing
        }

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
        geoCoder = Geocoder(this, Locale.getDefault())
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
                        val latitude: Double = location.latitude
                        val longitude: Double = location.longitude
                        val locationString : List<Address>? =
                            geoCoder.getFromLocation(latitude, longitude, 1)
                        val add : String = locationString?.get(0)?.getAddressLine(0) ?:""
                        if (fencing == true){
                            if(add.substring(0,8).equals(plusCode)){
                                locationCallback.invoke(add)
                            }
                            else{
                                showToast("You are not in office")
                                locationCallback.invoke(null)
                            }
                        }
                        else{
                            locationCallback.invoke(add)
                        }
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
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                val db = FirebaseFirestore.getInstance()

                val attendanceDetails = hashMapOf(
                    "Employee Name" to employeeName,
                    "Date" to currentDate,
                    "CheckIn_Time" to currentTime,
                    "Check-In Location" to location,
                    "CheckOut_Time" to "",
                    "Check-Out Location" to "",
                    "Duration" to "00:00",
                    "Status" to "Absent",
                )

                val attendanceRef = db.collection("attendance")
                    .whereEqualTo("Date", currentDate)
                    .whereEqualTo("Employee Name", employeeName)

                attendanceRef.get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents[0]
                            val attendanceId = document.id
                            val updateData = hashMapOf<String, Any?>(
                                "CheckIn_Time" to currentTime,
                                "Check-In Location" to location
                            )

                            if (activityType == "Check-Out") {
                                updateData["CheckOut_Time"] = checkOutTime ?: currentTime
                                updateData["Check-Out Location"] = checkOutLocation ?: location
                            }

                            db.collection("attendance")
                                .document(attendanceId)
                                .update(updateData)
                                .addOnSuccessListener {
                                    showToast("$activityType updated successfully")
                                    val intent = Intent(this, LoadingActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener {
                                    showToast("Failed to update $activityType details")
                                }
                        } else {
                            // No existing attendance entry for today, add a new one
                            addNewAttendance(activityType, attendanceDetails)
                        }
                    }
                    .addOnFailureListener {
                        showToast("Failed to check existing attendance")
                    }
            } else {
                showToast("Failed to fetch employee name")
            }
        }
    }

    private fun addNewAttendance(activityType: String, attendanceDetails: HashMap<String, String?>) {
        val db = FirebaseFirestore.getInstance()
        db.collection("attendance")
            .add(attendanceDetails)
            .addOnSuccessListener {
                showToast("$activityType successful")
                val intent = Intent(this, LoadingActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                showToast("Failed to record $activityType details")
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

    private fun fetchGeoLocation(uid: String?, callback: (Boolean?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("employeeDetails")
            .document(uid!!)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val fencing = documentSnapshot.get("geoFencing")
                    callback.invoke(fencing as Boolean?)
                } else {
                    showToast("User does not exist")
                    callback.invoke(null)
                }
            }
            .addOnFailureListener {
                showToast("Failed to fetch detail")
                callback.invoke(null)
            }
    }

    private fun fetchCircleField() {
        val auth = FirebaseAuth.getInstance()
        val id = auth.currentUser?.uid
        if (id != null) {
            fetchCircleName(id) { fetchedCircleName ->
                if (fetchedCircleName != null) {
                    circleName = fetchedCircleName
                    fetchPlusCode(circleName) { fetchedPlusCode ->
                        if (fetchedPlusCode != null) {
                            plusCode = fetchedPlusCode
                        } else {
                            showToast("Failed to fetch Pluscode")
                        }
                    }
                } else {
                    showToast("Failed to fetch Circle")
                }
            }
        } else {
            showToast("User not authenticated")
        }
    }

    // Function to fetch "Circle" field from "employeeDetails" collection
    private fun fetchCircleName(uid: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("employeeDetails")
            .document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val circle = documentSnapshot.getString("Circle")
                    callback.invoke(circle)
                } else {
                    showToast("User document does not exist")
                    callback.invoke(null)
                }
            }
            .addOnFailureListener {
                showToast("Failed to fetch Circle")
                callback.invoke(null)
            }
    }

    private fun fetchPlusCode(circleName: String?, callback: (String?) -> Unit) {
        if (circleName != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Circle")
                .document(circleName)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val plusCodeValue = documentSnapshot.getString("Pluscode")
                        callback.invoke(plusCodeValue)
                    } else {
                        showToast("Circle document does not exist")
                        callback.invoke(null)
                    }
                }
                .addOnFailureListener {
                    showToast("Failed to fetch Pluscode")
                    callback.invoke(null)
                }
        } else {
            showToast("Circle name is null")
            callback.invoke(null)
        }
    }


}