package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerOrdersActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvUserName: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    private val ordersList = mutableListOf<OrderRequest>()
    private lateinit var adapter: CustomerOrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_orders)

        tvUserName = findViewById(R.id.tvUserName)
        rvOrders = findViewById(R.id.rvOrders)
        tvEmpty = findViewById(R.id.tvEmpty)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        rvOrders.layoutManager = LinearLayoutManager(this)
        adapter = CustomerOrdersAdapter(ordersList)
        rvOrders.adapter = adapter

        bottomNavigation.selectedItemId = R.id.nav_orders
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, CustomerHomeActivity::class.java)); finish(); true }
                R.id.nav_orders -> true
                R.id.nav_settings -> { startActivity(Intent(this, CustomerSettingsActivity::class.java)); finish(); true }
                else -> false
            }
        }

        fetchUserData()
        fetchUserOrders()
    }

    private fun fetchUserData() {
        val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: return
        FirebaseDatabase.getInstance().getReference("Customers").child(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                    tvUserName.text = "Hello, $name"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun fetchUserOrders() {
        val phone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""
        if (phone.isEmpty()) return

        FirebaseDatabase.getInstance().getReference("Orders")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ordersList.clear()
                    for (orderSnap in snapshot.children) {
                        try {
                            val order = orderSnap.getValue(OrderRequest::class.java)
                            if (order != null && order.customerPhone == phone) {
                                ordersList.add(order)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    ordersList.sortByDescending { it.timestamp }
                    
                    if (ordersList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvOrders.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvOrders.visibility = View.VISIBLE
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CustomerOrdersActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    inner class CustomerOrdersAdapter(private val list: List<OrderRequest>) :
        RecyclerView.Adapter<CustomerOrdersAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_customer_order, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val order = list[position]
            
            holder.tvTitle.text = "Order #${order.id.takeLast(5)}"
            holder.tvStatus.text = order.status
            
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.tvDate.text = "Date: ${sdf.format(Date(order.timestamp))}"
            
            holder.tvDetails.text = if (order.items.isNotEmpty()) order.items else "Prescription Attached"

            holder.ivDelete.setOnClickListener {
                showDeleteConfirmation(order.id)
            }

            holder.itemView.setOnClickListener {
                resumeOrderProcess(order)
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle: TextView = v.findViewById(R.id.tvOrderTitle)
            val tvStatus: TextView = v.findViewById(R.id.tvOrderStatus)
            val tvDate: TextView = v.findViewById(R.id.tvOrderDate)
            val tvDetails: TextView = v.findViewById(R.id.tvOrderDetails)
            val ivDelete: ImageView = v.findViewById(R.id.ivDelete)
        }
    }

    private fun showDeleteConfirmation(orderId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Order")
            .setMessage("Are you sure you want to delete this order request?")
            .setPositiveButton("Delete") { _, _ ->
                deleteOrder(orderId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteOrder(orderId: String) {
        FirebaseDatabase.getInstance().getReference("Orders").child(orderId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Order deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resumeOrderProcess(order: OrderRequest) {
        when (order.status) {
            "Open" -> {
                // User waiting for quotes or needs to pick a shop
                val intent = Intent(this, AvailableShopsActivity::class.java)
                intent.putExtra("orderId", order.id)
                startActivity(intent)
            }
            "Approved" -> {
                // User has selected a shop but hasn't completed payment yet
                val shopPhone = order.approvedByShopPhone ?: ""
                val shopResponse = order.responses[shopPhone]
                
                val intent = Intent(this, PaymentMethodActivity::class.java)
                intent.putExtra("orderId", order.id)
                intent.putExtra("shopName", shopResponse?.shopName ?: "Shop")
                intent.putExtra("totalAmount", shopResponse?.total ?: 0.0)
                startActivity(intent)
            }
            "Confirmed", "Picked Up", "Dispatched", "Out for Delivery" -> {
                // Order is confirmed and being processed or delivered
                val intent = Intent(this, OrderTrackingActivity::class.java)
                intent.putExtra("orderId", order.id)
                startActivity(intent)
            }
            "Completed" -> {
                // Order finished, show tracking or a summary (tracking shows completed status)
                val intent = Intent(this, OrderTrackingActivity::class.java)
                intent.putExtra("orderId", order.id)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "Status: ${order.status}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
