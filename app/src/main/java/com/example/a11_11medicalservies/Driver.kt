package com.example.a11_11medicalservies

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Driver(
    val phone: String = "",
    val name: String = "",
    val vehicleNumber: String = "",
    val photoUrl: String? = null,
    val status: String = "Available", // Available, Busy, Offline
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 5.0
)
