package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.ArrayList

class AvailableShopsActivity : AppCompatActivity() {

    private lateinit var layoutSearching: LinearLayout
    private lateinit var tvSearchRadius: TextView
    private lateinit var rvAvailableShops: RecyclerView
    private lateinit var shopResponseAdapter: ShopResponseAdapter

    private var currentRadius = 10
    private var orderId: String? = null
    private var customerPrescription: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_available_shops)

        orderId = intent.getStringExtra("orderId")
        if (orderId == null) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        layoutSearching = findViewById(R.id.layoutSearching)
        tvSearchRadius = findViewById(R.id.tvSearchRadius)
        rvAvailableShops = findViewById(R.id.rvAvailableShops)

        rvAvailableShops.layoutManager = LinearLayoutManager(this)
        
        shopResponseAdapter = ShopResponseAdapter(emptyList()) { selectedShop ->
            val intent = Intent(this, CustomerBillDetailsActivity::class.java)
            intent.putExtra("orderId", orderId)
            intent.putExtra("shopPhone", selectedShop.shopPhone)
            intent.putExtra("shopName", selectedShop.shopName)
            intent.putExtra("subtotal", selectedShop.subtotal)
            intent.putExtra("gst", selectedShop.gst)
            intent.putExtra("medicineTotal", selectedShop.medicineTotal)
            intent.putExtra("deliveryCharge", selectedShop.deliveryCharge)
            intent.putExtra("total", selectedShop.total)
            intent.putExtra("billImage", selectedShop.billImage) // Shop bill
            intent.putExtra("customerPrescription", customerPrescription) // Original slip
            intent.putExtra("medicines", ArrayList(selectedShop.getMedicinesList()))
            startActivity(intent)
        }
        rvAvailableShops.adapter = shopResponseAdapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startSearchingProcess()
        fetchOrderAndListenResponses()
    }

    private fun startSearchingProcess() {
        searchRunnable = object : Runnable {
            override fun run() {
                if (currentRadius < 100) {
                    currentRadius += 10
                    tvSearchRadius.text = "Searching in ${currentRadius}km..."
                    handler.postDelayed(this, 5000)
                }
            }
        }
        handler.postDelayed(searchRunnable!!, 5000)
    }

    private fun fetchOrderAndListenResponses() {
        val orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId!!)
        
        orderRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(OrderRequest::class.java)
                if (order != null) {
                    customerPrescription = order.prescriptionUrl
                    
                    val responses = mutableListOf<ShopResponse>()
                    for (child in snapshot.child("responses").children) {
                        val response = child.getValue(ShopResponse::class.java)
                        if (response != null) {
                            responses.add(response)
                        }
                    }

                    if (responses.isNotEmpty()) {
                        layoutSearching.visibility = View.GONE
                        rvAvailableShops.visibility = View.VISIBLE
                        shopResponseAdapter.updateData(responses)
                    } else {
                        layoutSearching.visibility = View.VISIBLE
                        rvAvailableShops.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AvailableShopsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        searchRunnable?.let { handler.removeCallbacks(it) }
    }
}
