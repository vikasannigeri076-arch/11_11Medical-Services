package com.example.a11_11medicalservies

import com.google.firebase.database.IgnoreExtraProperties
import java.io.Serializable

@IgnoreExtraProperties
data class OrderRequest(
    val id: String = "",
    val customerPhone: String = "",
    val customerName: String = "",
    val items: String = "",
    val prescriptionUrl: String? = null,
    val prescriptionUrls: Any? = null, // Can be List or Map
    val timestamp: Long = 0,
    val status: String = "Open", // Open, Approved, Confirmed, Dispatched, Completed
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val district: String = "",
    val searchRadius: Int = 10,
    val approvedByShopPhone: String? = null,
    val approvalTimestamp: Long? = null,
    val responses: Map<String, ShopResponse> = emptyMap(),
    val drugRequest: DrugRequest? = null,
    
    // Payment & Final Order Details
    val medicineAmount: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMethod: String = "", // COD or Razorpay
    val paymentStatus: String = "Pending", // Pending, Paid
    val deliveryStatus: String = "Pending" // Pending, Assigned, Out for Delivery, Delivered
) : Serializable {
    fun getPrescriptionUrlsList(): List<String> {
        val list = mutableListOf<String>()
        prescriptionUrl?.let { if (it.isNotBlank()) list.add(it) }
        when (prescriptionUrls) {
            is List<*> -> list.addAll(prescriptionUrls.filterIsInstance<String>())
            is Map<*, *> -> list.addAll(prescriptionUrls.values.filterIsInstance<String>())
        }
        return list.distinct()
    }
}

@IgnoreExtraProperties
data class DrugRequest(
    val shopPhone: String = "",
    val shopName: String = "",
    val status: String = "Pending", 
    val doctorSignatureUrl: String? = null,
    val kmcNumber: String? = null,
    val timestamp: Long = 0
) : Serializable

@IgnoreExtraProperties
data class MedicineBillItem(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val unit: String = "Single"
) : Serializable

@IgnoreExtraProperties
data class ShopResponse(
    val shopPhone: String = "",
    val shopName: String = "",
    val subtotal: Double = 0.0,
    val gst: Double = 0.0,
    val medicineTotal: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val distance: Double = 0.0,
    val total: Double = 0.0,
    val billImage: String? = null,
    val timestamp: Long = 0,
    val medicines: Any? = null // Can be List or Map
) : Serializable {
    fun getMedicinesList(): List<MedicineBillItem> {
        return when (val data = medicines) {
            is List<*> -> {
                data.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        MedicineBillItem(
                            name = item["name"] as? String ?: "",
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                            unit = item["unit"] as? String ?: "Single"
                        )
                    } else if (item is MedicineBillItem) item else null
                }
            }
            is Map<*, *> -> {
                data.values.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        MedicineBillItem(
                            name = item["name"] as? String ?: "",
                            price = (item["price"] as? Number)?.toDouble() ?: 0.0,
                            quantity = (item["quantity"] as? Number)?.toInt() ?: 1,
                            unit = item["unit"] as? String ?: "Single"
                        )
                    } else null
                }
            }
            else -> emptyList()
        }
    }
}

@IgnoreExtraProperties
data class PaymentRecord(
    val paymentId: String = "",
    val orderId: String = "",
    val customerPhone: String = "",
    val shopPhone: String = "",
    val amount: Double = 0.0,
    val status: String = "",
    val method: String = "",
    val timestamp: Long = 0,
    val razorpayPaymentId: String? = null
) : Serializable

@IgnoreExtraProperties
data class OrderNotification(
    val id: String = "",
    val shopPhone: String = "",
    val title: String = "",
    val message: String = "",
    val orderId: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false
) : Serializable
