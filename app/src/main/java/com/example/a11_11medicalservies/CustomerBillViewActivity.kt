package com.example.a11_11medicalservies

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.FirebaseDatabase

class CustomerBillViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_bill_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val tvShopName = findViewById<TextView>(R.id.tvBillShopName)
        val tvSubtotal = findViewById<TextView>(R.id.tvBillSubtotal)
        val tvGst = findViewById<TextView>(R.id.tvBillGst)
        val tvTotal = findViewById<TextView>(R.id.tvBillTotal)
        val ivShopBill = findViewById<ImageView>(R.id.ivShopBillImage)
        val tvBillTitle = findViewById<TextView>(R.id.tvShopBillTitle)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmOrder)

        val orderId = intent.getStringExtra("orderId")
        val shopPhone = intent.getStringExtra("shopPhone")
        val shopName = intent.getStringExtra("shopName")
        val subtotal = intent.getDoubleExtra("subtotal", 0.0)
        val gst = intent.getDoubleExtra("gst", 0.0)
        val total = intent.getDoubleExtra("total", 0.0)
        val billImage = intent.getStringExtra("billImage")

        tvShopName.text = shopName
        tvSubtotal.text = "₹ %.2f".format(subtotal)
        tvGst.text = "₹ %.2f".format(subtotal * gst / 100.0)
        tvTotal.text = "₹ %.2f".format(total)

        if (!billImage.isNullOrEmpty()) {
            tvBillTitle.visibility = View.VISIBLE
            ivShopBill.visibility = View.VISIBLE
            try {
                val decodedString = Base64.decode(billImage, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivShopBill.setImageBitmap(bitmap)
            } catch (e: Exception) {}
        }

        btnConfirm.setOnClickListener {
            // Logic to confirm and notify shop
            if (orderId != null && shopPhone != null) {
                FirebaseDatabase.getInstance().getReference("Orders")
                    .child(orderId)
                    .child("status")
                    .setValue("Approved")
                    .addOnSuccessListener {
                        FirebaseDatabase.getInstance().getReference("Orders")
                            .child(orderId)
                            .child("approvedByShopPhone")
                            .setValue(shopPhone)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Order Confirmed with $shopName!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                    }
            }
        }
    }
}
