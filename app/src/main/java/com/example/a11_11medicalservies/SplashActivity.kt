package com.example.a11_11medicalservies

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SplashActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        videoView = findViewById(R.id.splashVideo)

        try {
            val video = Uri.parse("android.resource://" + packageName + "/" + R.raw.splash_screenvideo)
            videoView.setVideoURI(video)
            videoView.start()
        } catch (e: Exception) {
            // If video is missing, go directly to next screen
            checkAuthAndNavigate()
        }

        videoView.setOnCompletionListener {
            checkAuthAndNavigate()
        }

        val button = findViewById<Button>(R.id.btnskip)
        button.setOnClickListener {
            checkAuthAndNavigate()
        }
    }

    private fun checkAuthAndNavigate() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val phone = currentUser.phoneNumber?.replace("+91", "")
            if (phone != null) {
                // Check if user is a Customer
                FirebaseDatabase.getInstance().getReference("Customers").child(phone)
                    .get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            startActivity(Intent(this, CustomerHomeActivity::class.java))
                            finish()
                        } else {
                            // If not a Customer, check if user is a Shop
                            FirebaseDatabase.getInstance().getReference("Shops").child(phone)
                                .get().addOnSuccessListener { shopSnapshot ->
                                    if (shopSnapshot.exists()) {
                                        startActivity(Intent(this, ShopHomeActivity::class.java))
                                        finish()
                                    } else {
                                        // If not found in either, go to selection screen
                                        auth.signOut()
                                        goToSelectionScreen()
                                    }
                                }.addOnFailureListener {
                                    goToSelectionScreen()
                                }
                        }
                    }.addOnFailureListener {
                        goToSelectionScreen()
                    }
            } else {
                goToSelectionScreen()
            }
        } else {
            goToSelectionScreen()
        }
    }

    private fun goToSelectionScreen() {
        startActivity(Intent(this, SelectUserActivity::class.java))
        finish()
    }
}
