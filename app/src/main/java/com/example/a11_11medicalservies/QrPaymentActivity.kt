package com.example.a11_11medicalservies

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

class QrPaymentActivity : AppCompatActivity() {

    private lateinit var imgQR: ImageView
    private lateinit var imgBarcode: ImageView
    private lateinit var tvAmountDetails: TextView
    private lateinit var tvPrintCustomerName: TextView
    private lateinit var tvPrintAddress: TextView
    private lateinit var btnPrint: Button
    private lateinit var btnSearchDeliveryBoy: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_qr_payment)

            imgQR = findViewById(R.id.imgQR)
            imgBarcode = findViewById(R.id.imgBarcode)
            tvAmountDetails = findViewById(R.id.tvAddress)
            tvPrintCustomerName = findViewById(R.id.tvPrintCustomerName)
            tvPrintAddress = findViewById(R.id.tvPrintAddress)
            btnPrint = findViewById(R.id.btnPrint)
            btnSearchDeliveryBoy = findViewById(R.id.btnSearchDeliveryBoy)
            
            btnPrint.text = "Print Label"

            val orderId = intent.getStringExtra("orderId") ?: "00000"
            val totalAmount = intent.getDoubleExtra("totalAmount", 0.0)
            val shopName = intent.getStringExtra("shopName") ?: "Shop"

            // Fetch additional details from Firebase using orderId
            fetchOrderDetailsForPrinting(orderId)

            val amountInfoTemplate = """
                Shop: %s
                Total Payable: ₹%.2f
                
                (Note: Split logic will be processed by our 
                secure gateway upon successful transaction)
            """.trimIndent()
            
            tvAmountDetails.text = amountInfoTemplate.format(shopName, totalAmount)

            generateUpiQRCode(totalAmount, shopName)
            generateBarcode(orderId)

            btnPrint.setOnClickListener {
                Toast.makeText(this, "Connecting to printer...", Toast.LENGTH_SHORT).show()
                // Logic for printing the label would go here
            }

            btnSearchDeliveryBoy.setOnClickListener {
                Toast.makeText(this, "Searching for nearby delivery boys...", Toast.LENGTH_LONG).show()
                // Logic for searching delivery boy would go here
            }
            
            findViewById<View>(R.id.toolbar)?.setOnClickListener {
                finish()
            }
            
        } catch (e: Exception) {
            Log.e("QrPaymentActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun fetchOrderDetailsForPrinting(orderId: String) {
        if (orderId == "00000") return

        FirebaseDatabase.getInstance().getReference("Orders").child(orderId)
            .get().addOnSuccessListener { snapshot ->
                val order = snapshot.getValue(OrderRequest::class.java)
                if (order != null) {
                    tvPrintCustomerName.text = order.customerName
                    tvPrintAddress.text = order.district 
                    
                    fetchCustomerAddress(order.customerPhone)
                }
            }
    }

    private fun fetchCustomerAddress(phone: String) {
        FirebaseDatabase.getInstance().getReference("Customers").child(phone)
            .child("address").get().addOnSuccessListener { snapshot ->
                val address = snapshot.getValue(String::class.java)
                if (!address.isNullOrEmpty()) {
                    tvPrintAddress.text = address
                }
            }
    }

    private fun generateUpiQRCode(amount: Double, name: String) {
        val upiVpa = "company@upi"
        
        try {
            val encodedName = Uri.encode(name)
            val upiUri = "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR".format(upiVpa, encodedName, amount)

            val writer = MultiFormatWriter()
            val matrix = writer.encode(upiUri, BarcodeFormat.QR_CODE, 512, 512)
            val encoder = BarcodeEncoder()
            val bitmap = encoder.createBitmap(matrix)
            imgQR.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("QrPaymentActivity", "Error generating QR", e)
        }
    }

    private fun generateBarcode(data: String) {
        try {
            val writer = MultiFormatWriter()
            val matrix = writer.encode(data, BarcodeFormat.CODE_128, 600, 200)
            val encoder = BarcodeEncoder()
            val bitmap = encoder.createBitmap(matrix)
            imgBarcode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("QrPaymentActivity", "Error generating Barcode", e)
        }
    }
}
