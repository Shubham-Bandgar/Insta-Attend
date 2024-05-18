package com.insta.ams

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class loginActivity : AppCompatActivity() {

    private lateinit var phoneNumberEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var sendOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber)
        otpEditText = findViewById(R.id.editTextOtp)
        sendOtpButton = findViewById(R.id.buttonSendOtp)
        verifyOtpButton = findViewById(R.id.buttonVerifyOtp)
        progressBar = findViewById(R.id.progressBar)

        sendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()
            if (TextUtils.isEmpty(phoneNumber) || !isValidPhoneNumber(phoneNumber)) {
                Toast.makeText(this@loginActivity, "Enter a valid phone number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkPhoneNumberRegistered(phoneNumber)
        }

        verifyOtpButton.setOnClickListener {
            val otp = otpEditText.text.toString().trim()
            if (TextUtils.isEmpty(otp)) {
                Toast.makeText(this@loginActivity, "Enter OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOtp(otp)
        }
    }

    private fun checkPhoneNumberRegistered(phoneNumber: String) {
        progressBar.visibility = View.VISIBLE
        firestore.collection("employeeDetails")
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    if (querySnapshot != null && !querySnapshot.isEmpty) {
                        sendOtp(phoneNumber)
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@loginActivity, "Phone number not registered.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@loginActivity, "Error checking phone number: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendOtp(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(phoneAuthCredential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@loginActivity, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    progressBar.visibility = View.GONE
                    this@loginActivity.verificationId = verificationId
                    Toast.makeText(this@loginActivity, "OTP sent!", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(otp: String) {
        progressBar.visibility = View.VISIBLE
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val uid = task.result?.user?.uid
                    uid?.let { checkEnrollmentStatus(it) }
                } else {
                    progressBar.visibility = View.GONE
                    val authException = task.exception as? FirebaseAuthException
                    if (authException != null) {
                        Toast.makeText(this@loginActivity, "Error: ${authException.errorCode}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@loginActivity, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun checkEnrollmentStatus(uid: String) {
        firestore.collection("employeeDetails").document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                progressBar.visibility = View.GONE
                if (documentSnapshot.exists()) {
                    startActivity(Intent(this@loginActivity, homeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@loginActivity, "User not found in database", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this@loginActivity, "Error checking enrollment status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return Pattern.compile("^\\d{10}$").matcher(phoneNumber).matches()
    }
}
