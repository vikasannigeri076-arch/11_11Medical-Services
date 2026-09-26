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

class ShopCodPaymentActivity : AppCompatActivity() {

    private lateinit var tvTotalEarnings: TextView
    private lateinit var tvFilterDescription: TextView
    private lateinit var btnFilter: MaterialButton
    private lateinit var rvTransactions: RecyclerView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var transactionAdapter: TransactionAdapter
    
    private var shopPhone: String = ""
    private val allTransactions = mutableListOf<ShopTransaction>()
    
    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null // 0-indexed (Jan = 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_cod_payment)

        // Initialize Views
        tvTotalEarnings = findViewById(R.id.tvTotalEarningsAmount)
        tvFilterDescription = findViewById(R.id.tvFilterDescription)
        btnFilter = findViewById(R.id.btnFilter)
        rvTransactions = findViewById(R.id.rvTransactions)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""

        setupRecyclerView()
        setupBottomNavigation()
        fetchCodOrders()

        btnFilter.setOnClickListener {
            showFilterDialog()
        }

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

    private fun fetchCodOrders() {
        if (shopPhone.isEmpty()) return

        val ordersRef = FirebaseDatabase.getInstance().getReference("Orders")
        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTransactions.clear()
                for (orderSnap in snapshot.children) {
                    val order = orderSnap.getValue(OrderRequest::class.java)
                    
                    // Filter: ONLY COD Paid orders approved by this shop
                    if (order != null && order.approvedByShopPhone == shopPhone && 
                        order.paymentMethod.equals("COD", ignoreCase = true) && 
                        order.paymentStatus == "Paid") {
                        
                        // Deriving values from root order object for consistency
                        val totalOrderAmount = order.grandTotal
                        val delCharge = order.deliveryCharge
                        val medAmount = totalOrderAmount - delCharge
                        
                        // 1. Admin takes 5% of Medicine Amount
                        val adminPlatformFee = medAmount * 0.05
                        
                        // 2. Shop earns 95% of Medicine Amount
                        val shopEarned = medAmount - adminPlatformFee
                        
                        // 3. Admin takes 100% of Delivery Charge
                        val totalAdminTaken = adminPlatformFee + delCharge
                        
                        val transaction = ShopTransaction(
                            orderId = order.id,
                            customerName = order.customerName,
                            totalAmount = totalOrderAmount,
                            shopEarnings = shopEarned,
                            adminCommission = totalAdminTaken,
                            deliveryCharge = delCharge,
                            timestamp = order.timestamp
                        )
                        allTransactions.add(transaction)
                    }
                }
                applyFilter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopCodPaymentActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
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

        // Total COD Earning sum at the top
        var totalGrandSum = 0.0
        for (trans in filteredList) {
            totalGrandSum += trans.totalAmount
        }

        filteredList.sortByDescending { it.timestamp }
        transactionAdapter.updateData(filteredList)
        tvTotalEarnings.text = "₹ %.2f".format(totalGrandSum)
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
            .setTitle("Customize COD View")
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
        bottomNavigation.selectedItemId = R.id.nav_money
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, ShopHomeActivity::class.java))
                    finish()
                    true
                }
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
                R.id.nav_money -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, ShopSettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
