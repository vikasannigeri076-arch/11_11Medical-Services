package com.example.a11_11medicalservies

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class MedicineAvailabilityActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerLocation: TextView
    private lateinit var tvMedicineList: TextView
    private lateinit var ivPrescription: ImageView
    private lateinit var tvPrescriptionTitle: TextView
    private lateinit var btnAvailable: Button
    private lateinit var btnNotAvailable: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private var orderId: String? = null
    private var shopPhone: String? = null
    private var shopName: String? = null
    private var medicineRequirements: String? = null
    private var prescriptionData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicine_availability)

        tvUserName = findViewById(R.id.tvUserName)
        tvAddress = findViewById(R.id.tvAddress)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerLocation = findViewById(R.id.tvCustomerLocation)
        tvMedicineList = findViewById(R.id.tvMedicineList)
        ivPrescription = findViewById(R.id.ivPrescription)
        tvPrescriptionTitle = findViewById(R.id.tvPrescriptionTitle)
        btnAvailable = findViewById(R.id.btnAvailable)
        btnNotAvailable = findViewById(R.id.btnNotAvailable)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.headerLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        orderId = intent.getStringExtra("orderId")
        val customerName = intent.getStringExtra("customerName")
        medicineRequirements = intent.getStringExtra("medicineRequirements")
        val lat = intent.getDoubleExtra("lat", 0.0)
        val lng = intent.getDoubleExtra("lng", 0.0)
        prescriptionData = intent.getStringExtra("prescriptionUrl")

        tvCustomerName.text = "Customer: $customerName"
        tvMedicineList.text = medicineRequirements
        updateCustomerLocation(lat, lng)

        if (!prescriptionData.isNullOrEmpty()) {
            tvPrescriptionTitle.visibility = View.VISIBLE
            ivPrescription.visibility = View.VISIBLE
            try {
                val decodedString = Base64.decode(prescriptionData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivPrescription.setImageBitmap(bitmap)
                
                ivPrescription.setOnClickListener {
                    showFullScreenImage(bitmap)
                }
            } catch (e: Exception) {}
        }

        fetchShopDetails()
        setupBottomNavigation()

        btnAvailable.setOnClickListener {
            val intent = Intent(this, ShopBillActivity::class.java).apply {
                putExtra("orderId", orderId)
                putExtra("shopPhone", shopPhone)
                putExtra("shopName", shopName)
                putExtra("medicineRequirements", medicineRequirements)
                putExtra("prescriptionUrl", prescriptionData)
            }
            startActivity(intent)
            finish()
        }

        btnNotAvailable.setOnClickListener {
            Toast.makeText(this, "Order marked as not available.", Toast.LENGTH_SHORT).show()
            // Intent to go back to ShopHomeActivity
            val intent = Intent(this, ShopHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Finish current activity
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, ShopHomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, OrderHistoryActivityShop::class.java))
                    finish()
                    true
                }
                R.id.nav_payment -> {
                    startActivity(Intent(this, ShopPaymentActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, ShopSettingsActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_money -> {
                    Toast.makeText(this, "COD Money feature coming soon", Toast.LENGTH_SHORT).show()
                    false
                }
                else -> false
            }
        }
    }

    private fun showFullScreenImage(bitmap: Bitmap) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.setContentView(imageView)
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun fetchShopDetails() {
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber?.replace("+91", "") ?: ""
        shopPhone = phone

        val shopRef = FirebaseDatabase.getInstance().getReference("Shops").child(phone)
        shopRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shop = snapshot.getValue(Shop::class.java)
                if (shop != null) {
                    tvUserName.text = "Hello, ${shop.ownerName}"
                    tvAddress.text = shop.address
                    shopName = shop.shopName
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateCustomerLocation(lat: Double, lng: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                tvCustomerLocation.text = "Location: ${addresses[0].getAddressLine(0)}"
            }
        } catch (e: IOException) {
            tvCustomerLocation.text = "Location: $lat, $lng"
        }
    }
}
