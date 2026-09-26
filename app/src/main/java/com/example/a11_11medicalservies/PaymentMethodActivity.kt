package com.example.a11_11medicalservies

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class PaymentMethodActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var tvUserName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvShopNameDisplay: TextView
    private lateinit var tvTotalAmountDisplay: TextView
    private lateinit var paymentGroup: RadioGroup
    private lateinit var btnConfirmOrder: Button

    private var orderId: String? = null
    private var totalAmount: Double = 0.0
    private var medicineAmount: Double = 0.0
    private var deliveryCharge: Double = 0.0
    private var shopName: String? = null
    private var shopPhone: String? = null
    private var customerPhone: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_method)

        // Initialize Razorpay
        Checkout.preload(applicationContext)

        tvUserName = findViewById(R.id.tvUserName)
        tvAddress = findViewById(R.id.tvAddress)
        tvShopNameDisplay = findViewById(R.id.tvShopNameDisplay)
        tvTotalAmountDisplay = findViewById(R.id.tvTotalAmountDisplay)
        paymentGroup = findViewById(R.id.paymentGroup)
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder)

        orderId = intent.getStringExtra("orderId")
        shopName = intent.getStringExtra("shopName") ?: "Shop"
        totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
        medicineAmount = intent.getDoubleExtra("medicineAmount", 0.0)
        deliveryCharge = intent.getDoubleExtra("deliveryCharge", 0.0)

        tvShopNameDisplay.text = shopName
        tvTotalAmountDisplay.text = "Total Amount: ₹ %.2f".format(totalAmount)

        fetchOrderDetailsAndUserData()

        btnConfirmOrder.setOnClickListener {
            val selectedId = paymentGroup.checkedRadioButtonId
            if (selectedId == -1) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val radioButton = findViewById<RadioButton>(selectedId)
            val paymentMethod = radioButton.text.toString()

            if (paymentMethod.contains("UPI") || paymentMethod.contains("Online")) {
                startRazorpayPayment()
            } else {
                showCodConfirmationDialog()
            }
        }
    }

    private fun fetchOrderDetailsAndUserData() {
        customerPhone = FirebaseAuth.getInstance().currentUser?.phoneNumber?.replace("+91", "") ?: ""
        
        // Fetch User Data
        FirebaseDatabase.getInstance().getReference("Customers").child(customerPhone!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                    val address = snapshot.child("address").getValue(String::class.java) ?: "Your Address"
                    tvUserName.text = "Hello, $name"
                    tvAddress.text = address
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        // Fetch Shop Phone from Order
        if (orderId != null) {
            FirebaseDatabase.getInstance().getReference("Orders").child(orderId!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val order = snapshot.getValue(OrderRequest::class.java)
                        if (order != null) {
                            shopPhone = order.approvedByShopPhone
                            // If amounts weren't passed in intent, try getting them from shop response
                            if (medicineAmount == 0.0 && shopPhone != null) {
                                val resp = order.responses[shopPhone]
                                medicineAmount = resp?.medicineTotal ?: 0.0
                                deliveryCharge = resp?.deliveryCharge ?: 0.0
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun showCodConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Cash on Delivery")
            .setMessage("Are you sure you want to place this order using COD? You will pay ₹%.2f to the delivery partner.".format(totalAmount))
            .setPositiveButton("Confirm") { _, _ ->
                processOrderConfirmation("COD", "Pending")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startRazorpayPayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_TFlF85gkjcq2vn")

        try {
            val options = JSONObject()
            options.put("name", "11:11 Medical Services")
            options.put("description", "Payment for Medicines - $shopName")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("currency", "INR")
            options.put("amount", (totalAmount * 100).toInt().toString()) // Amount in paise
            options.put("prefill.contact", customerPhone)
            
            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
        savePaymentRecord(razorpayPaymentId, "Paid")
        processOrderConfirmation("Online", "Paid")
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }

    private fun savePaymentRecord(razorpayId: String?, status: String) {
        val paymentRef = FirebaseDatabase.getInstance().getReference("Payments").push()
        val paymentRecord = PaymentRecord(
            paymentId = paymentRef.key ?: "",
            orderId = orderId ?: "",
            customerPhone = customerPhone ?: "",
            shopPhone = shopPhone ?: "",
            amount = totalAmount,
            status = status,
            method = "Razorpay",
            timestamp = System.currentTimeMillis(),
            razorpayPaymentId = razorpayId
        )
        paymentRef.setValue(paymentRecord)
    }

    private fun processOrderConfirmation(method: String, pStatus: String) {
        if (orderId == null) return

        val updates = mutableMapOf<String, Any>()
        updates["status"] = "Confirmed"
        updates["paymentMethod"] = method
        updates["paymentStatus"] = pStatus
        updates["medicineAmount"] = medicineAmount
        updates["deliveryCharge"] = deliveryCharge
        updates["grandTotal"] = totalAmount
        updates["confirmationTimestamp"] = System.currentTimeMillis()
        updates["deliveryStatus"] = "Pending"

        FirebaseDatabase.getInstance().getReference("Orders").child(orderId!!)
            .updateChildren(updates)
            .addOnSuccessListener {
                sendNotificationToShop()
                navigateToTracking()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to confirm order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendNotificationToShop() {
        if (shopPhone == null) return
        
        val notificationRef = FirebaseDatabase.getInstance().getReference("Notifications").push()
        val notification = OrderNotification(
            id = notificationRef.key ?: "",
            shopPhone = shopPhone!!,
            title = "New Confirmed Order!",
            message = "A customer has confirmed an order for ₹%.2f. Start preparing!".format(totalAmount),
            orderId = orderId!!,
            timestamp = System.currentTimeMillis(),
            read = false
        )
        notificationRef.setValue(notification)
    }

    private fun navigateToTracking() {
        Toast.makeText(this, "Order Placed Successfully!", Toast.LENGTH_LONG).show()
        val intent = Intent(this, OrderTrackingActivity::class.java)
        intent.putExtra("orderId", orderId)
        intent.putExtra("shopPhone", shopPhone)
        // Clear activity stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
