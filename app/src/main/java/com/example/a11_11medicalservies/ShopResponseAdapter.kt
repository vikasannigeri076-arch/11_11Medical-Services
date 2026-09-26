package com.example.a11_11medicalservies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShopResponseAdapter(
    private var responses: List<ShopResponse>,
    private val onShopSelected: (ShopResponse) -> Unit
) : RecyclerView.Adapter<ShopResponseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvShopName: TextView = view.findViewById(R.id.tvShopName)
        val tvDistance: TextView = view.findViewById(R.id.tvDistance)
        val tvMedicineAmount: TextView = view.findViewById(R.id.tvMedicineAmount)
        val tvDeliveryCharge: TextView = view.findViewById(R.id.tvDeliveryCharge)
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_available_shop, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val response = responses[position]
        holder.tvShopName.text = response.shopName
        
        // Only show distance if it is 7km or above
        if (response.distance >= 7.0) {
            holder.tvDistance.visibility = View.VISIBLE
            holder.tvDistance.text = "Distance: %.1f km".format(response.distance)
        } else {
            holder.tvDistance.visibility = View.GONE
        }

        holder.tvMedicineAmount.text = "Medicine: ₹ %.2f".format(response.medicineTotal)
        holder.tvDeliveryCharge.text = "Delivery: ₹ %.2f".format(response.deliveryCharge)
        holder.tvTotalAmount.text = "₹ %.2f".format(response.total)

        holder.itemView.setOnClickListener {
            onShopSelected(response)
        }
    }

    override fun getItemCount() = responses.size

    fun updateData(newResponses: List<ShopResponse>) {
        // Sort by total price (Medicine + Delivery)
        responses = newResponses.sortedBy { it.total }
        notifyDataSetChanged()
    }
}
