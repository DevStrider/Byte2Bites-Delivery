package com.byte2bites.delivery

data class Delivery(
    val deliveryId: String = "",
    val orderId: String = "",
    val restaurantName: String? = null,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    val totalCents: Int = 0,
    val status: String = "AVAILABLE",   // AVAILABLE, ASSIGNED, PICKED_UP, DELIVERED
    val driverUid: String? = null,
    val timestamp: Long = 0L,
    val pointsEarned: Int = 0
)
