package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class OrderHistoryActivityShop : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var orderRecycler: RecyclerView
    private lateinit var btnApproved: Button
    private lateinit var btnDispatched: Button
    private lateinit var btnCompleted: Button
    private lateinit var btnCancelled: Button
    private var bottomNavigation: BottomNavigationView? = null
    
    private lateinit var orderAdapter: OrderAdapter
    private var allOrders = mutableListOf<OrderRequest>()
    private var currentStatus = "Approved"
    private var shopPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_order_history_shop)

            tvTitle = findViewById(R.id.tvTitle)
            orderRecycler = findViewById(R.id.orderRecycler)
            btnApproved = findViewById(R.id.btnConfirmed)
            btnDispatched = findViewById(R.id.btnDispatched)
            btnCompleted = findViewById(R.id.btnCompleted)
            btnCancelled = findViewById(R.id.btnCancelled)
            bottomNavigation = findViewById(R.id.bottomNavigation)

            shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""
            
            if (shopPhone.isEmpty()) {
                Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            setupRecyclerView()
            setupStatusButtons()
            setupBottomNavigation()
            fetchOrders()
            
        } catch (e: Exception) {
            Log.e("OrderHistoryShop", "Error in onCreate", e)
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRecyclerView() {
        orderRecycler.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(emptyList()) { order -> 
            // Handle delete if needed
        }
        orderRecycler.adapter = orderAdapter
    }

    private fun setupStatusButtons() {
        btnApproved.setOnClickListener {
            currentStatus = "Approved"
            updateUI("Approved Orders")
        }
        btnDispatched.setOnClickListener {
            currentStatus = "Dispatched"
            updateUI("Dispatched Orders")
        }
        btnCompleted.setOnClickListener {
            currentStatus = "Completed"
            updateUI("Completed Orders")
        }
        btnCancelled.setOnClickListener {
            currentStatus = "Canceled"
            updateUI("Cancelled Orders")
        }
        updateUI("Approved Orders")
    }

    private fun setupBottomNavigation() {
        bottomNavigation?.let { nav ->
            nav.selectedItemId = R.id.nav_history
            nav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        startActivity(Intent(this, ShopHomeActivity::class.java))
                        finish()
                        true
                    }
                    R.id.nav_history -> true
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
    }

    private fun updateUI(title: String) {
        tvTitle.text = title
        filterAndDisplay()
        btnApproved.alpha = if (currentStatus == "Approved") 1.0f else 0.6f
        btnDispatched.alpha = if (currentStatus == "Dispatched") 1.0f else 0.6f
        btnCompleted.alpha = if (currentStatus == "Completed") 1.0f else 0.6f
        btnCancelled.alpha = if (currentStatus == "Canceled") 1.0f else 0.6f
    }

    private fun fetchOrders() {
        val ordersRef = FirebaseDatabase.getInstance().getReference("Orders")
        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allOrders.clear()
                for (postSnapshot in snapshot.children) {
                    try {
                        val order = postSnapshot.getValue(OrderRequest::class.java)
                        if (order != null) {
                            if (order.approvedByShopPhone == shopPhone || order.responses.containsKey(shopPhone)) {
                                allOrders.add(order)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("OrderHistoryShop", "Error parsing order", e)
                    }
                }
                filterAndDisplay()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("OrderHistoryShop", "Database error", error.toException())
            }
        })
    }

    private fun filterAndDisplay() {
        val filteredList = allOrders.filter { 
            if (currentStatus == "Approved") {
                it.status.equals("Approved", ignoreCase = true) || it.status.equals("Confirmed", ignoreCase = true)
            } else {
                it.status.equals(currentStatus, ignoreCase = true)
            }
        }
        orderAdapter.updateData(filteredList)
    }
}
