package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase

class ShopSignupActivity : AppCompatActivity() {

    private lateinit var etShopName: EditText
    private lateinit var etOwnerName: EditText
    private lateinit var etMobile: EditText
    private lateinit var etGST: EditText
    private lateinit var actvDistrict: AutoCompleteTextView
    private lateinit var etPincode: EditText
    private lateinit var etAddress: EditText
    private lateinit var checkAgreement: CheckBox
    private lateinit var btnLocation: Button
    private lateinit var btnSignup: Button
    private lateinit var tvTermsLink: TextView

    private var shopLat: Double = 0.0
    private var shopLng: Double = 0.0

    private val districts = arrayOf(
        "Bagalkot", "Ballari (Bellary)", "Belagavi (Belgaum)", "Bengaluru (Bangalore) Rural",
        "Bengaluru (Bangalore) Urban", "Bidar", "Chamarajanagar", "Chikkaballapur",
        "Chikkamagaluru (Chikmagalur)", "Chitradurga", "Dakshina Kannada", "Davanagere",
        "Dharwad", "Gadag", "Hassan", "Haveri", "Kalaburagi (Gulbarga)", "Kodagu",
        "Kolar", "Koppal", "Mandya", "Mysuru (Mysore)", "Raichur", "Ramanagara",
        "Shivamogga (Shimoga)", "Tumakuru (Tumkur)", "Udupi", "Uttara Kannada (Karwar)",
        "Vijayapura (Bijapur)", "Yadgir", "Vijayanagara"
    )

    private val mapLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            shopLat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            shopLng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            val address = result.data?.getStringExtra("address") ?: ""
            etAddress.setText(address)
            btnLocation.text = "Location Selected ✓"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_signup)

        etShopName = findViewById(R.id.etShopName)
        etOwnerName = findViewById(R.id.etOwnerName)
        etMobile = findViewById(R.id.etMobile)
        etGST = findViewById(R.id.etGST)
        actvDistrict = findViewById(R.id.actvDistrict)
        etPincode = findViewById(R.id.etPincode)
        etAddress = findViewById(R.id.etAddress)
        checkAgreement = findViewById(R.id.checkAgreement)
        btnLocation = findViewById(R.id.btnLocation)
        btnSignup = findViewById(R.id.btnSignup)
        tvTermsLink = findViewById(R.id.tvTermsLink)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, districts)
        actvDistrict.setAdapter(adapter)

        btnLocation.setOnClickListener {
            mapLauncher.launch(Intent(this, MapPickerActivity::class.java))
        }

        btnSignup.setOnClickListener {
            if (validateForm()) saveShopToDatabase()
        }

        tvTermsLink.setOnClickListener {
            val intent = Intent(this, TermsAndConditionsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateForm(): Boolean {
        if (etShopName.text.isEmpty() || etOwnerName.text.isEmpty() || etMobile.text.isEmpty() ||
            etGST.text.isEmpty() || actvDistrict.text.isEmpty() || etPincode.text.isEmpty() || etAddress.text.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        if (shopLat == 0.0 || shopLng == 0.0) {
            Toast.makeText(this, "Please select shop location", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!checkAgreement.isChecked) {
            Toast.makeText(this, "Accept terms", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveShopToDatabase() {
        val shop = Shop(
            etShopName.text.toString().trim(),
            etOwnerName.text.toString().trim(),
            etMobile.text.toString().trim(),
            etGST.text.toString().trim(),
            etPincode.text.toString().trim(),
            etAddress.text.toString().trim(),
            shopLat,
            shopLng,
            actvDistrict.text.toString().trim()
        )

        FirebaseDatabase.getInstance().getReference("Shops").child(shop.mobile).setValue(shop)
            .addOnSuccessListener {
                startActivity(Intent(this, LanguageSelectionActivity::class.java).putExtra("userType", "Shop"))
                finish()
            }
    }
}