package com.example.a11_11medicalservies

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderAdapter(
    private var orders: List<OrderRequest>,
    private val onDeleteClick: (OrderRequest) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCustomerName: TextView = view.findViewById(R.id.tvCustomerName)
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvOrderDateTime: TextView = view.findViewById(R.id.tvOrderDateTime)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvApprovalDateTime: TextView = view.findViewById(R.id.tvApprovalDateTime)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order_request, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        
        // Initial setup
        holder.tvCustomerName.text = order.customerName
        holder.tvOrderId.text = "Order #${order.id.takeLast(8)}"
        
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        holder.tvOrderDateTime.text = sdf.format(Date(order.timestamp))
        holder.tvStatus.text = order.status

        // Fetch live name for better identification
        FirebaseDatabase.getInstance().getReference("Customers")
            .child(order.customerPhone).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.getValue(String::class.java)
                    if (name != null) holder.tvCustomerName.text = name
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        when (order.status) {
            "Approved", "Confirmed", "Preparing", "Picked Up", "Dispatched", "Out for Delivery" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.rect_border_black)
                if (order.approvalTimestamp != null) {
                    holder.tvApprovalDateTime.visibility = View.VISIBLE
                    holder.tvApprovalDateTime.text = "Approved: ${sdf.format(Date(order.approvalTimestamp))}"
                }
            }
            "Completed" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.rounded_green_badge)
                holder.tvApprovalDateTime.visibility = View.GONE
            }
            else -> {
                holder.tvStatus.setBackgroundResource(R.drawable.rounded_green_badge)
                holder.tvApprovalDateTime.visibility = View.GONE
            }
        }

        holder.ivDelete.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Delete Request")
                .setMessage("Are you sure you want to remove this request from your list?")
                .setPositiveButton("Delete") { _, _ -> onDeleteClick(order) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            when (order.status) {
                "Open" -> {
                    val intent = Intent(context, MedicineAvailabilityActivity::class.java).apply {
                        putExtra("orderId", order.id)
                        putExtra("customerName", order.customerName)
                        putExtra("medicineRequirements", order.items)
                        putExtra("lat", order.latitude)
                        putExtra("lng", order.longitude)
                        putExtra("prescriptionUrl", order.prescriptionUrl)
                    }
                    context.startActivity(intent)
                }
                "Approved", "Confirmed", "Preparing", "Picked Up", "Dispatched", "Out for Delivery", "Completed" -> {
                    // Open the modern Order Approval Details screen
                    val intent = Intent(context, OrderApprovalDetailsActivity::class.java).apply {
                        putExtra("orderId", order.id)
                    }
                    context.startActivity(intent)
                }
                "Canceled" -> {
                    Toast.makeText(context, "This order was canceled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = orders.size

    fun updateData(newOrders: List<OrderRequest>) {
        orders = newOrders
        notifyDataSetChanged()
    }
}
