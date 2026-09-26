package com.example.a11_11medicalservies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private var transactions: List<ShopTransaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderName: TextView = view.findViewById(R.id.tvOrderName)
        val tvOrderIdSmall: TextView = view.findViewById(R.id.tvOrderIdSmall)
        val tvTotalAmount: TextView = view.findViewById(R.id.tvTotalAmount)
        val tvShopEarned: TextView = view.findViewById(R.id.tvShopEarned)
        val tvAdminCommission: TextView = view.findViewById(R.id.tvAdminCommission)
        val tvDeliveryCharge: TextView = view.findViewById(R.id.tvDeliveryCharge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvOrderName.text = transaction.customerName
        holder.tvOrderIdSmall.text = "#${transaction.orderId.takeLast(6)}"
        
        // Setting the actual total amount from the transaction data
        holder.tvTotalAmount.text = "₹%.2f".format(transaction.totalAmount)

        holder.tvShopEarned.text = "₹%.2f".format(transaction.shopEarnings)
        holder.tvAdminCommission.text = "₹%.2f".format(transaction.adminCommission)
        holder.tvDeliveryCharge.text = "₹%.2f".format(transaction.deliveryCharge)
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<ShopTransaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}
