package com.example.a11_11medicalservies

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*

class OrderTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var orderId: String? = null
    private var orderRef: DatabaseReference? = null
    private var orderListener: ValueEventListener? = null

    private lateinit var tvLiveStatus: TextView
    private lateinit var tvStatusDescription: TextView
    private lateinit var tvEta: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvDriverName: TextView
    private lateinit var tvVehicleNumber: TextView
    private lateinit var tvShopName: TextView
    private lateinit var tvOrderId: TextView
    private lateinit var tvMedicineAmount: TextView
    private lateinit var tvDeliveryCharge: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvPaymentStatus: TextView
    private lateinit var tvOrderSummaryTitle: TextView
    private lateinit var ivDriverPhoto: ImageView
    private lateinit var btnCallDriver: MaterialButton
    private lateinit var cvDeliveryPartner: MaterialCardView
    private lateinit var cvEta: MaterialCardView
    
    private lateinit var step1: View
    private lateinit var step2: View
    private lateinit var step3: View
    private lateinit var step4: View
    private lateinit var step5: View

    private var customerMarker: Marker? = null
    private var shopMarker: Marker? = null
    private var driverMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var driverPhone: String? = null
    
    private var isInitialZoomDone = false
    private var destinationLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_tracking)

        orderId = intent.getStringExtra("orderId")
        if (orderId == null) {
            Toast.makeText(this, getString(R.string.order_id_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        handleWindowInsets()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        
        btnCallDriver.setOnClickListener {
            driverPhone?.let {
                val intent = Intent(Intent.ACTION_DIAL, "tel:$it".toUri())
                startActivity(intent)
            } ?: Toast.makeText(this, getString(R.string.driver_contact_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        tvLiveStatus = findViewById(R.id.tvLiveStatus)
        tvStatusDescription = findViewById(R.id.tvStatusDescription)
        tvEta = findViewById(R.id.tvEta)
        tvDistance = findViewById(R.id.tvDistance)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvVehicleNumber = findViewById(R.id.tvVehicleNumber)
        tvShopName = findViewById(R.id.tvShopName)
        tvOrderId = findViewById(R.id.tvOrderId)
        tvMedicineAmount = findViewById(R.id.tvMedicineAmount)
        tvDeliveryCharge = findViewById(R.id.tvDeliveryCharge)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        tvOrderSummaryTitle = findViewById(R.id.tvOrderSummaryTitle)
        ivDriverPhoto = findViewById(R.id.ivDriverPhoto)
        btnCallDriver = findViewById(R.id.btnCallDriver)
        cvDeliveryPartner = findViewById(R.id.cvDeliveryPartner)
        cvEta = findViewById(R.id.cvEta)
        
        step1 = findViewById(R.id.step1)
        step2 = findViewById(R.id.step2)
        step3 = findViewById(R.id.step3)
        step4 = findViewById(R.id.step4)
        step5 = findViewById(R.id.step5)
    }

    private fun handleWindowInsets() {
        // Adjust UI for system bars (status bar)
        ViewCompat.setOnApplyWindowInsetsListener(cvEta) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + (40 * resources.displayMetrics.density).toInt()
            }
            insets
        }
        
        val backBtn = findViewById<View>(R.id.btnBack)
        val container = backBtn?.parent as? View
        if (container != null) {
            ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = systemBars.top + (40 * resources.displayMetrics.density).toInt()
                }
                insets
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // --- ZOOM CONTROLS ---
        mMap.uiSettings.isZoomControlsEnabled = true 
        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = false
        
        val peekHeight = (280 * resources.displayMetrics.density).toInt()
        mMap.setPadding(0, 0, 0, peekHeight)
        
        listenToOrder()
    }

    private fun listenToOrder() {
        orderRef = FirebaseDatabase.getInstance().getReference("Orders").child(orderId!!)
        orderListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val status = snapshot.child("status").getValue(String::class.java)
                val shopPhone = snapshot.child("approvedByShopPhone").getValue(String::class.java)
                
                // --- Fetch Pricing & Shop Info from the specific Shop Response ---
                var medAmount = 0.0
                var delCharge = 0.0
                var total = 0.0
                var displayShopName = snapshot.child("drugRequest/shopName").getValue(String::class.java)

                if (shopPhone != null) {
                    val shopRespSnap = snapshot.child("responses").child(shopPhone)
                    if (shopRespSnap.exists()) {
                        medAmount = shopRespSnap.child("medicineTotal").getValue(Double::class.java) ?: 0.0
                        delCharge = shopRespSnap.child("deliveryCharge").getValue(Double::class.java) ?: 0.0
                        total = shopRespSnap.child("total").getValue(Double::class.java) ?: 0.0
                        if (displayShopName == null) {
                            displayShopName = shopRespSnap.child("shopName").getValue(String::class.java)
                        }
                    }
                }

                if (total == 0.0) {
                    medAmount = snapshot.child("medicineAmount").getValue(Double::class.java) ?: 0.0
                    delCharge = snapshot.child("deliveryCharge").getValue(Double::class.java) ?: 0.0
                    total = snapshot.child("grandTotal").getValue(Double::class.java) ?: 0.0
                }
                
                val payMethod = snapshot.child("paymentMethod").getValue(String::class.java) ?: "N/A"
                val payStatus = snapshot.child("paymentStatus").getValue(String::class.java) ?: "Pending"
                val customerLat = snapshot.child("latitude").getValue(Double::class.java)
                val customerLng = snapshot.child("longitude").getValue(Double::class.java)

                updateOrderUI(status, displayShopName, medAmount, delCharge, total, payMethod, payStatus)
                updateStatusSteps(status)
                
                if (customerLat != null && customerLng != null) {
                    destinationLatLng = LatLng(customerLat, customerLng)
                    updateMarker(1, destinationLatLng!!, getString(R.string.you_label))
                }

                shopPhone?.let { fetchShopDetails(it) }

                // --- Handle Delivery Partner & Real-time Distance/ETA ---
                val delivery = snapshot.child("delivery")
                if (delivery.exists()) {
                    cvDeliveryPartner.visibility = View.VISIBLE
                    val name = delivery.child("name").getValue(String::class.java) ?: getString(R.string.delivery_partner_label)
                    val vehicle = delivery.child("vehicleNumber").getValue(String::class.java) ?: "N/A"
                    val photo = delivery.child("photoUrl").getValue(String::class.java)
                    driverPhone = delivery.child("phone").getValue(String::class.java)
                    val dLat = delivery.child("latitude").getValue(Double::class.java)
                    val dLng = delivery.child("longitude").getValue(Double::class.java)

                    tvDriverName.text = name
                    tvVehicleNumber.text = vehicle
                    photo?.let {
                        Glide.with(this@OrderTrackingActivity).load(it).placeholder(R.drawable.applogo1).into(ivDriverPhoto)
                    }

                    if (dLat != null && dLng != null) {
                        val driverLatLng = LatLng(dLat, dLng)
                        animateDriverMarker(driverLatLng)
                        if (destinationLatLng != null) {
                            calculateEtaAndDistance(driverLatLng, destinationLatLng!!)
                        }
                    }
                } else {
                    cvDeliveryPartner.visibility = View.GONE
                    tvLiveStatus.text = getString(R.string.waiting_for_driver)
                    
                    // If no driver assigned, show distance from Shop to Customer
                    if (destinationLatLng != null && shopMarker != null) {
                        calculateEtaAndDistance(shopMarker!!.position, destinationLatLng!!)
                    }
                }
                
                // Auto-zoom only initially to allow user manual control later
                if (!isInitialZoomDone) {
                    zoomToFit()
                    if (customerMarker != null && (shopMarker != null || driverMarker != null)) {
                        isInitialZoomDone = true
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        orderRef?.addValueEventListener(orderListener!!)
    }

    private fun updateOrderUI(status: String?, shopName: String?, med: Double, del: Double, total: Double, method: String, pStatus: String) {
        val shortId = if (orderId!!.length > 8) orderId!!.substring(orderId!!.length - 8) else orderId!!
        tvOrderSummaryTitle.text = getString(R.string.order_details_title, shortId)
        tvOrderId.text = getString(R.string.order_id_label, shortId)
        tvShopName.text = shopName ?: getString(R.string.pharmacy_label)
        
        tvMedicineAmount.text = getString(R.string.amount_format, med)
        tvDeliveryCharge.text = getString(R.string.amount_format, del)
        tvGrandTotal.text = getString(R.string.amount_format, total)
        
        tvPaymentMethod.text = method
        tvPaymentStatus.text = pStatus

        if (status != null) {
            tvLiveStatus.text = status
            val msgRes = when (status) {
                "Confirmed" -> R.string.order_confirmed_msg
                "Approved" -> R.string.order_approved_msg
                "Picked Up" -> R.string.order_picked_msg
                "Out for Delivery" -> R.string.order_on_way_msg
                "Completed" -> R.string.order_delivered_msg
                else -> R.string.processing_order
            }
            tvStatusDescription.text = getString(msgRes)
        }
    }

    private fun updateStatusSteps(status: String?) {
        val activeColor = "#4CAF50".toColorInt()
        val inactiveColor = "#E0E0E0".toColorInt()
        
        listOf(step1, step2, step3, step4, step5).forEach { it.setBackgroundColor(inactiveColor) }

        if (status == null) return
        
        step1.setBackgroundColor(activeColor)
        if (status in listOf("Approved", "Picked Up", "Out for Delivery", "Completed")) step2.setBackgroundColor(activeColor)
        if (status in listOf("Picked Up", "Out for Delivery", "Completed")) step3.setBackgroundColor(activeColor)
        if (status in listOf("Out for Delivery", "Completed")) step4.setBackgroundColor(activeColor)
        if (status == "Completed") step5.setBackgroundColor(activeColor)
    }

    private fun fetchShopDetails(phone: String) {
        FirebaseDatabase.getInstance().getReference("Shops").child(phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("latitude").getValue(Double::class.java)
                    val lng = snapshot.child("longitude").getValue(Double::class.java)
                    val name = snapshot.child("shopName").getValue(String::class.java) ?: getString(R.string.pharmacy_label)
                    
                    if (lat != null && lng != null) {
                        updateMarker(2, LatLng(lat, lng), name)
                        drawRoute()
                        
                        // Update distance immediately if no driver yet
                        if (driverMarker == null && destinationLatLng != null) {
                            calculateEtaAndDistance(LatLng(lat, lng), destinationLatLng!!)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun calculateEtaAndDistance(source: LatLng, dest: LatLng) {
        val results = FloatArray(1)
        Location.distanceBetween(source.latitude, source.longitude,
                dest.latitude, dest.longitude, results)
        val distanceInMeters = results[0]
        
        cvEta.visibility = View.VISIBLE
        tvDistance.text = getString(R.string.distance_format, distanceInMeters / 1000.0)
        
        var etaMinutes = (distanceInMeters / 333.0).toInt() 
        if (etaMinutes < 2) etaMinutes = 1
        
        tvEta.text = getString(R.string.eta_format, etaMinutes)
    }

    private fun animateDriverMarker(destination: LatLng) {
        if (driverMarker == null) {
            driverMarker = mMap.addMarker(MarkerOptions()
                .position(destination)
                .title(getString(R.string.delivery_partner_label))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
            return
        }

        val startPosition = driverMarker!!.position
        val handler = Handler(Looper.getMainLooper())
        val start = SystemClock.uptimeMillis()
        val interpolator = AccelerateDecelerateInterpolator()
        val duration = 1500f

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed / duration)
                val lat = t * destination.latitude + (1 - t) * startPosition.latitude
                val lng = t * destination.longitude + (1 - t) * startPosition.longitude
                driverMarker!!.position = LatLng(lat, lng)
                
                if (t < 1.0) {
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    private fun updateMarker(type: Int, pos: LatLng, title: String) {
        when(type) {
            1 -> { // Customer
                if (customerMarker == null) {
                    customerMarker = mMap.addMarker(MarkerOptions()
                        .position(pos)
                        .title(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
                } else {
                    customerMarker!!.position = pos
                }
            }
            2 -> { // Shop
                if (shopMarker == null) {
                    shopMarker = mMap.addMarker(MarkerOptions()
                        .position(pos)
                        .title(title)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                } else {
                    shopMarker!!.position = pos
                }
            }
        }
    }

    private fun drawRoute() {
        if (shopMarker != null && destinationLatLng != null) {
            routePolyline?.remove()
            routePolyline = mMap.addPolyline(PolylineOptions()
                .add(shopMarker!!.position, destinationLatLng!!)
                .width(12f)
                .color("#4A90E2".toColorInt())
                .geodesic(true)
                .jointType(JointType.ROUND))
        }
    }

    private fun zoomToFit() {
        if (!::mMap.isInitialized) return
        val builder = LatLngBounds.Builder()
        var pointsAdded = false
        
        customerMarker?.let { builder.include(it.position); pointsAdded = true }
        shopMarker?.let { builder.include(it.position); pointsAdded = true }
        driverMarker?.let { builder.include(it.position); pointsAdded = true }

        if (pointsAdded) {
            val bounds = builder.build()
            val padding = 200
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orderListener?.let { orderRef?.removeEventListener(it) }
    }
}
