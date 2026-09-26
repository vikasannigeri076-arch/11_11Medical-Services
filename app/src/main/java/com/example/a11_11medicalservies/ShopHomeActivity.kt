package com.example.a11_11medicalservies

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class ShopHomeActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvShopName: TextView
    private lateinit var tvTotalEarning: TextView
    private lateinit var tabRequests: TextView
    private lateinit var tabApproved: TextView
    private lateinit var rvOrders: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var orderAdapter: OrderAdapter
    private var allOrders = mutableListOf<OrderRequest>()
    private var currentShopPhone: String? = null
    private var currentShopDistrict: String? = null
    private val deletedOrderIds = mutableSetOf<String>()
    private var currentStatusFilter = "Open"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            startLocationUpdates()
        }
    }

    private val mapPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            val address = result.data?.getStringExtra("address") ?: ""
            
            if (lat != 0.0 && lng != 0.0) {
                tvAddress.text = address
                updateShopLocationInFirebase(lat, lng, address)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_home)

        tvUserName = findViewById(R.id.tvUserName)
        tvAddress = findViewById(R.id.tvAddress)
        tvShopName = findViewById(R.id.tvShopName)
        tvTotalEarning = findViewById(R.id.tvTotalEarning)
        tabRequests = findViewById(R.id.tabRequests)
        tabApproved = findViewById(R.id.tabApproved)
        rvOrders = findViewById(R.id.rvOrders)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        rvOrders.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(emptyList()) { order ->
            deleteOrder(order)
        }
        rvOrders.adapter = orderAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    if (tvAddress.text == "Your Address") {
                        updateAddressFromLatLng(it.latitude, it.longitude)
                    }
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchShopDetails()
        checkLocationPermissions()

        tvAddress.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        tabRequests.setOnClickListener {
            currentStatusFilter = "Open"
            showOrdersByStatus("Open")
            tabRequests.alpha = 1.0f
            tabApproved.alpha = 0.5f
            tabRequests.setBackgroundColor(0xFF88D498.toInt())
            tabApproved.setBackgroundColor(0xFFE0E0E0.toInt())
        }

        tabApproved.setOnClickListener {
            currentStatusFilter = "Orders"
            showOrdersByStatus("Orders")
            tabRequests.alpha = 0.5f
            tabApproved.alpha = 1.0f
            tabRequests.setBackgroundColor(0xFFE0E0E0.toInt())
            tabApproved.setBackgroundColor(0xFFEF473A.toInt())
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_home
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
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
                R.id.nav_settings -> {
                    startActivity(Intent(this, ShopSettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateShopLocationInFirebase(lat: Double, lng: Double, address: String) {
        val phone = currentShopPhone ?: return
        val updates = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "address" to address
        )
        FirebaseDatabase.getInstance().getReference("Shops").child(phone).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Shop location updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchShopDetails() {
        val user = FirebaseAuth.getInstance().currentUser
        val phone = user?.phoneNumber?.replace("+91", "") ?: ""
        currentShopPhone = phone

        if (phone.isEmpty()) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val shopRef = FirebaseDatabase.getInstance().getReference("Shops").child(phone)
        shopRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val shop = snapshot.getValue(Shop::class.java)
                    if (shop != null) {
                        tvUserName.text = "Hello, ${shop.ownerName}"
                        tvShopName.text = shop.shopName
                        currentShopDistrict = shop.district
                        
                        deletedOrderIds.clear()
                        snapshot.child("deletedOrders").children.forEach {
                            it.key?.let { id -> deletedOrderIds.add(id) }
                        }
                        
                        calculateTotalEarnings(phone)
                        fetchOrders(shop)
                    }
                } catch (e: Exception) {}
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun calculateTotalEarnings(phone: String) {
        FirebaseDatabase.getInstance().getReference("Orders")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0.0
                    for (orderSnap in snapshot.children) {
                        val order = orderSnap.getValue(OrderRequest::class.java)
                        if (order != null && order.approvedByShopPhone == phone && order.paymentStatus == "Paid") {
                            // Summing the full Grand Total as requested
                            total += order.grandTotal
                        }
                    }
                    tvTotalEarning.text = "₹ %.2f".format(total)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun updateAddressFromLatLng(lat: Double, lng: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                tvAddress.text = addresses[0].getAddressLine(0)
            }
        } catch (e: Exception) {}
    }

    private fun fetchOrders(shop: Shop) {
        val ordersRef = FirebaseDatabase.getInstance().getReference("Orders")
        ordersRef.orderByChild("district").equalTo(shop.district)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allOrders.clear()
                    for (postSnapshot in snapshot.children) {
                        try {
                            val order = postSnapshot.getValue(OrderRequest::class.java)
                            if (order != null && !deletedOrderIds.contains(order.id)) {
                                if (order.status == "Open") {
                                    if (!order.responses.containsKey(currentShopPhone)) {
                                        allOrders.add(order)
                                    }
                                } else if ((order.status == "Approved" || order.status == "Confirmed" || order.status == "Dispatched" || order.status == "Picked Up" || order.status == "Out for Delivery") 
                                    && order.approvedByShopPhone == currentShopPhone) {
                                    allOrders.add(order)
                                }
                            }
                        } catch (e: Exception) {}
                    }
                    showOrdersByStatus(currentStatusFilter)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showOrdersByStatus(status: String) {
        val filtered = allOrders.filter { 
            if (status == "Open") it.status == "Open" 
            else it.status != "Open"
        }
        orderAdapter.updateData(filtered)
    }

    private fun deleteOrder(order: OrderRequest) {
        val shopPhone = currentShopPhone ?: return
        FirebaseDatabase.getInstance().getReference("Shops")
            .child(shopPhone)
            .child("deletedOrders")
            .child(order.id)
            .setValue(true)
            .addOnSuccessListener {
                Toast.makeText(this, "Order removed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
