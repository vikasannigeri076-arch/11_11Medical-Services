package com.example.a11_11medicalservies

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class OrderApprovalDetailsActivity : AppCompatActivity() {

    private lateinit var tvOrderId: TextView
    private lateinit var tvOrderDateTime: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvCustomerPhone: TextView
    private lateinit var tvDeliveryAddress: TextView
    private lateinit var layoutMedicineList: LinearLayout
    private lateinit var tvBillImageTitle: TextView
    private lateinit var ivShopBill: ImageView
    private lateinit var tvMedicineTotal: TextView
    private lateinit var tvDeliveryCharge: TextView
    private lateinit var tvPlatformFee: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvPaymentStatus: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var cardPrescription: MaterialCardView
    private lateinit var btnViewPrescription: MaterialButton
    private lateinit var btnViewOnMap: MaterialButton
    private lateinit var layoutAssignPartner: LinearLayout
    private lateinit var layoutPartnerDetails: View
    private lateinit var ivPartnerPhoto: ImageView
    private lateinit var tvPartnerName: TextView
    private lateinit var tvPartnerVehicle: TextView
    private lateinit var btnCallPartner: MaterialButton
    private lateinit var btnAssignPartner: MaterialButton
    private lateinit var btnPrintOrderLabel: MaterialButton

    private var orderId: String? = null
    private var orderRef: DatabaseReference? = null
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_approval_details)

        orderId = intent.getStringExtra("orderId")
        if (orderId == null) {
            Toast.makeText(this, getString(R.string.order_id_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        fetchOrderDetails()

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        tvOrderId = findViewById(R.id.tvOrderId)
        tvOrderDateTime = findViewById(R.id.tvOrderDateTime)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone)
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress)
        layoutMedicineList = findViewById(R.id.layoutMedicineList)
        tvBillImageTitle = findViewById(R.id.tvBillImageTitle)
        ivShopBill = findViewById(R.id.ivShopBill)
        tvMedicineTotal = findViewById(R.id.tvMedicineTotal)
        tvDeliveryCharge = findViewById(R.id.tvDeliveryCharge)
        tvPlatformFee = findViewById(R.id.tvPlatformFee)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        tvOrderStatus = findViewById(R.id.tvOrderStatus)
        cardPrescription = findViewById(R.id.cardPrescription)
        btnViewPrescription = findViewById(R.id.btnViewPrescription)
        btnViewOnMap = findViewById(R.id.btnViewOnMap)
        layoutAssignPartner = findViewById(R.id.layoutAssignPartner)
        layoutPartnerDetails = findViewById(R.id.layoutPartnerDetails)
        ivPartnerPhoto = findViewById(R.id.ivPartnerPhoto)
        tvPartnerName = findViewById(R.id.tvPartnerName)
        tvPartnerVehicle = findViewById(R.id.tvPartnerVehicle)
        btnCallPartner = findViewById(R.id.btnCallPartner)
        btnAssignPartner = findViewById(R.id.btnAssignPartner)
        btnPrintOrderLabel = findViewById(R.id.btnPrintOrderLabel)

        btnAssignPartner.setOnClickListener { assignDeliveryPartner() }
        btnPrintOrderLabel.setOnClickListener { openOrderLabel() }
    }

    private fun openOrderLabel() {
        val shopPhone = auth.currentUser?.phoneNumber?.replace("+91", "")
        if (shopPhone == null) {
            Toast.makeText(this, "Shop session expired", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, OrderLabelActivity::class.java).apply {
            putExtra("orderId", orderId)
            putExtra("shopPhone", shopPhone)
        }
        startActivity(intent)
    }

    private fun fetchOrderDetails() {
        orderRef = database.getReference("Orders").child(orderId!!)
        orderRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(OrderRequest::class.java)
                if (order != null) {
                    updateUI(order, snapshot)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderApprovalDetailsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(order: OrderRequest, snapshot: DataSnapshot) {
        val shortId = if (order.id.length > 8) order.id.takeLast(8) else order.id
        tvOrderId.text = getString(R.string.order_id_label, shortId)
        
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        tvOrderDateTime.text = "Date & Time: ${sdf.format(Date(order.timestamp))}"

        tvCustomerName.text = "Name: ${order.customerName}"
        tvCustomerPhone.text = "Phone: ${order.customerPhone}"

        tvDeliveryAddress.text = "Address: ${order.district} (Location Tagged)"

        btnViewOnMap.setOnClickListener {
            val gmmIntentUri = "geo:${order.latitude},${order.longitude}?q=${order.latitude},${order.longitude}(Customer Location)".toUri()
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }

        // We hide the external card and move prescriptions into the Medicines box for a consolidated view
        cardPrescription.visibility = View.GONE

        // 5. Consolidated Ordered Medicines View
        layoutMedicineList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val density = resources.displayMetrics.density

        // --- A. CUSTOMER'S REQUESTED MEDICINES (Text & Image) ---
        val tvHeaderCustomer = TextView(this).apply {
            text = "Customer Requested:"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@OrderApprovalDetailsActivity, R.color.PrimaryTextColor))
            setPadding(0, (8 * density).toInt(), 0, (4 * density).toInt())
        }
        layoutMedicineList.addView(tvHeaderCustomer)

        // Show Text Items
        if (order.items.isNotBlank()) {
            val items = order.items.split("\n").filter { it.isNotBlank() }
            items.forEach { line ->
                val medView = inflater.inflate(R.layout.item_ordered_medicine, layoutMedicineList, false)
                medView.findViewById<TextView>(R.id.tvMedicineName).text = line.trim().removePrefix("-").removePrefix("•").trim()
                medView.findViewById<TextView>(R.id.tvMedicineDetails).text = "Customer Typed Item"
                medView.findViewById<TextView>(R.id.tvMedicinePrice).visibility = View.GONE
                medView.findViewById<ImageView>(R.id.ivMedicineIcon).setImageResource(R.drawable.barorder)
                layoutMedicineList.addView(medView)
            }
        }

        // Show Prescription Photos
        val prescriptions = order.getPrescriptionUrlsList()
        if (prescriptions.isNotEmpty()) {
            val tvPresLabel = TextView(this).apply {
                text = "Prescription Photos:"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding((8 * density).toInt(), (4 * density).toInt(), 0, (4 * density).toInt())
            }
            layoutMedicineList.addView(tvPresLabel)

            val hScroll = android.widget.HorizontalScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                isHorizontalScrollBarEnabled = false
            }
            val imageLayout = LinearLayout(this).apply { 
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, (12 * density).toInt())
            }
            
            prescriptions.forEach { base64OrUrl ->
                val card = com.google.android.material.card.MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams((100 * density).toInt(), (100 * density).toInt()).apply { setMargins(0, 0, (12 * density).toInt(), 0) }
                    radius = 8 * density
                    cardElevation = 2 * density
                }
                val iv = ImageView(this).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setOnClickListener { showFullScreenImage(base64OrUrl) }
                }
                
                try {
                    val decoded = Base64.decode(base64OrUrl, Base64.DEFAULT)
                    iv.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.size))
                } catch (e: Exception) {
                    Glide.with(this).load(base64OrUrl).placeholder(R.drawable.applogo1).into(iv)
                }
                
                card.addView(iv)
                imageLayout.addView(card)
            }
            hScroll.addView(imageLayout)
            layoutMedicineList.addView(hScroll)
        }

        // --- B. SHOP'S CONFIRMED BILLING ---
        // Find which shop response to show (Either currently logged in shop or the approved shop)
        val activeShopPhone = order.approvedByShopPhone ?: auth.currentUser?.phoneNumber?.replace("+91", "") ?: ""
        val shopResponse = order.responses[activeShopPhone]

        if (shopResponse != null) {
            // Divider between Request and Confirmed Bill
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply { 
                    setMargins(0, (12 * density).toInt(), 0, (12 * density).toInt()) 
                }
                setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
            }
            layoutMedicineList.addView(divider)

            val tvHeaderBill = TextView(this).apply {
                text = "Billed Items (Confirmed by Shop)"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(this@OrderApprovalDetailsActivity, R.color.otpbtn))
                setPadding(0, 0, 0, (8 * density).toInt())
            }
            layoutMedicineList.addView(tvHeaderBill)

            shopResponse.getMedicinesList().forEach { item ->
                val medView = inflater.inflate(R.layout.item_ordered_medicine, layoutMedicineList, false)
                val tvName = medView.findViewById<TextView>(R.id.tvMedicineName)
                val tvDetails = medView.findViewById<TextView>(R.id.tvMedicineDetails)
                val tvPrice = medView.findViewById<TextView>(R.id.tvMedicinePrice)
                val ivIcon = medView.findViewById<ImageView>(R.id.ivMedicineIcon)
                
                tvName.text = item.name
                tvDetails.text = "Qty: ${item.quantity} | Unit: ${item.unit}"
                tvPrice.text = "₹ %.2f".format(item.price)
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.otpbtn))
                layoutMedicineList.addView(medView)
            }
            
            // Show the actual bill proof image uploaded by the shop
            if (!shopResponse.billImage.isNullOrEmpty()) {
                tvBillImageTitle.visibility = View.VISIBLE
                ivShopBill.visibility = View.VISIBLE
                try {
                    val decoded = Base64.decode(shopResponse.billImage, Base64.DEFAULT)
                    ivShopBill.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.size))
                    ivShopBill.setOnClickListener { showFullScreenImage(shopResponse.billImage!!) }
                } catch (e: Exception) {
                    ivShopBill.visibility = View.GONE
                }
            } else {
                tvBillImageTitle.visibility = View.GONE
                ivShopBill.visibility = View.GONE
            }

            // Summary data
            val medTotal = shopResponse.medicineTotal
            val delCharge = shopResponse.deliveryCharge
            val platformFee = medTotal * 0.05 
            
            tvMedicineTotal.text = getString(R.string.amount_format, medTotal)
            tvDeliveryCharge.text = getString(R.string.amount_format, delCharge)
            tvPlatformFee.text = getString(R.string.amount_format, platformFee).let { "- $it" }
            tvGrandTotal.text = getString(R.string.amount_format, medTotal + delCharge - platformFee)
        } else {
            // No shop response / bill yet
            tvBillImageTitle.visibility = View.GONE
            ivShopBill.visibility = View.GONE
            
            tvMedicineTotal.text = getString(R.string.amount_format, order.medicineAmount)
            tvDeliveryCharge.text = getString(R.string.amount_format, order.deliveryCharge)
            tvPlatformFee.text = "₹ 0.00"
            tvGrandTotal.text = getString(R.string.amount_format, order.grandTotal)
        }

        tvPaymentMethod.text = "Method: ${order.paymentMethod.ifEmpty { "Pending Selection" }}"
        tvPaymentStatus.text = "Status: ${order.paymentStatus}"
        tvOrderStatus.text = order.status

        // Delivery Partner Details Logic
        val deliverySnap = snapshot.child("delivery")
        if (deliverySnap.exists()) {
            layoutAssignPartner.visibility = View.GONE
            layoutPartnerDetails.visibility = View.VISIBLE
            
            val name = deliverySnap.child("name").getValue(String::class.java)
            val vehicle = deliverySnap.child("vehicleNumber").getValue(String::class.java)
            val phone = deliverySnap.child("phone").getValue(String::class.java)
            val photo = deliverySnap.child("photoUrl").getValue(String::class.java)

            tvPartnerName.text = name ?: getString(R.string.delivery_partner_label)
            tvPartnerVehicle.text = "Vehicle: ${vehicle ?: "N/A"}"
            
            if (!photo.isNullOrEmpty()) {
                Glide.with(this).load(photo).placeholder(R.drawable.applogo1).into(ivPartnerPhoto)
            }

            btnCallPartner.setOnClickListener {
                if (!phone.isNullOrEmpty()) {
                    startActivity(Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()))
                } else {
                    Toast.makeText(this, getString(R.string.driver_contact_unavailable), Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            layoutAssignPartner.visibility = View.VISIBLE
            layoutPartnerDetails.visibility = View.GONE
        }
    }

    private fun showFullScreenImage(base64OrUrl: String) {
        try {
            val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val imageView = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            
            try {
                val decoded = Base64.decode(base64OrUrl, Base64.DEFAULT)
                imageView.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.size))
            } catch (e: Exception) {
                Glide.with(this).load(base64OrUrl).into(imageView)
            }
            
            dialog.setContentView(imageView)
            imageView.setOnClickListener { dialog.dismiss() }
            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Image view failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun assignDeliveryPartner() {
        val updates = mapOf(
            "deliveryStatus" to "Searching",
            "status" to "Preparing"
        )
        orderRef?.updateChildren(updates)?.addOnSuccessListener {
            Toast.makeText(this, "Order updated to Preparing. Looking for partners.", Toast.LENGTH_LONG).show()
        }
    }
}
