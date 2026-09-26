package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class ShopLoginActivity : AppCompatActivity() {

    private lateinit var etShopName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etOtp: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var btnLogin: Button
    private lateinit var tvSignupShop: TextView

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_login)

        auth = FirebaseAuth.getInstance()

        etShopName = findViewById(R.id.etShopName)
        etPhone = findViewById(R.id.etPhone)
        etOtp = findViewById(R.id.etOtp)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignupShop = findViewById(R.id.signupshop)

        etOtp.visibility = View.GONE
        btnLogin.visibility = View.GONE

        tvSignupShop.setOnClickListener {
            startActivity(Intent(this, ShopSignupActivity::class.java))
        }

        btnSendOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            val shopName = etShopName.text.toString().trim()

            if (phone.length == 10 && shopName.isNotEmpty()) {
                checkShopExistsAndSendOtp(phone, shopName)
            } else {
                Toast.makeText(this, "Enter valid Shop Name and 10-digit Phone Number", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.length == 6 && verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                signInWithCredential(credential)
            } else {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkShopExistsAndSendOtp(phone: String, shopName: String) {
        val shopsRef = FirebaseDatabase.getInstance().getReference("Shops")
        shopsRef.child(phone).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val dbShopName = snapshot.child("shopName").getValue(String::class.java) ?: ""
                    
                    if (dbShopName.trim().equals(shopName.trim(), ignoreCase = true)) {
                        sendVerificationCode("+91$phone")
                    } else {
                        Toast.makeText(this@ShopLoginActivity, "Shop Name mismatch. Correct name is: $dbShopName", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@ShopLoginActivity, "Phone number not registered as a shop.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopLoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendVerificationCode(number: String) {
        btnSendOtp.isEnabled = false
        
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    btnSendOtp.isEnabled = true
                    Toast.makeText(this@ShopLoginActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    etOtp.visibility = View.VISIBLE
                    btnLogin.visibility = View.VISIBLE
                    startResendTimer()
                    Toast.makeText(this@ShopLoginActivity, "OTP Sent to $number", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun startResendTimer() {
        btnSendOtp.visibility = View.VISIBLE
        btnSendOtp.isEnabled = false
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                btnSendOtp.text = "Resend OTP in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                btnSendOtp.isEnabled = true
                btnSendOtp.text = "Send OTP"
            }
        }.start()
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    resendTimer?.cancel()
                    Toast.makeText(this, "Shop Login Successful", Toast.LENGTH_SHORT).show()
                    
                    // Force navigation to ShopHomeActivity and clear the stack
                    val intent = Intent(this, ShopHomeActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid OTP. Please check and try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
