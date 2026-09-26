package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class CustomerSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_settings)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_settings

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, CustomerHomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_orders -> {
                    startActivity(Intent(this, CustomerOrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }

        // 1. Profile Click
        findViewById<View>(R.id.layoutProfile).setOnClickListener {
            startActivity(Intent(this, CustomerProfileActivity::class.java))
        }

        // 2. Language Click
        findViewById<View>(R.id.layoutLanguage).setOnClickListener {
            startActivity(Intent(this, LanguageSelectionActivity::class.java).apply {
                putExtra("userType", "Customer")
                putExtra("isUpdate", true)
            })
        }

        // 3. My Address Click
        findViewById<View>(R.id.layoutAddress).setOnClickListener {
            startActivity(Intent(this, MapPickerActivity::class.java).apply {
                putExtra("isUpdate", true)
            })
        }

        // 4. Help & Support Click
        findViewById<View>(R.id.layoutHelp).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        // 5. Logout Click
        findViewById<View>(R.id.layoutLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, CustomerLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}