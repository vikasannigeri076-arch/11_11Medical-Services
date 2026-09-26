package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

class CustomerLoginActivity : AppCompatActivity() {

    private lateinit var phoneInput: TextInputEditText
    private lateinit var otpInput: TextInputEditText
    private lateinit var otpLayout: TextInputLayout
    private lateinit var btnSendOtp: Button
    private lateinit var btnVerifyOtp: Button
    private lateinit var signupText: TextView

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        phoneInput = findViewById(R.id.phoneInput)
        otpInput = findViewById(R.id.otpInput)
        otpLayout = findViewById(R.id.otpLayout)
        btnSendOtp = findViewById(R.id.btnotp)
        btnVerifyOtp = findViewById(R.id.verifyOtpBtn)
        signupText = findViewById(R.id.signupText)

        otpLayout.visibility = View.GONE
        btnVerifyOtp.visibility = View.GONE

        signupText.setOnClickListener {
            startActivity(Intent(this, CustomerSignupActivity::class.java))
        }

        btnSendOtp.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.length == 10) {
                checkUserExistsAndSendOtp(phone)
            } else {
                Toast.makeText(this, "Enter valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerifyOtp.setOnClickListener {
            val code = otpInput.text.toString().trim()
            if (code.length == 6 && verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                signInWithCredential(credential)
            } else {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserExistsAndSendOtp(phone: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("Customers")
        usersRef.child(phone).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Fetch the stored OTP for display (Mocking the display of fixed/database OTP)
                    val storedOtp = snapshot.child("otp").getValue(String::class.java) ?: "123456"
                    
                    // Show OTP Pop-up with the actual stored value
                    AlertDialog.Builder(this@CustomerLoginActivity)
                        .setTitle("OTP Received")
                        .setMessage("Your verification code is: $storedOtp")
                        .setPositiveButton("OK") { _, _ ->
                            otpInput.setText(storedOtp) // Auto-fill for ease
                        }
                        .show()
                        
                    sendVerificationCode("+91$phone")
                } else {
                    Toast.makeText(this@CustomerLoginActivity, "Account not found. Please Sign Up first.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@CustomerLoginActivity, CustomerSignupActivity::class.java))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CustomerLoginActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this@CustomerLoginActivity, "Verification Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    otpLayout.visibility = View.VISIBLE
                    btnVerifyOtp.visibility = View.VISIBLE
                    startResendTimer()
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
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, CustomerHomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
