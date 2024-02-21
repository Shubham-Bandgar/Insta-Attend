package com.example.myapplication
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        val anim = findViewById<LottieAnimationView>(R.id.done)

        Handler(Looper.getMainLooper()).postDelayed({
            anim.visibility = View.VISIBLE
            anim.playAnimation()
        }, 100)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, homeActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}