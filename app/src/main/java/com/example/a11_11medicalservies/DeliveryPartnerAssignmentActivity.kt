package com.example.a11_11medicalservies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.*

class DeliveryPartnerAssignmentActivity : AppCompatActivity() {

    private lateinit var rvDrivers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private var orderId: String? = null
    private val database = FirebaseDatabase.getInstance()
    private val driversList = mutableListOf<Driver>()
    private lateinit var adapter: DriverAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_delivery_partner_assignment)

        orderId = intent.getStringExtra("orderId")
        if (orderId == null) {
            Toast.makeText(this, "Order ID error", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvDrivers = findViewById(R.id.rvDrivers)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvDrivers.layoutManager = LinearLayoutManager(this)
        adapter = DriverAdapter(driversList) { driver ->
            assignDriverToOrder(driver)
        }
        rvDrivers.adapter = adapter

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }

        fetchAvailableDrivers()
    }

    private fun fetchAvailableDrivers() {
        progressBar.visibility = View.VISIBLE
        // Note: Assuming there is a "Drivers" node in your Firebase
        database.getReference("Drivers")
            .orderByChild("status")
            .equalTo("Available")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    driversList.clear()
                    for (snap in snapshot.children) {
                        val driver = snap.getValue(Driver::class.java)
                        if (driver != null) {
                            driversList.add(driver)
                        }
                    }
                    progressBar.visibility = View.GONE
                    if (driversList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvDrivers.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvDrivers.visibility = View.VISIBLE
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@DeliveryPartnerAssignmentActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun assignDriverToOrder(driver: Driver) {
        val orderRef = database.getReference("Orders").child(orderId!!)
        
        val deliveryData = mapOf(
            "name" to driver.name,
            "phone" to driver.phone,
            "vehicleNumber" to driver.vehicleNumber,
            "photoUrl" to (driver.photoUrl ?: ""),
            "latitude" to driver.latitude,
            "longitude" to driver.longitude,
            "rating" to driver.rating
        )

        val updates = mapOf(
            "delivery" to deliveryData,
            "deliveryStatus" to "Assigned",
            "status" to "Preparing"
        )

        orderRef.updateChildren(updates).addOnSuccessListener {
            // Mark driver as busy (Optional logic based on your flow)
            database.getReference("Drivers").child(driver.phone).child("status").setValue("Busy")
            
            // Notify simulated via order update
            Toast.makeText(this, "${driver.name} assigned successfully!", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to assign: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    inner class DriverAdapter(
        private val list: List<Driver>,
        private val onAssign: (Driver) -> Unit
    ) : RecyclerView.Adapter<DriverAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvDriverName)
            val vehicle: TextView = v.findViewById(R.id.tvVehicleDetails)
            val rating: TextView = v.findViewById(R.id.tvRating)
            val photo: ImageView = v.findViewById(R.id.ivDriverPhoto)
            val btn: MaterialButton = v.findViewById(R.id.btnSelect)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_partner, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val driver = list[position]
            holder.name.text = driver.name
            holder.vehicle.text = driver.vehicleNumber
            holder.rating.text = "⭐ ${driver.rating}"
            
            if (!driver.photoUrl.isNullOrEmpty()) {
                Glide.with(this@DeliveryPartnerAssignmentActivity)
                    .load(driver.photoUrl)
                    .placeholder(R.drawable.applogo1)
                    .into(holder.photo)
            }

            holder.btn.setOnClickListener { onAssign(driver) }
        }

        override fun getItemCount() = list.size
    }
}
