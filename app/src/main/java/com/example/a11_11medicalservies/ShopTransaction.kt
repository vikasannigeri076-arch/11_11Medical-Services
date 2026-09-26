package com.example.a11_11medicalservies

data class ShopTransaction(
    val orderId: String = "",
    val customerName: String = "",
    val totalAmount: Double = 0.0,
    val shopEarnings: Double = 0.0,
    val adminCommission: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val timestamp: Long = 0
)
