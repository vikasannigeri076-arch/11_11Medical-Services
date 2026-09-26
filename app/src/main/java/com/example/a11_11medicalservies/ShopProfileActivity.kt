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

class ShopProfileActivity : AppCompatActivity() {

    private lateinit var etShopName: EditText
    private lateinit var etOwnerName: EditText
    private lateinit var etShopPhone: EditText
    private lateinit var etGST: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnUpdate: Button
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_profile)

        etShopName = findViewById(R.id.etProfileShopName)
        etOwnerName = findViewById(R.id.etProfileOwnerName)
        etShopPhone = findViewById(R.id.etProfileShopPhone)
        etGST = findViewById(R.id.etProfileGST)
        etAddress = findViewById(R.id.etProfileShopAddress)
        btnUpdate = findViewById(R.id.btnUpdateShopProfile)

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        fetchShopProfileData()

        btnUpdate.setOnClickListener {
            updateShopProfile()
        }
    }

    private fun fetchShopProfileData() {
        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: return
        db.getReference("Shops").child(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shop = snapshot.getValue(Shop::class.java)
                    if (shop != null) {
                        etShopName.setText(shop.shopName)
                        etOwnerName.setText(shop.ownerName)
                        etShopPhone.setText(shop.mobile)
                        etGST.setText(shop.gst)
                        etAddress.setText(shop.address)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateShopProfile() {
        val shopName = etShopName.text.toString().trim()
        val ownerName = etOwnerName.text.toString().trim()
        val gst = etGST.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val phone = etShopPhone.text.toString().trim()

        if (shopName.isEmpty() || ownerName.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "shopName" to shopName,
            "ownerName" to ownerName,
            "gst" to gst,
            "address" to address
        )

        db.getReference("Shops").child(phone).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Shop profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}