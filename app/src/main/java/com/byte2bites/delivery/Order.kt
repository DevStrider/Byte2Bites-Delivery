package com.byte2bites.delivery

import java.io.Serializable

data class Order(
    val orderId: String = "",
    val buyerUid: String = "",
    val deliveryType: String = "",
    val deliveryFeeCents: Int = 0,
    val totalCents: Int = 0,
    val status: String = "",          // PENDING / READY / ACCEPTED / DELIVERED ...
    val timestamp: Long = 0L,
    val deliveredBy: String = "",
    val deliveredAt: Long = 0L,
    val sellerDeliver: String = "",   // "Yes" or "No"

    // Address fields (pulled from Buyers/{uid}/address)
    var apartmentNumber: String = "",
    var buildingName: String = "",
    var floorNumber: String = "",
    var streetName: String = "",

    // Display-only
    var buyerName: String = "Loading...",
    var sellerName: String = "Loading..."
) : Serializable