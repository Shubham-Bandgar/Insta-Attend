package com.insta.ams

import CircleAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import dev.skomlach.biometric.compat.AuthenticationFailureReason
import dev.skomlach.biometric.compat.AuthenticationResult
import dev.skomlach.biometric.compat.BiometricApi
import dev.skomlach.biometric.compat.BiometricAuthRequest
import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricManagerCompat
import dev.skomlach.biometric.compat.BiometricPromptCompat
import dev.skomlach.biometric.compat.BiometricType
import java.util.regex.Pattern

class registrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var selectedItem : String
    private var circleNames = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val spinner = findViewById<Spinner>(R.id.spinner)
        spinner.prompt = "Select Circle"
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedItem = parent.getItemAtPosition(position).toString()

            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                showToast("Please Select your employment type")
            }
        }

        //Hint for password pattern
        val editText = findViewById<EditText>(R.id.editTextTextPassword5)
        val hintTextView = findViewById<TextView>(R.id.hintTextView)
        editText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // When the EditText gains focus, show the hint below it
                hintTextView.visibility = View.VISIBLE
            } else {
                // When the EditText loses focus, hide the hint
                hintTextView.visibility = View.GONE
                }

            }

        val floatingActionButton = findViewById<FloatingActionButton>(R.id.backtohome)

        floatingActionButton.setOnClickListener {
            val intent = Intent(this, preLogActivity::class.java)
            startActivity(intent)
        }

        //logo image
        val imageView=findViewById<ImageView>(R.id.imageView2)
        imageView.setOnClickListener {
            val intent=Intent(this,preLogActivity::class.java)
            startActivity(intent)
            }



        val txtButton = findViewById<TextView>(R.id.textView3)
        txtButton.setOnClickListener {
            val intent = Intent(this, loginActivity::class.java)
            startActivity(intent)
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val buttonNavigate = findViewById<Button>(R.id.button5)
        buttonNavigate.setOnClickListener {
            //validations
            val usernameEditText = findViewById<EditText>(R.id.editTextText5)
            val emailEditText = findViewById<EditText>(R.id.editTextTextEmailAddress4)
            val phoneNumberEditText: EditText = findViewById(R.id.phoneNumberEditText)
            val enteredPhoneNumber: String = phoneNumberEditText.text.toString().trim()
            if(usernameEditText==null){
                Toast.makeText(this,"Please Enter Your Name",Toast.LENGTH_LONG).show()
            }

            if(emailEditText==null){
                Toast.makeText(this,"Please Enter Email",Toast.LENGTH_LONG).show()
            }

            // Validate the phone number
            if (isValidPhoneNumber(enteredPhoneNumber)) {
                // Valid phone number format with 10 digits
            } else {
                // Invalid phone number format
                Toast.makeText(this, "Invalid phone number. Please enter a 10-digit number", Toast.LENGTH_SHORT).show()
            }

            //validate password
            val passwordEditText: EditText = findViewById(R.id.editTextTextPassword5)

            // Get the entered values
            val password: String = passwordEditText.text.toString().trim()

            // Validate the password
            if (!isValidPassword(password)) {
                Toast.makeText(this, "Invalid password format.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            //bioAuthentication
            startBioAuth()
            }

        fetchCircleData()
    }

    private fun fetchCircleData() {
        val spinner = findViewById<Spinner>(R.id.spinner)
        spinner.prompt = "Select Circle" // Set the default prompt

        val circleCollection = FirebaseFirestore.getInstance().collection("Circle")
        circleCollection.get()
            .addOnSuccessListener { result ->
                circleNames.clear()
                circleNames.add("Select Circle")

                for (document in result) {
                    val circleName = document.id
                    circleNames.add(circleName)
                }

                val adapter = CircleAdapter(this, android.R.layout.simple_spinner_item, circleNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
            .addOnFailureListener { exception ->
                showToast("Error fetching Circle data: $exception")
            }
    }





    private fun registerUser() {
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
                        auth.currentUser?.sendEmailVerification()
                            ?.addOnSuccessListener {
                                val user = auth.currentUser
                                val uid = user?.uid
                                val employeeDetails = hashMapOf(
                                    "username" to username,
                                    "email" to email,
                                    "phoneNumber" to phoneNumber,
                                    "Circle" to selectedItem,
                                    "isEnrolled" to false,
                                    "geoFencing" to false,
                                    "deletePermission" to null
                                )

                                if (uid != null) {
                                    firestore.collection("employeeDetails")
                                        .document(uid)
                                        .set(employeeDetails)
                                        .addOnSuccessListener {
                                            val registerProgressBar = findViewById<ProgressBar>(R.id.registerProgressbar)
                                            registerProgressBar.visibility = View.GONE
                                            showSuccessDialog()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                this,
                                                "Failed to store employee details",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    showToast(
                                        "Registration failed: ${task.exception?.message}"
                                    )
                                }
                            }
                            ?.addOnFailureListener {
                                showToast(it.toString())
                                showToast("Invalid Email")
                            }
                    }
                    else {
                        // Handle Firebase authentication errors
                        val errorMessage = task.exception?.message

                        when (task.exception) {
                            is FirebaseAuthUserCollisionException -> {
                                val registerProgressBar = findViewById<ProgressBar>(R.id.registerProgressbar)
                                registerProgressBar.visibility = View.GONE
                                showToast("User with this email already exists. Please log in.")
                            }
                            else -> {
                                showToast("Registration failed: $errorMessage")
                            }
                        }
                    }
                }
        } else {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
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
            this.setTitle("Register your biometric")
        }

        prompt.build().authenticate(object : BiometricPromptCompat.AuthenticationCallback() {
            override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
                super.onSucceeded(confirmed)
                val registerProgressBar = findViewById<ProgressBar>(R.id.registerProgressbar)
                registerProgressBar.visibility = View.VISIBLE
                registerUser()
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

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Remove non-digit characters
        val cleanPhoneNumber = phoneNumber.replace("[^\\d]".toRegex(), "")

        // Check if the cleaned phone number has exactly 10 digits
        return cleanPhoneNumber.length == 10
    }

    // Function to validate password with specific pattern
    private fun isValidPassword(password: String): Boolean {
        val PASSWORD_PATTERN: Pattern = Pattern.compile(
            "^" +
                    "(?=.*[0-9])" +         // at least 1 digit
                    "(?=.*[a-zA-Z])" +      // at least 1 letter
                    "(?=.*[@#$%^&+=])" +    // at least 1 special character
                    "(?=\\S+$)" +           // no white spaces
                    ".{6,}" +               // at least 8 characters
                    "$"
        )
        return PASSWORD_PATTERN.matcher(password).matches()
    }
    //showSuccessDialog
    private fun showSuccessDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registration Successful")
            .setMessage("Please Check Your email for verification link")
            .setCancelable(true)
            .setPositiveButton("OK") { dialogInterface,it->
                val intent = Intent(this, loginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .create()
        .show()
        }
}
