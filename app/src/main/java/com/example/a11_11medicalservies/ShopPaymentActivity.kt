package com.example.a11_11medicalservies

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.DateFormatSymbols
import java.util.*

class ShopPaymentActivity : AppCompatActivity() {

    private lateinit var tvTotalEarnings: TextView
    private lateinit var tvFilterDescription: TextView
    private lateinit var btnFilter: MaterialButton
    private lateinit var rvTransactions: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var transactionAdapter: TransactionAdapter
    
    private var shopPhone: String = ""
    private val allTransactions = mutableListOf<ShopTransaction>()
    
    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_payment)

        tvTotalEarnings = findViewById<TextView>(R.id.tvTotalEarningsAmount)
        tvFilterDescription = findViewById<TextView>(R.id.tvFilterDescription)
        btnFilter = findViewById<MaterialButton>(R.id.btnFilter)
        rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""

        setupRecyclerView()
        setupBottomNavigation()
        fetchPaidOnlineOrders()

        btnFilter.setOnClickListener { showFilterDialog() }

        findViewById<View>(R.id.appBarLayout)?.let { appBar ->
            ViewCompat.setOnApplyWindowInsetsListener(appBar) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, systemBars.top, 0, 0)
                insets
            }
        }
    }

    private fun setupRecyclerView() {
        rvTransactions.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(emptyList())
        rvTransactions.adapter = transactionAdapter
    }

    private fun fetchPaidOnlineOrders() {
        if (shopPhone.isEmpty()) return

        val ordersRef = FirebaseDatabase.getInstance().getReference("Orders")
        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTransactions.clear()
                for (orderSnap in snapshot.children) {
                    val order = orderSnap.getValue(OrderRequest::class.java)
                    
                    if (order != null && order.approvedByShopPhone == shopPhone && 
                        order.paymentStatus == "Paid" && !order.paymentMethod.equals("COD", ignoreCase = true)) {
                        
                        // Robust retrieval of medicine total and delivery charge from the database snapshot
                        // to avoid issues with map parsing or missing root fields
                        val responsesSnap = orderSnap.child("responses")
                        var shopRespSnap = responsesSnap.child(shopPhone)
                        if (!shopRespSnap.exists()) {
                            shopRespSnap = responsesSnap.child("+91$shopPhone")
                        }
                        
                        var delCharge = 0.0
                        var medAmount = 0.0
                        
                        if (shopRespSnap.exists()) {
                            delCharge = shopRespSnap.child("deliveryCharge").getValue(Double::class.java) ?: 0.0
                            medAmount = shopRespSnap.child("medicineTotal").getValue(Double::class.java) ?: 0.0
                        }
                        
                        // Fallback to root fields if snapshot lookup failed
                        if (delCharge == 0.0 && order.deliveryCharge > 0) delCharge = order.deliveryCharge
                        if (medAmount == 0.0) {
                            medAmount = if (order.medicineAmount > 0) order.medicineAmount else (order.grandTotal - delCharge)
                        }
                        
                        // Calculation logic: 95% Shop / 5% Admin
                        val adminCommission = medAmount * 0.05
                        val shopEarned = medAmount - adminCommission
                        
                        val transaction = ShopTransaction(
                            orderId = order.id,
                            customerName = order.customerName,
                            totalAmount = order.grandTotal,
                            shopEarnings = shopEarned,
                            adminCommission = adminCommission,
                            deliveryCharge = delCharge,
                            timestamp = order.timestamp
                        )
                        allTransactions.add(transaction)
                        
                        // Ensure we have the live customer name
                        fetchRealCustomerName(order.customerPhone, transaction)
                    }
                }
                applyFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopPaymentActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchRealCustomerName(phone: String, transaction: ShopTransaction) {
        FirebaseDatabase.getInstance().getReference("Customers").child(phone).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val realName = snapshot.getValue(String::class.java)
                    if (realName != null) {
                        val index = allTransactions.indexOfFirst { it.orderId == transaction.orderId }
                        if (index != -1) {
                            allTransactions[index] = allTransactions[index].copy(customerName = realName)
                            transactionAdapter.notifyItemChanged(index)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun applyFilter() {
        val filteredList = if (selectedYear == null && selectedMonth == null) {
            tvFilterDescription.text = "All time"
            allTransactions.toMutableList()
        } else {
            val cal = Calendar.getInstance()
            allTransactions.filter { trans ->
                cal.timeInMillis = trans.timestamp
                val yearMatches = selectedYear == null || cal.get(Calendar.YEAR) == selectedYear
                val monthMatches = selectedMonth == null || cal.get(Calendar.MONTH) == selectedMonth
                yearMatches && monthMatches
            }.toMutableList()
        }

        if (selectedYear != null || selectedMonth != null) {
            val monthName = if (selectedMonth != null) DateFormatSymbols().months[selectedMonth!!] else ""
            tvFilterDescription.text = "Filtered: $monthName ${selectedYear ?: ""}".trim()
        }

        var sumTotal = 0.0
        for (trans in filteredList) {
            sumTotal += trans.totalAmount
        }

        filteredList.sortByDescending { it.timestamp }
        transactionAdapter.updateData(filteredList)
        tvTotalEarnings.text = "₹ %.2f".format(sumTotal)
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_filter, null)
        val spinnerMonth = dialogView.findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerYear = dialogView.findViewById<Spinner>(R.id.spinnerYear)

        val months = arrayOf("All Months", "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December")
        spinnerMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        selectedMonth?.let { spinnerMonth.setSelection(it + 1) }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = mutableListOf("All Years")
        for (i in currentYear downTo 2023) {
            years.add(i.toString())
        }
        spinnerYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
        selectedYear?.let { year ->
            val index = years.indexOf(year.toString())
            if (index != -1) spinnerYear.setSelection(index)
        }

        AlertDialog.Builder(this)
            .setTitle("Customize Online View")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val monthPos = spinnerMonth.selectedItemPosition
                val yearStr = spinnerYear.selectedItem.toString()

                selectedMonth = if (monthPos == 0) null else monthPos - 1
                selectedYear = if (yearStr == "All Years") null else yearStr.toInt()

                applyFilter()
            }
            .setNegativeButton("Clear Filter") { _, _ ->
                selectedMonth = null
                selectedYear = null
                applyFilter()
            }
            .show()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_payment
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { startActivity(Intent(this, ShopHomeActivity::class.java)); finish(); true }
                R.id.nav_history -> { startActivity(Intent(this, OrderHistoryActivityShop::class.java)); finish(); true }
                R.id.nav_payment -> true
                R.id.nav_money -> { startActivity(Intent(this, ShopCodPaymentActivity::class.java)); finish(); true }
                R.id.nav_settings -> { startActivity(Intent(this, ShopSettingsActivity::class.java)); finish(); true }
                else -> false
            }
        }
    }
}
