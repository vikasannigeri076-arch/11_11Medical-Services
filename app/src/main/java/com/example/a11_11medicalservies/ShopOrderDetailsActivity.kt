package com.example.a11_11medicalservies

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ShopOrderDetailsActivity : AppCompatActivity() {

    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerAddress: TextView
    private lateinit var layoutMedicineList: LinearLayout
    private lateinit var tvTotalBillAmount: TextView
    private lateinit var btnDispatch: Button
    private lateinit var btnCall: ImageView
    private lateinit var btnCustomize: TextView
    private lateinit var layoutOrderDetails: LinearLayout

    private var orderId: String? = null
    private var customerPhone: String? = null
    private var currentShopName: String = "Shop"
    private var currentOrder: OrderRequest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_order_details)

        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerAddress = findViewById(R.id.tvCustomerAddress)
        layoutMedicineList = findViewById(R.id.layoutMedicineList)
        tvTotalBillAmount = findViewById(R.id.tvTotalBillAmount)
        btnDispatch = findViewById(R.id.btnDispatch)
        btnCall = findViewById(R.id.btnCallCustomer)
        btnCustomize = findViewById(R.id.btnCustomize)
        layoutOrderDetails = findViewById(R.id.layoutOrderDetails)

        orderId = intent.getStringExtra("orderId")
        
        if (orderId != null) {
            fetchOrderDetails(orderId!!)
        }

        btnCall.setOnClickListener {
            Toast.makeText(this, "Connecting you securely to the customer...", Toast.LENGTH_LONG).show()
        }

        btnDispatch.setOnClickListener {
            updateOrderStatusAndNavigateToQr("Dispatched")
        }

        btnCustomize.setOnClickListener {
            showCustomizeDialog()
        }

        layoutOrderDetails.setOnClickListener {
            navigateToQrPayment()
        }

        // Hide drug request related views if they exist in the layout
        findViewById<View>(R.id.btnSubmitRequest)?.visibility = View.GONE
        findViewById<View>(R.id.layoutUploadedInfo)?.visibility = View.GONE
    }

    private fun navigateToQrPayment() {
        val shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""
        val myBill = currentOrder?.responses?.get(shopPhone)
        
        if (myBill != null) {
            val intent = Intent(this, QrPaymentActivity::class.java).apply {
                putExtra("orderId", orderId)
                putExtra("totalAmount", myBill.total)
                putExtra("shopName", currentShopName)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Order details not ready yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchOrderDetails(id: String) {
        FirebaseDatabase.getInstance().getReference("Orders").child(id)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val order = snapshot.getValue(OrderRequest::class.java)
                    currentOrder = order
                    if (order != null) {
                        tvCustomerName.text = "Name : ${order.customerName}"
                        customerPhone = order.customerPhone
                        
                        val shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""
                        val myBill = order.responses[shopPhone]
                        
                        if (myBill != null) {
                            currentShopName = myBill.shopName
                            tvTotalBillAmount.text = "₹ %.2f".format(myBill.total)
                            
                            layoutMedicineList.removeAllViews()
                            myBill.getMedicinesList().forEachIndexed { index, item ->
                                val tv = TextView(this@ShopOrderDetailsActivity)
                                tv.text = "${index + 1}. ${item.name} (${item.quantity} ${item.unit}) - ₹${item.price}"
                                tv.setPadding(0, 8, 0, 8)
                                tv.setTextColor(resources.getColor(R.color.PrimaryTextColor))
                                layoutMedicineList.addView(tv)
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showCustomizeDialog() {
        val order = currentOrder ?: return
        val shopPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""
        val myBill = order.responses[shopPhone] ?: return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_customize_medicine, null)
        val rvCustomize = dialogView.findViewById<RecyclerView>(R.id.rvCustomizeList)
        val tvNewTotal = dialogView.findViewById<TextView>(R.id.tvNewTotal)
        val etGstPercent = dialogView.findViewById<EditText>(R.id.etGstPercent)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btnUpdateOrder)

        val tempMedicines = myBill.getMedicinesList().map { it.copy() }.toMutableList()
        etGstPercent.setText(myBill.gst.toString())
        
        val recalculateTotal = {
            val subtotal = tempMedicines.sumOf { it.price }
            val gstPercent = etGstPercent.text.toString().toDoubleOrNull() ?: 0.0
            val gstAmount = subtotal * (gstPercent / 100.0)
            val total = subtotal + gstAmount
            tvNewTotal.text = "₹ %.2f".format(total)
        }

        val adapter = CustomizeAdapter(tempMedicines) {
            recalculateTotal()
        }
        
        rvCustomize.layoutManager = LinearLayoutManager(this)
        rvCustomize.adapter = adapter
        
        etGstPercent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                recalculateTotal()
            }
        })

        recalculateTotal()

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnUpdate.setOnClickListener {
            val subtotal = tempMedicines.sumOf { it.price }
            val gstPercent = etGstPercent.text.toString().toDoubleOrNull() ?: 0.0
            val gstAmount = subtotal * (gstPercent / 100.0)
            val total = subtotal + gstAmount
            updateShopBill(shopPhone, tempMedicines, subtotal, gstPercent, total)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateShopBill(shopPhone: String, newMedicines: List<MedicineBillItem>, subtotal: Double, gst: Double, total: Double) {
        if (orderId == null) return
        
        val ref = FirebaseDatabase.getInstance().getReference("Orders").child(orderId!!)
            .child("responses").child(shopPhone)
            
        val updates = mapOf(
            "medicines" to newMedicines,
            "subtotal" to subtotal,
            "gst" to gst,
            "total" to total
        )

        ref.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Order updated successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    inner class CustomizeAdapter(
        private val medicines: MutableList<MedicineBillItem>,
        private val onUpdate: () -> Unit
    ) : RecyclerView.Adapter<CustomizeAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_customize_medicine, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = medicines[position]
            holder.tvName.text = item.name
            holder.etPrice.setText(item.price.toString())
            holder.etQty.setText(item.quantity.toString())
            
            val units = arrayOf("Single", "Strip", "Box")
            val spinnerAdapter = ArrayAdapter(holder.itemView.context, android.R.layout.simple_spinner_item, units)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            holder.spinnerUnit.adapter = spinnerAdapter
            
            val unitIndex = units.indexOf(item.unit)
            if (unitIndex >= 0) holder.spinnerUnit.setSelection(unitIndex)

            holder.spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        medicines[currentPos] = medicines[currentPos].copy(unit = units[pos])
                        onUpdate()
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        val p = holder.etPrice.text.toString().toDoubleOrNull() ?: 0.0
                        val q = holder.etQty.text.toString().toIntOrNull() ?: 1
                        medicines[currentPos] = medicines[currentPos].copy(price = p, quantity = q)
                        onUpdate()
                    }
                }
            }

            holder.etPrice.addTextChangedListener(watcher)
            holder.etQty.addTextChangedListener(watcher)
        }

        override fun getItemCount() = medicines.size

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvMedName)
            val etPrice: EditText = v.findViewById(R.id.etPrice)
            val etQty: EditText = v.findViewById(R.id.etQuantity)
            val spinnerUnit: Spinner = v.findViewById(R.id.spinnerUnit)
        }
    }

    private fun updateOrderStatusAndNavigateToQr(newStatus: String) {
        if (orderId == null) return
        
        FirebaseDatabase.getInstance().getReference("Orders").child(orderId!!)
            .child("status").setValue(newStatus)
            .addOnSuccessListener {
                navigateToQrPayment()
                finish()
            }
    }
}
