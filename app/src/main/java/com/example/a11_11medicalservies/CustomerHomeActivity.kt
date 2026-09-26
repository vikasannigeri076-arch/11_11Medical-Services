package com.example.a11_11medicalservies

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

class CustomerHomeActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var rvPrescriptions: RecyclerView
    private lateinit var btnUpload: Button
    private lateinit var btnNext: Button
    private lateinit var fabCamera: FloatingActionButton
    private lateinit var fabChatbot: FloatingActionButton
    private lateinit var etMedicine: EditText
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0
    private var userName: String = "User"
    private val prescriptionBase64List = mutableListOf<String>()
    private lateinit var prescriptionAdapter: PrescriptionPreviewAdapter

    private val districts = arrayOf(
        "Bagalkot", "Ballari (Bellary)", "Belagavi (Belgaum)", "Bengaluru (Bangalore) Rural",
        "Bengaluru (Bangalore) Urban", "Bidar", "Chamarajanagar", "Chikkaballapur",
        "Chikkamagaluru (Chikmagalur)", "Chitradurga", "Dakshina Kannada", "Davanagere",
        "Dharwad", "Gadag", "Hassan", "Haveri", "Kalaburagi (Gulbarga)", "Kodagu",
        "Kolar", "Koppal", "Mandya", "Mysuru (Mysore)", "Raichur", "Ramanagara",
        "Shivamogga (Shimoga)", "Tumakuru (Tumkur)", "Udupi", "Uttara Kannada (Karwar)",
        "Vijayapura (Bijapur)", "Yadgir", "Vijayanagara"
    )

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(it)
            bitmap?.let { b ->
                val base64 = bitmapToBase64(b)
                prescriptionBase64List.add(base64)
                updatePrescriptionUI()
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                val base64 = bitmapToBase64(it)
                prescriptionBase64List.add(base64)
                updatePrescriptionUI()
            }
        }
    }

    private val mapPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val lat = result.data?.getDoubleExtra("lat", 0.0) ?: 0.0
            val lng = result.data?.getDoubleExtra("lng", 0.0) ?: 0.0
            val address = result.data?.getStringExtra("address") ?: ""
            
            if (lat != 0.0 && lng != 0.0) {
                userLat = lat
                userLng = lng
                tvAddress.text = address
                updateUserLocationInFirebase(lat, lng, address)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera()
    }

    private val locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_customer_home)
        
        tvUserName = findViewById(R.id.tvUserName)
        tvAddress = findViewById(R.id.tvAddress)
        rvPrescriptions = findViewById(R.id.rvPrescriptions)
        btnUpload = findViewById(R.id.btnCamera)
        btnNext = findViewById(R.id.btnNext)
        fabCamera = findViewById(R.id.fabCamera)
        fabChatbot = findViewById(R.id.fabChatbot)
        etMedicine = findViewById(R.id.etMedicine)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        setupPrescriptionRecyclerView()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    if (userLat == 0.0) { 
                        userLat = it.latitude
                        userLng = it.longitude
                        updateLocationInfo(it.latitude, it.longitude)
                    }
                }
            }
        }

        fetchUserData()
        checkLocationPermissions()

        btnUpload.setOnClickListener { galleryLauncher.launch("image/*") }
        fabCamera.setOnClickListener { checkCameraPermission() }

        btnNext.setOnClickListener {
            handleSearchClick()
        }

        tvAddress.setOnClickListener {
            val intent = Intent(this, MapPickerActivity::class.java)
            mapPickerLauncher.launch(intent)
        }

        fabChatbot.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_orders -> { startActivity(Intent(this, CustomerOrdersActivity::class.java)); false }
                R.id.nav_settings -> { startActivity(Intent(this, CustomerSettingsActivity::class.java)); false }
                else -> false
            }
        }
        setupMedicineAddButtons()
    }

    private fun setupPrescriptionRecyclerView() {
        prescriptionAdapter = PrescriptionPreviewAdapter(prescriptionBase64List) { position ->
            prescriptionBase64List.removeAt(position)
            updatePrescriptionUI()
        }
        rvPrescriptions.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvPrescriptions.adapter = prescriptionAdapter
    }

    private fun updatePrescriptionUI() {
        if (prescriptionBase64List.isEmpty()) {
            rvPrescriptions.visibility = View.GONE
        } else {
            rvPrescriptions.visibility = View.VISIBLE
            prescriptionAdapter.notifyDataSetChanged()
        }
    }

    private fun updateUserLocationInFirebase(lat: Double, lng: Double, address: String) {
        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: return
        val updates = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "address" to address
        )
        database.getReference("Customers").child(phone).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Location updated successfully", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleSearchClick() {
        val items = etMedicine.text.toString().trim()
        val hasPrescription = prescriptionBase64List.isNotEmpty()
        
        if (items.isEmpty() && !hasPrescription) {
            Toast.makeText(this, "Please enter medicine names or upload photo", Toast.LENGTH_SHORT).show()
            return
        }

        showDistrictSelectionDialog()
    }

    private fun showDistrictSelectionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select your District")
            .setItems(districts) { _, which ->
                val selectedDistrict = districts[which]
                sendOrderRequest(selectedDistrict, prescriptionBase64List)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendOrderRequest(district: String, photos: List<String>) {
        val items = etMedicine.text.toString().trim()
        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: ""
        val orderId = database.getReference("Orders").push().key ?: return
        
        val order = OrderRequest(
            id = orderId,
            customerPhone = phone,
            customerName = userName,
            items = items,
            prescriptionUrl = if (photos.isNotEmpty()) photos[0] else null,
            prescriptionUrls = photos,
            timestamp = System.currentTimeMillis(),
            status = "Open",
            latitude = userLat,
            longitude = userLng,
            district = district
        )

        database.getReference("Orders").child(orderId).setValue(order)
            .addOnSuccessListener {
                Toast.makeText(this, "Order sent to shops in $district!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, AvailableShopsActivity::class.java)
                intent.putExtra("orderId", orderId)
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 40, baos)
        val byteArray = baos.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchUserData() {
        val phone = auth.currentUser?.phoneNumber?.replace("+91", "") ?: return
        database.getReference("Customers").child(phone).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userName = snapshot.child("name").getValue(String::class.java) ?: "User"
                tvUserName.text = "Hello, $userName"
                
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                val addr = snapshot.child("address").getValue(String::class.java)
                
                if (lat != 0.0 && lng != 0.0) {
                    userLat = lat
                    userLng = lng
                    if (addr != null) {
                        tvAddress.text = addr
                    } else {
                        updateLocationInfo(lat, lng)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(req, locationCallback, null)
        }
    }

    private fun updateLocationInfo(lat: Double, lng: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                tvAddress.text = addresses[0].getAddressLine(0)
            }
        } catch (e: Exception) {}
    }

    private fun setupMedicineAddButtons() {
        val medicineMap = mapOf(
            R.id.btn_add_amlodipine to "Amlodipine (BP)",
            R.id.btn_add_losartan to "Losartan (BP)",
            R.id.btn_add_telmisartan to "Telmisartan (BP)",
            R.id.btn_add_metformin to "Metformin",
            R.id.btn_add_glimepiride to "Glimepiride",
            R.id.btn_add_atorvastatin to "Atorvastatin",
            R.id.btn_add_aspirin to "Aspirin",
            R.id.btn_add_pantoprazole to "Pantoprazole",
            R.id.btn_add_paracetamol to "Paracetamol"
        )

        for ((btnId, name) in medicineMap) {
            findViewById<Button>(btnId).setOnClickListener { showQuantityDialog(name) }
        }
    }

    private fun showQuantityDialog(medicineName: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quantity_picker, null)
        val tvMedName = dialogView.findViewById<TextView>(R.id.tvDialogMedName)
        val rgUnit = dialogView.findViewById<RadioGroup>(R.id.rgUnit)
        val btnMinus = dialogView.findViewById<Button>(R.id.btnMinus)
        val btnPlus = dialogView.findViewById<Button>(R.id.btnPlus)
        val tvQuantity = dialogView.findViewById<TextView>(R.id.tvQuantity)
        tvMedName.text = medicineName
        var quantity = 1
        btnMinus.setOnClickListener { if (quantity > 1) { quantity--; tvQuantity.text = quantity.toString() } }
        btnPlus.setOnClickListener { quantity++; tvQuantity.text = quantity.toString() }

        AlertDialog.Builder(this).setView(dialogView).setPositiveButton("Add") { _, _ ->
            val selectedUnit = dialogView.findViewById<RadioButton>(rgUnit.checkedRadioButtonId)?.text?.toString() ?: "Single"
            addMedicineToBox(medicineName, quantity, selectedUnit)
        }.setNegativeButton("Cancel", null).show()
    }

    private fun addMedicineToBox(name: String, quantity: Int, unit: String) {
        val entry = "\n$name ($quantity $unit)"
        etMedicine.append(entry)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        try { cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}

class PrescriptionPreviewAdapter(
    private val images: List<String>,
    private val onRemove: (Int) -> Unit
) : RecyclerView.Adapter<PrescriptionPreviewAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPreview: ImageView = view.findViewById(R.id.ivPreview)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prescription_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val base64 = images[position]
        try {
            val decodedString = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            holder.ivPreview.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        holder.btnRemove.setOnClickListener {
            onRemove(holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount() = images.size
}
