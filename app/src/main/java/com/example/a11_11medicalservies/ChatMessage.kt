package com.example.a11_11medicalservies

data class AppChatMessage(
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isFromCustomer: Boolean = true,
    val imageData: String? = null
)
