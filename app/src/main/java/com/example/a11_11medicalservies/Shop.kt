package com.example.a11_11medicalservies

data class Shop(
    val shopName: String = "",
    val ownerName: String = "",
    val mobile: String = "",
    val gst: String = "",
    val pincode: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val district: String = ""
)