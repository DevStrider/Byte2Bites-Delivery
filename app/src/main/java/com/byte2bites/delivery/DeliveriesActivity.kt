package com.byte2bites.delivery

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DeliveriesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var adapter: DeliveriesAdapter
    private lateinit var orderDeliveryService: OrderDeliveryService
    private val ordersList = mutableListOf<Order>()
    private var hasActiveOrder = false   // for info only

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deliveries)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        orderDeliveryService = OrderDeliveryService()

        initViews()
        setupProfileButton()
        checkForActiveOrder()
        loadOrdersFromSellers()
    }

    private fun initViews() {
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView)
        ordersRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = DeliveriesAdapter(
            orders = ordersList,
            currentUserId = auth.currentUser?.uid,
            onOrderClick = { showOrderDetails(it) },
            onAcceptOrder = { acceptOrder(it) },
            onStartDelivery = { startDelivery(it) },
            onDeliverOrder = { deliverOrder(it) }
        )
        ordersRecyclerView.adapter = adapter
    }

    private fun setupProfileButton() {
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun checkForActiveOrder() {
        val user = auth.currentUser ?: return
        orderDeliveryService.getCurrentActiveOrder(
            userId = user.uid,
            onOrderLoaded = { order ->
                hasActiveOrder = order != null
                adapter.notifyDataSetChanged()
            },
            onFailure = {
                hasActiveOrder = false
                adapter.notifyDataSetChanged()
            }
        )
    }

    // ===== LOAD ORDERS FROM /Sellers =====

    private fun loadOrdersFromSellers() {
        val sellersRef = database.getReference("Sellers")

        sellersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()
                for (sellerSnapshot in snapshot.children) {
                    val sellerName =
                        sellerSnapshot.child("name").getValue(String::class.java) ?: "Unknown Seller"
                    val ordersRef = sellerSnapshot.child("orders")

                    for (orderSnapshot in ordersRef.children) {
                        val order = parseOrderFromSnapshot(orderSnapshot)
                        if (order != null && shouldShowOrder(order)) {
                            order.sellerName = sellerName
                            fetchBuyerNameAndAddress(order)
                        }
                    }
                }
                // list is refined + sorted inside fetchBuyerNameAndAddress
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DeliveriesActivity,
                    "Failed to load orders: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /**
     * Rules:
     *  - Only DELIVERY orders.
     *  - Do NOT show if seller has not accepted yet:
     *      "Waiting for seller approval" / "Waiting_approval" / "Pending".
     *  - Do NOT show rejected orders.
     *  - Do NOT show orders assigned to a different driver.
     *  - Show:
     *      * Unassigned orders that seller already accepted (PREPARING / READY...).
     *      * Your own orders that are ACCEPTED / DELIVERING / DELIVERED.
     */
    private fun shouldShowOrder(order: Order): Boolean {
        if (order.deliveryType != "DELIVERY") return false

        val statusUpper = order.status.uppercase()
        val currentUid = auth.currentUser?.uid

        // 1) Hide if seller did NOT accept yet
        val waitingStatuses = setOf(
            "WAITING FOR SELLER APPROVAL",
            "WAITING_FOR_SELLER_APPROVAL",
            "WAITING_APPROVAL",
            "PENDING"
        )
        if (statusUpper in waitingStatuses) return false

        // 2) Hide rejected orders
        if (statusUpper.startsWith("REJECT")) return false

        val isDelivered = statusUpper == "DELIVERED"
        val isAssigned = order.deliveredBy.isNotBlank()
        val isMine = currentUid != null && order.deliveredBy == currentUid

        // 3) If assigned to someone else → hide
        if (isAssigned && !isMine) return false

        // 4) Show my delivered orders (optional)
        if (isDelivered) return isMine

        // 5) For all other statuses (PREPARING, READY, DELIVERING...):
        //    show if unassigned (so I can accept) OR assigned to me.
        return !isAssigned || isMine
    }

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
            null
        }
    }

    // ===== NEWEST ORDERS AT TOP (change is here) =====
    private fun fetchBuyerNameAndAddress(order: Order) {
        val buyerRef = database.getReference("Buyers").child(order.buyerUid)
        buyerRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(buyerSnapshot: DataSnapshot) {
                if (buyerSnapshot.exists()) {
                    order.buyerName =
                        buyerSnapshot.child("fullName").getValue(String::class.java) ?: "Unknown Buyer"

                    val addr = buyerSnapshot.child("address")
                    if (addr.exists()) {
                        order.apartmentNumber =
                            addr.child("apartmentNumber").getValue(String::class.java) ?: ""
                        order.buildingName =
                            addr.child("buildingName").getValue(String::class.java) ?: ""
                        order.floorNumber =
                            addr.child("floorNumber").getValue(String::class.java) ?: ""
                        order.streetName =
                            addr.child("streetName").getValue(String::class.java) ?: ""
                    }
                } else {
                    order.buyerName = "Unknown Buyer"
                }

                // If order already in list, update it; otherwise add
                val existingIndex = ordersList.indexOfFirst { it.orderId == order.orderId }
                if (existingIndex >= 0) {
                    ordersList[existingIndex] = order
                } else {
                    ordersList.add(order)
                }

                // 🔽 KEY PART: sort by timestamp DESC so newest at top 🔽
                ordersList.sortByDescending { it.timestamp }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                order.buyerName = "Unknown Buyer"

                val existingIndex = ordersList.indexOfFirst { it.orderId == order.orderId }
                if (existingIndex >= 0) {
                    ordersList[existingIndex] = order
                } else {
                    ordersList.add(order)
                }

                ordersList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }
        })
    }

    // ===== ACTIONS =====

    private fun acceptOrder(order: Order) {
        // "Accept" = assign the order to this driver (does NOT change status).
        orderDeliveryService.assignOrder(
            order = order,
            onSuccess = {
                Toast.makeText(this, "Order assigned to you.", Toast.LENGTH_SHORT).show()
                hasActiveOrder = true
                checkForActiveOrder()
                loadOrdersFromSellers()
            },
            onFailure = { msg ->
                Toast.makeText(this, "Failed to assign: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun startDelivery(order: Order) {
        orderDeliveryService.startDelivery(
            order = order,
            onSuccess = {
                Toast.makeText(this, "Delivery started.", Toast.LENGTH_SHORT).show()
                loadOrdersFromSellers()
            },
            onFailure = { msg ->
                Toast.makeText(this, "Failed to start delivery: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun deliverOrder(order: Order) {
        orderDeliveryService.markOrderAsDelivered(
            order = order,
            onSuccess = { points ->
                Toast.makeText(
                    this,
                    "Order delivered! You earned $points points.",
                    Toast.LENGTH_LONG
                ).show()
                hasActiveOrder = false
                checkForActiveOrder()
                loadOrdersFromSellers()
            },
            onFailure = { msg ->
                Toast.makeText(this, "Failed to deliver: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showOrderDetails(order: Order) {
        val i = Intent(this, OrderDetailsActivity::class.java)
        i.putExtra("ORDER", order)
        startActivity(i)
    }

    override fun onResume() {
        super.onResume()
        checkForActiveOrder()
        loadOrdersFromSellers()
    }
}
