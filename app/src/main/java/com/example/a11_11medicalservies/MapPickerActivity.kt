package com.example.a11_11medicalservies

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private lateinit var etSearchLocation: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var tvCurrentAddress: TextView
    private lateinit var btnConfirmLocation: Button
    private var selectedAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        etSearchLocation = findViewById(R.id.etSearchLocation)
        btnSearch = findViewById(R.id.btnSearch)
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress)
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnSearch.setOnClickListener {
            searchLocation()
        }

        etSearchLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                true
            } else false
        }

        btnConfirmLocation.setOnClickListener {
            selectedLatLng?.let {
                val resultIntent = Intent()
                resultIntent.putExtra("lat", it.latitude)
                resultIntent.putExtra("lng", it.longitude)
                resultIntent.putExtra("address", selectedAddress)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        val defaultLoc = LatLng(20.5937, 78.9629)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 5f))

        mMap.setOnCameraIdleListener {
            selectedLatLng = mMap.cameraPosition.target
            updateAddressText(selectedLatLng!!)
        }
    }

    private fun searchLocation() {
        val locationName = etSearchLocation.text.toString().trim()
        if (locationName.isNotEmpty()) {
            val geocoder = Geocoder(this, Locale.getDefault())
            try {
                val addressList = geocoder.getFromLocationName(locationName, 1)
                if (addressList != null && addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateAddressText(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressText = address.getAddressLine(0)
                selectedAddress = addressText
                tvCurrentAddress.text = addressText
            } else {
                tvCurrentAddress.text = "Address not found"
            }
        } catch (e: IOException) {
            tvCurrentAddress.text = "Error fetching address"
            e.printStackTrace()
        }
    }
}
