package com.byte2bites.delivery

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderDeliveryService {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // ===== ASSIGN ORDER (driver accepts while any status) =====

    fun assignOrder(
        order: Order,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("User not logged in")
            return
        }

        val updates = mapOf(
            "deliveredBy" to currentUser.uid
        )

        database.getReference("orders").child(order.orderId)
            .updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.d("OrderDelivery", "Root /orders failed, trying sellers: ${e.message}")
                findAndUpdateOrderInSellers(
                    orderId = order.orderId,
                    orderUpdates = updates,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
    }

    // ===== START DELIVERY (READY FOR DELIVERING → DELIVERING) =====

    fun startDelivery(
        order: Order,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("User not logged in")
            return
        }

        if (order.deliveredBy != currentUser.uid) {
            onFailure("This order is not assigned to you")
            return
        }

        val updates = mapOf(
            "status" to "DELIVERING"
        )

        database.getReference("orders").child(order.orderId)
            .updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.d("OrderDelivery", "Root /orders failed, trying sellers: ${e.message}")
                findAndUpdateOrderInSellers(
                    orderId = order.orderId,
                    orderUpdates = updates,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            }
    }

    // ===== MARK DELIVERED + POINTS (DELIVERING → DELIVERED) =====

    fun markOrderAsDelivered(
        order: Order,
        onSuccess: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("User not logged in")
            return
        }

        if (order.deliveredBy != currentUser.uid) {
            onFailure("This order is not assigned to you")
            return
        }

        val pointsEarned = PointsCalculator.calculatePoints(order.totalCents)

        val updates = mapOf(
            "status" to "DELIVERED",
            "deliveredAt" to System.currentTimeMillis()
        )

        database.getReference("orders").child(order.orderId)
            .updateChildren(updates)
            .addOnSuccessListener {
                updateUserPoints(
                    userId = currentUser.uid,
                    pointsToAdd = pointsEarned,
                    onSuccess = { onSuccess(pointsEarned) },
                    onFailure = onFailure
                )
            }
            .addOnFailureListener { e ->
                Log.d("OrderDelivery", "Root /orders failed, trying sellers: ${e.message}")
                findAndUpdateOrderInSellers(
                    orderId = order.orderId,
                    orderUpdates = updates,
                    onSuccess = {
                        updateUserPoints(
                            userId = currentUser.uid,
                            pointsToAdd = pointsEarned,
                            onSuccess = { onSuccess(pointsEarned) },
                            onFailure = onFailure
                        )
                    },
                    onFailure = onFailure
                )
            }
    }

    // ===== COMMON HELPER TO UPDATE UNDER /Sellers/*/orders =====

    private fun findAndUpdateOrderInSellers(
        orderId: String,
        orderUpdates: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sellersRef = database.getReference("Sellers")

        sellersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var found = false

                for (sellerSnapshot in snapshot.children) {
                    val sellerId = sellerSnapshot.key
                    val orderRef = sellerSnapshot.child("orders").child(orderId)

                    if (orderRef.exists()) {
                        found = true
                        orderRef.ref.updateChildren(orderUpdates)
                            .addOnSuccessListener {
                                Log.d("OrderDelivery", "Order updated in seller: $sellerId")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                onFailure("Failed to update order in seller $sellerId: ${e.message}")
                            }
                        return
                    }
                }

                if (!found) onFailure("Order not found under any seller.")
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure("Search cancelled: ${error.message}")
            }
        })
    }

    // ===== POINTS =====

    private fun updateUserPoints(
        userId: String,
        pointsToAdd: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val userRef = database.getReference("Buyers").child(userId).child("points")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentPoints = snapshot.getValue(Int::class.java) ?: 0
                val newPoints = currentPoints + pointsToAdd

                userRef.setValue(newPoints)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onFailure(e.message ?: "Failed to update points")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.message)
            }
        })
    }

    fun getUserPoints(
        userId: String,
        onPointsLoaded: (Int) -> Unit,
        onFailure: (String) -> Unit
    ) {
        database.getReference("Buyers").child(userId).child("points")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onPointsLoaded(snapshot.getValue(Int::class.java) ?: 0)
                }

                override fun onCancelled(error: DatabaseError) {
                    onFailure(error.message)
                }
            })
    }

    // ===== ACTIVE ORDER FOR DRIVER =====

    fun getCurrentActiveOrder(
        userId: String,
        onOrderLoaded: (Order?) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val sellersRef = database.getReference("Sellers")

        sellersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var activeOrder: Order? = null

                for (sellerSnapshot in snapshot.children) {
                    val sellerName =
                        sellerSnapshot.child("name").getValue(String::class.java) ?: "Unknown Seller"
                    val ordersRef = sellerSnapshot.child("orders")

                    for (orderSnapshot in ordersRef.children) {
                        val deliveredBy =
                            orderSnapshot.child("deliveredBy").getValue(String::class.java)
                        val status =
                            orderSnapshot.child("status").getValue(String::class.java) ?: ""

                        if (deliveredBy == userId && status != "DELIVERED") {
                            activeOrder = parseOrderFromSnapshot(orderSnapshot)
                            activeOrder?.sellerName = sellerName
                            break
                        }
                    }
                    if (activeOrder != null) break
                }

                onOrderLoaded(activeOrder)
            }

            override fun onCancelled(error: DatabaseError) {
                onFailure(error.message)
            }
        })
    }

    // ===== PARSING =====

    private fun parseOrderFromSnapshot(orderSnapshot: DataSnapshot): Order? {
        return try {
            val totalCents = when {
                orderSnapshot.child("totalCents").getValue(Long::class.java) != null ->
                    (orderSnapshot.child("totalCents").getValue(Long::class.java) ?: 0L).toInt()
                else ->
                    orderSnapshot.child("totalCents").getValue(Int::class.java) ?: 0
            }

            val deliveryFeeCents = when {
                orderSnapshot.child("deliveryFeeCents").getValue(Long::class.java) != null ->
                    (orderSnapshot.child("deliveryFeeCents").getValue(Long::class.java) ?: 0L).toInt()
                else ->
                    orderSnapshot.child("deliveryFeeCents").getValue(Int::class.java) ?: 0
            }

            Order(
                orderId = orderSnapshot.child("orderId").getValue(String::class.java)
                    ?: orderSnapshot.key.orEmpty(),
                buyerUid = orderSnapshot.child("buyerUid").getValue(String::class.java) ?: "",
                deliveryType = orderSnapshot.child("deliveryType").getValue(String::class.java) ?: "",
                deliveryFeeCents = deliveryFeeCents,
                totalCents = totalCents,
                status = orderSnapshot.child("status").getValue(String::class.java) ?: "PENDING",
                timestamp = orderSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L,
                deliveredBy = orderSnapshot.child("deliveredBy").getValue(String::class.java) ?: "",
                deliveredAt = orderSnapshot.child("deliveredAt").getValue(Long::class.java) ?: 0L
            )
        } catch (e: Exception) {
            Log.e("OrderDelivery", "Error parsing order: ${e.message}")
            null
        }
    }
}
