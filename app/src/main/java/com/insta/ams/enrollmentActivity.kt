package com.insta.ams
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class enrollmentActivity : AppCompatActivity() {

    val auth = FirebaseAuth.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)
        val exitButton: Button = findViewById(R.id.extBtn)
        val logOutButton: Button = findViewById(R.id.logOutBtn)


        exitButton.setOnClickListener {
            exitApp()
        }

        logOutButton.setOnClickListener {
            logoutUser()
        }
    }
    private fun exitApp() {
        finishAffinity()
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(this, loginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
