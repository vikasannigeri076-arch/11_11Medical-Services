package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.TimeUnit

class CustomerSignupActivity : AppCompatActivity() {

    private lateinit var nameInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var otpInput: TextInputEditText
    private lateinit var otpLayout: TextInputLayout
    private lateinit var btnSendOtp: Button
    private lateinit var btnVerifyOtp: Button
    
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_signup)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        
        nameInput = findViewById(R.id.nameInput)
        phoneInput = findViewById(R.id.phoneInput)
        otpInput = findViewById(R.id.otpInput)
        otpLayout = findViewById(R.id.otpLayout)
        btnSendOtp = findViewById(R.id.btnotp)
        btnVerifyOtp = findViewById(R.id.verifyOtpBtn)

        // Initially hide OTP field and Verify button
        otpLayout.visibility = View.GONE
        btnVerifyOtp.visibility = View.GONE

        btnSendOtp.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.isNotEmpty() && phone.length == 10) {
                sendVerificationCode("+91$phone")
            } else {
                Toast.makeText(this, "Enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        btnVerifyOtp.setOnClickListener {
            val code = otpInput.text.toString().trim()
            if (code.length == 6 && verificationId != null) {
                val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
                signInWithCredential(credential)
            } else {
                Toast.makeText(this, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("Firebase", "Verification Failed", e)
                    Toast.makeText(this@CustomerSignupActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(verId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = verId
                    Toast.makeText(this@CustomerSignupActivity, "OTP Sent", Toast.LENGTH_SHORT).show()
                    
                    // Show OTP field and Verify button once code is sent
                    otpLayout.visibility = View.VISIBLE
                    btnVerifyOtp.visibility = View.VISIBLE
                    btnSendOtp.visibility = View.GONE // Optional: hide Send button
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveUserToDatabase()
                } else {
                    Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToDatabase() {
        val name = nameInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("Customers")
        val customer = Customer(name, phone)

        usersRef.child(phone).setValue(customer)
            .addOnSuccessListener {
                Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show()
                // Navigate to LanguageSelectionActivity
                val intent = Intent(this, LanguageSelectionActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}