package com.example.a11_11medicalservies

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BankDetailsActivity : AppCompatActivity() {

    private lateinit var etUpiId: EditText
    private lateinit var etBankName: EditText
    private lateinit var etAccNumber: EditText
    private lateinit var etIFSC: EditText
    private lateinit var btnUpdate: Button
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bank_details)

        etUpiId = findViewById(R.id.etProfileUpiId)
        etBankName = findViewById(R.id.etProfileBankName)
        etAccNumber = findViewById(R.id.etProfileAccNumber)
        etIFSC = findViewById(R.id.etProfileIFSC)
        btnUpdate = findViewById(R.id.btnUpdateBankDetails)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        fetchBankDetails()

        btnUpdate.setOnClickListener {
            updateBankDetails()
        }
    }

    private fun fetchBankDetails() {
        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: return
        db.getReference("Shops").child(phone).child("bankDetails")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        etUpiId.setText(snapshot.child("upiId").getValue(String::class.java))
                        etBankName.setText(snapshot.child("bankName").getValue(String::class.java))
                        etAccNumber.setText(snapshot.child("accNumber").getValue(String::class.java))
                        etIFSC.setText(snapshot.child("ifsc").getValue(String::class.java))
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateBankDetails() {
        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: return
        val upiId = etUpiId.text.toString().trim()
        val bankName = etBankName.text.toString().trim()
        val accNumber = etAccNumber.text.toString().trim()
        val ifsc = etIFSC.text.toString().trim()

        if (upiId.isEmpty()) {
            Toast.makeText(this, "UPI ID is required", Toast.LENGTH_SHORT).show()
            return
        }

        val bankData = mapOf(
            "upiId" to upiId,
            "bankName" to bankName,
            "accNumber" to accNumber,
            "ifsc" to ifsc
        )

        db.getReference("Shops").child(phone).child("bankDetails").setValue(bankData)
            .addOnSuccessListener {
                Toast.makeText(this, "Payment details saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save details", Toast.LENGTH_SHORT).show()
            }
    }
}