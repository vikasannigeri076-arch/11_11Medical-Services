package com.example.a11_11medicalservies

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream

class ShopBillActivity : AppCompatActivity() {

    private lateinit var layoutMedicineList: LinearLayout
    private lateinit var layoutPrescriptionBox: View
    private lateinit var ivCustomerSlip: ImageView
    private lateinit var etPrescriptionPrice: EditText
    private lateinit var tvSubtotal: TextView
    private lateinit var etGst: EditText
    private lateinit var tvTotalAmount: TextView
    private lateinit var ivShopBillPreview: ImageView
    private lateinit var fabCaptureBill: FloatingActionButton
    private lateinit var btnUploadBill: Button
    private lateinit var btnSendRequest: Button

    private var orderId: String? = null
    private var shopPhone: String? = null
    private var shopName: String? = null
    private var shopBillBase64: String? = null
    private var prescriptionData: String? = null
    
    private val medicineItems = mutableListOf<MedicineBillItem>()
    
    private var customerLat: Double = 0.0
    private var customerLng: Double = 0.0
    private var shopLat: Double = 0.0
    private var shopLng: Double = 0.0
    private var deliveryCharge: Double = 0.0
    private var distanceInKm: Double = 0.0

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                val resized = resizeBitmap(it, 600)
                ivShopBillPreview.setImageBitmap(resized)
                shopBillBase64 = bitmapToBase64(resized)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shop_bill)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        layoutMedicineList = findViewById(R.id.layoutMedicineList)
        layoutPrescriptionBox = findViewById(R.id.layoutPrescriptionBox)
        ivCustomerSlip = findViewById(R.id.ivCustomerSlip)
        etPrescriptionPrice = findViewById(R.id.etPrescriptionPrice)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        etGst = findViewById(R.id.etGst)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        ivShopBillPreview = findViewById(R.id.ivShopBillPreview)
        fabCaptureBill = findViewById(R.id.fabCaptureBill)
        btnUploadBill = findViewById(R.id.btnUploadBill)
        btnSendRequest = findViewById(R.id.btnSendRequest)

        orderId = intent.getStringExtra("orderId")
        shopPhone = intent.getStringExtra("shopPhone")
        shopName = intent.getStringExtra("shopName")
        val requirements = intent.getStringExtra("medicineRequirements") ?: ""
        prescriptionData = intent.getStringExtra("prescriptionUrl")
        
        // We need customer location from Order
        fetchOrderAndShopLocations()

        setupMedicineRows(requirements)
        setupPrescriptionView()

        etGst.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { calculateTotal() }
            override fun afterTextChanged(s: Editable?) {}
        })

        fabCaptureBill.setOnClickListener {
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }

        btnUploadBill.setOnClickListener {
            uploadFinalBill()
        }

        btnSendRequest.setOnClickListener {
            sendDoctorRequest()
        }
    }

    private fun sendDoctorRequest() {
        val oId = orderId
        val sPhone = shopPhone
        val sName = shopName

        if (oId != null && sPhone != null && sName != null) {
            val drugRequest = DrugRequest(
                shopPhone = sPhone,
                shopName = sName,
                status = "Pending",
                timestamp = System.currentTimeMillis()
            )

            FirebaseDatabase.getInstance().getReference("Orders")
                .child(oId)
                .child("drugRequest")
                .setValue(drugRequest)
                .addOnSuccessListener {
                    Toast.makeText(this, "Doctor request sent to customer", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send request: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Missing order or shop information", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchOrderAndShopLocations() {
        if (orderId == null || shopPhone == null) return
        
        val db = FirebaseDatabase.getInstance()
        
        // Fetch Order for Customer Location
        db.getReference("Orders").child(orderId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                customerLat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                customerLng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                
                // Fetch Shop for Shop Location
                db.getReference("Shops").child(shopPhone!!).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(shopSnap: DataSnapshot) {
                        shopLat = shopSnap.child("latitude").getValue(Double::class.java) ?: 0.0
                        shopLng = shopSnap.child("longitude").getValue(Double::class.java) ?: 0.0
                        
                        calculateDistanceAndDelivery()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun calculateDistanceAndDelivery() {
        if (customerLat == 0.0 || shopLat == 0.0) return
        
        val results = FloatArray(1)
        Location.distanceBetween(shopLat, shopLng, customerLat, customerLng, results)
        val distanceInMeters = results[0]
        distanceInKm = (distanceInMeters / 1000).toDouble()
        
        // Delivery Charge = Distance (in km) × ₹10 per km
        deliveryCharge = distanceInKm * 10.0
        
        calculateTotal()
    }

    private fun setupMedicineRows(requirements: String) {
        val medicines = requirements.split("\n").filter { it.isNotBlank() }
        val inflater = LayoutInflater.from(this)

        for (med in medicines) {
            val view = inflater.inflate(R.layout.item_medicine_bill_row, layoutMedicineList, false)
            val tvName = view.findViewById<TextView>(R.id.tvMedicineName)
            val etPrice = view.findViewById<EditText>(R.id.etMedicinePrice)

            val cleanName = med.trim()
            tvName.text = cleanName
            
            etPrice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val price = s.toString().toDoubleOrNull() ?: 0.0
                    updateMedicinePrice(cleanName, price)
                    calculateTotal()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            layoutMedicineList.addView(view)
        }
    }

    private fun updateMedicinePrice(name: String, price: Double) {
        val existing = medicineItems.find { it.name == name }
        if (existing != null) {
            medicineItems.remove(existing)
        }
        medicineItems.add(MedicineBillItem(name, price))
    }

    private fun setupPrescriptionView() {
        if (!prescriptionData.isNullOrEmpty()) {
            layoutPrescriptionBox.visibility = View.VISIBLE
            try {
                val decodedString = Base64.decode(prescriptionData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                ivCustomerSlip.setImageBitmap(bitmap)
                
                ivCustomerSlip.setOnClickListener {
                    showFullScreenImage(bitmap)
                }
            } catch (e: Exception) {}

            etPrescriptionPrice.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    calculateTotal()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
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

    private fun calculateTotal() {
        var medicineSubtotal = 0.0
        for (item in medicineItems) {
            medicineSubtotal += item.price
        }
        
        val prescriptionPrice = etPrescriptionPrice.text.toString().toDoubleOrNull() ?: 0.0
        medicineSubtotal += prescriptionPrice

        tvSubtotal.text = "₹ %.2f".format(medicineSubtotal)

        val gstPercent = etGst.text.toString().toDoubleOrNull() ?: 0.0
        val medicineTotalWithGst = medicineSubtotal + (medicineSubtotal * gstPercent / 100.0)
        
        val finalTotal = medicineTotalWithGst + deliveryCharge
        tvTotalAmount.text = "₹ %.2f (Incl. ₹%.2f Delivery)".format(finalTotal, deliveryCharge)
    }

    private fun uploadFinalBill() {
        val subtotalStr = tvSubtotal.text.toString().replace("₹ ", "").replace(",", "")
        val subtotal = subtotalStr.toDoubleOrNull() ?: 0.0
        
        if (subtotal <= 0) {
            Toast.makeText(this, "Total amount cannot be zero", Toast.LENGTH_SHORT).show()
            return
        }

        val gst = etGst.text.toString().toDoubleOrNull() ?: 0.0
        val medicineTotalWithGst = subtotal + (subtotal * gst / 100.0)
        val finalTotal = medicineTotalWithGst + deliveryCharge

        val response = ShopResponse(
            shopPhone = shopPhone ?: "",
            shopName = shopName ?: "",
            subtotal = subtotal,
            gst = gst,
            medicineTotal = medicineTotalWithGst,
            deliveryCharge = deliveryCharge,
            distance = distanceInKm,
            total = finalTotal,
            billImage = shopBillBase64,
            timestamp = System.currentTimeMillis(),
            medicines = medicineItems
        )

        val oId = orderId
        val sPhone = shopPhone

        if (oId != null && sPhone != null) {
            FirebaseDatabase.getInstance().getReference("Orders")
                .child(oId)
                .child("responses")
                .child(sPhone)
                .setValue(response)
                .addOnSuccessListener {
                    val intent = Intent(this, BillUploadSuccessActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}
