package com.example.a11_11medicalservies

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class CustomerBillDetailsActivity : AppCompatActivity() {

    private lateinit var layoutDoctorInfo: LinearLayout
    private lateinit var etDoctorKmcNo: EditText
    private lateinit var ivDoctorSign: ImageView
    private lateinit var layoutDoctorSign: RelativeLayout
    
    private var signatureBase64: String? = null
    private var isDoctorInfoRequired = false
    private var orderId: String? = null
    private var shopPhone: String? = null
    private var shopName: String? = "Shop"
    private var totalAmount: Double = 0.0

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            bitmap?.let {
                ivDoctorSign.setImageBitmap(it)
                signatureBase64 = bitmapToBase64(it)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_bill_details)

        // --- Get Data from Intent ---
        orderId = intent.getStringExtra("orderId")
        shopPhone = intent.getStringExtra("shopPhone")
        shopName = intent.getStringExtra("shopName") ?: "Shop Details"
        val subtotal = intent.getDoubleExtra("subtotal", 0.0)
        val gstPercent = intent.getDoubleExtra("gst", 0.0)
        totalAmount = intent.getDoubleExtra("total", 0.0)
        val billImageBase64 = intent.getStringExtra("billImage")
        val customerPrescription = intent.getStringExtra("customerPrescription")
        val medicines = intent.getSerializableExtra("medicines") as? ArrayList<MedicineBillItem> ?: ArrayList()

        if (orderId == null) {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // --- Setup Toolbar ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = shopName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Find btnCallShop - using activity findView to be safer
        val btnCallShop = findViewById<ImageView>(R.id.btnCallShop)
        btnCallShop?.setOnClickListener {
            if (!shopPhone.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:$shopPhone")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Shop phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Find Views ---
        val layoutMedicines = findViewById<LinearLayout>(R.id.layoutMedicineList)
        val layoutPrescriptionBox = findViewById<View>(R.id.layoutPrescriptionBox)
        val ivCustomerSlip = findViewById<ImageView>(R.id.ivCustomerSlip)
        val tvPrescriptionPrice = findViewById<TextView>(R.id.tvPrescriptionPrice)
        val tvSubtotalView = findViewById<TextView>(R.id.tvSubtotal)
        val tvGstView = findViewById<TextView>(R.id.tvGst)
        val tvTotalAmountView = findViewById<TextView>(R.id.tvTotalAmount)
        val ivShopBillPreview = findViewById<ImageView>(R.id.ivShopBillPreview)
        val imgIcon = findViewById<ImageView>(R.id.imgIcon)
        val btnConfirmOrder = findViewById<Button>(R.id.btnConfirmOrder)
        
        layoutDoctorInfo = findViewById(R.id.layoutDoctorInfo)
        etDoctorKmcNo = findViewById(R.id.etDoctorKmcNo)
        ivDoctorSign = findViewById(R.id.ivDoctorSign)
        layoutDoctorSign = findViewById(R.id.layoutDoctorSign)

        // --- Populate Summary ---
        tvSubtotalView.text = "₹ %.2f".format(subtotal)
        tvGstView.text = "${gstPercent}%"
        tvTotalAmountView.text = "₹ %.2f".format(totalAmount)

        // --- Populate Medicine List ---
        val inflater = LayoutInflater.from(this)
        var medicinesPriceSum = 0.0
        medicines.forEach { item ->
            val rowView = inflater.inflate(R.layout.item_medicine_bill_row, layoutMedicines, false)
            val tvName = rowView.findViewById<TextView>(R.id.tvMedicineName)
            val tvPrice = rowView.findViewById<TextView>(R.id.etMedicinePrice)
            
            tvName.text = item.name
            tvPrice.text = "₹ %.2f".format(item.price)
            tvPrice.isEnabled = false
            layoutMedicines.addView(rowView)
            medicinesPriceSum += item.price
        }

        // --- Handle Customer Prescription ---
        if (!customerPrescription.isNullOrEmpty()) {
            layoutPrescriptionBox.visibility = View.VISIBLE
            try {
                val decodedString = Base64.decode(customerPrescription, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivCustomerSlip.setImageBitmap(bitmap)
                ivCustomerSlip.setOnClickListener { showFullScreenImage(bitmap) }
                
                val pPrice = subtotal - medicinesPriceSum
                tvPrescriptionPrice.text = "₹ %.2f".format(if (pPrice > 0) pPrice else 0.0)
            } catch (e: Exception) {}
        }

        // --- Handle Shop's Manual Bill --- 
        if (!billImageBase64.isNullOrEmpty()) {
            ivShopBillPreview.visibility = View.VISIBLE
            imgIcon.visibility = View.VISIBLE
            try {
                val decodedString = Base64.decode(billImageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivShopBillPreview.setImageBitmap(bitmap)
                
                val showBill = { showFullScreenImage(bitmap) }
                imgIcon.setOnClickListener { showBill() }
                ivShopBillPreview.setOnClickListener { showBill() }
            } catch (e: Exception) {
                ivShopBillPreview.visibility = View.GONE
                imgIcon.visibility = View.GONE
            }
        }

        // --- Check for Doctor Information Request ---
        checkDoctorRequest()

        layoutDoctorSign.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        btnConfirmOrder.setOnClickListener {
            if (isDoctorInfoRequired) {
                val kmc = etDoctorKmcNo.text.toString().trim()
                if (kmc.isEmpty() || signatureBase64 == null) {
                    Toast.makeText(this, "Please provide Doctor KMC No and Signature", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            
            if (orderId != null && shopPhone != null) {
                confirmOrderWithShop(orderId!!, shopPhone!!, shopName!!)
            }
        }
    }

    private fun checkDoctorRequest() {
        FirebaseDatabase.getInstance().getReference("Orders")
            .child(orderId!!)
            .child("drugRequest")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        layoutDoctorInfo.visibility = View.GONE
                        isDoctorInfoRequired = false
                        return
                    }
                    val drugReq = snapshot.getValue(DrugRequest::class.java)
                    if (drugReq != null && drugReq.status == "Pending") {
                        layoutDoctorInfo.visibility = View.VISIBLE
                        isDoctorInfoRequired = true
                    } else {
                        layoutDoctorInfo.visibility = View.GONE
                        isDoctorInfoRequired = false
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFullScreenImage(bitmap: Bitmap) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this)
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.setContentView(imageView)
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmOrderWithShop(orderId: String, selectedShopPhone: String, shopName: String) {
        val orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId)
        val updates = mutableMapOf<String, Any>(
            "status" to "Approved",
            "approvedByShopPhone" to selectedShopPhone,
            "approvalTimestamp" to System.currentTimeMillis()
        )

        if (isDoctorInfoRequired) {
            val drugRequestUpdates = mapOf(
                "kmcNumber" to etDoctorKmcNo.text.toString().trim(),
                "doctorSignatureUrl" to signatureBase64!!,
                "status" to "Uploaded"
            )
            orderRef.child("drugRequest").updateChildren(drugRequestUpdates)
        }

        orderRef.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Order confirmed! Proceeding to payment...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, PaymentMethodActivity::class.java)
            intent.putExtra("orderId", orderId)
            intent.putExtra("shopName", shopName)
            intent.putExtra("totalAmount", totalAmount)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to confirm order", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}
