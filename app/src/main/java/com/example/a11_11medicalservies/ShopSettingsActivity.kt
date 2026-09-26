package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ShopSettingsActivity : AppCompatActivity() {

    private lateinit var shopStatusSwitch: SwitchCompat
    private lateinit var tvShopStatusDesc: TextView
    private var shopPhone: String = ""
    private val db = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_settings)

        shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_settings

        shopStatusSwitch = findViewById(R.id.switchShopStatus)
        tvShopStatusDesc = findViewById(R.id.tvShopStatusDesc)

        setupBottomNavigation(bottomNavigation)
        fetchShopStatus()

        shopStatusSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateShopStatus(isChecked)
        }

        // 1. Shop Profile Click
        findViewById<View>(R.id.layoutShopProfile).setOnClickListener {
            startActivity(Intent(this, ShopProfileActivity::class.java))
        }

        // 2. Bank / UPI Details Click
        findViewById<View>(R.id.layoutBankDetails).setOnClickListener {
            startActivity(Intent(this, BankDetailsActivity::class.java))
        }

        // 3. Language Click
        findViewById<View>(R.id.layoutShopLanguage).setOnClickListener {
            startActivity(Intent(this, LanguageSelectionActivity::class.java).apply {
                putExtra("userType", "Shop")
                putExtra("isUpdate", true)
            })
        }

        // 4. Help & Support Click
        findViewById<View>(R.id.layoutShopHelp).setOnClickListener {
            showHelpOptions()
        }

        // 5. Logout Click
        findViewById<View>(R.id.layoutShopLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupBottomNavigation(nav: BottomNavigationView) {
        nav.setOnItemSelectedListener { item ->
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
                R.id.nav_money -> {
                    startActivity(Intent(this, ShopCodPaymentActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun fetchShopStatus() {
        if (shopPhone.isEmpty()) return
        db.getReference("Shops").child(shopPhone).child("status")
            .get().addOnSuccessListener { snapshot ->
                val status = snapshot.getValue(String::class.java) ?: "Closed"
                shopStatusSwitch.isChecked = status == "Open"
                updateStatusText(shopStatusSwitch.isChecked)
            }
    }

    private fun updateShopStatus(isOpen: Boolean) {
        if (shopPhone.isEmpty()) return
        val status = if (isOpen) "Open" else "Closed"
        db.getReference("Shops").child(shopPhone).child("status").setValue(status)
            .addOnSuccessListener {
                updateStatusText(isOpen)
                val msg = if (isOpen) "Shop is now OPEN for requests" else "Shop is now CLOSED"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStatusText(isOpen: Boolean) {
        tvShopStatusDesc.text = if (isOpen) "Status: OPEN (Receiving Requests)" else "Status: CLOSED (Not Receiving Requests)"
        tvShopStatusDesc.setTextColor(if (isOpen) resources.getColor(android.R.color.holo_green_dark) else resources.getColor(android.R.color.holo_red_dark))
    }

    private fun showHelpOptions() {
        val options = arrayOf("Call Support", "WhatsApp Support", "Email Support")
        AlertDialog.Builder(this)
            .setTitle("Contact Help & Support")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Calling support...", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Opening WhatsApp...", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Opening Email...", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout from your shop account?")
            .setPositiveButton("Logout") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, ShopLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}