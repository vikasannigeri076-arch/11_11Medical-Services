package com.example.a11_11medicalservies

data class Customer(
    val name: String = "",
    val phone: String = "",
    val otp: String = "123456" // Default OTP if not set
)