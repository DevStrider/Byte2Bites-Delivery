package com.byte2bites.delivery

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.byte2bites.delivery.databinding.ItemDeliveryBinding
import java.text.SimpleDateFormat
import java.util.*

class DeliveriesAdapter(
    private val orders: MutableList<Order>,
    private val currentUserId: String?,
    private val onOrderClick: (Order) -> Unit,
    private val onAcceptOrder: (Order) -> Unit,
    private val onStartDelivery: (Order) -> Unit,
    private val onDeliverOrder: (Order) -> Unit
) : RecyclerView.Adapter<DeliveriesAdapter.VH>() {

    inner class VH(val b: ItemDeliveryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDeliveryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = orders[position]
        val b = holder.b

        val ctx = holder.itemView.context
        val statusUpper = order.status.uppercase(Locale.getDefault())
        val isAssignedToMe = currentUserId != null && order.deliveredBy == currentUserId
        val isUnassigned = order.deliveredBy.isBlank()
        val isRejected = statusUpper.startsWith("REJECT")
        val isDelivered = statusUpper == "DELIVERED"

        // --- Basic info ---
        b.tvOrderId.text = "Order ID: ${order.orderId}"
        b.tvBuyerName.text = "Buyer: ${order.buyerName.ifBlank { "Unknown buyer" }}"
        b.tvRestaurantName.text = "Restaurant: ${order.sellerName.ifBlank { "Restaurant" }}"

        // Pickup address (using seller name as placeholder)
        b.tvPickupAddress.text = "Pickup: ${order.sellerName.ifBlank { "Restaurant" }}"

        val addrParts = listOf(
            order.apartmentNumber.ifBlank { null },
            order.buildingName.ifBlank { null },
            if (order.floorNumber.isNotBlank()) "Floor ${order.floorNumber}" else null,
            order.streetName.ifBlank { null }
        ).filterNotNull()

        b.tvDropoffAddress.text =
            if (addrParts.isNotEmpty()) {
                "Drop-off: ${addrParts.joinToString(", ")}"
            } else {
                "Drop-off: (no address)"
            }

        // --- Total from cents ---
        val symbol = ctx.getString(R.string.currency_symbol)
        val cents = order.totalCents
        val dollars = cents / 100
        val frac = (cents % 100).toString().padStart(2, '0')
        b.tvTotal.text = "Total: $symbol$dollars.$frac"

        // --- Time ---
        val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        b.tvTime.text = sdf.format(Date(order.timestamp))

        // Click → details
        b.root.setOnClickListener { onOrderClick(order) }

        // --- Default button visibility ---
        b.btnAcceptOrder.visibility = View.GONE
        b.btnDeliverOrder.visibility = View.GONE

        // --- Status label + colors ---
        when {
            isRejected -> {
                b.tvStatus.text = "Status: REJECTED"
                b.tvStatus.setTextColor(Color.RED)
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
            isDelivered && isAssignedToMe -> {
                b.tvStatus.text = "Status: DELIVERED ✓"
                b.tvStatus.setTextColor(Color.GREEN)
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
            else -> {
                b.tvStatus.text = "Status: ${order.status}"
                b.tvStatus.setTextColor(
                    ContextCompat.getColor(ctx, android.R.color.holo_blue_dark)
                )
                holder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        if (isRejected) {
            // No actions for rejected orders
            return
        }

        // --- Button logic ---

        // 1) No driver yet → show Accept button (for any non-delivered status)
        if (!isDelivered && isUnassigned) {
            b.btnAcceptOrder.visibility = View.VISIBLE
            b.btnAcceptOrder.isEnabled = true
            b.btnAcceptOrder.text = "Accept order"
            b.btnAcceptOrder.alpha = 1f
            b.btnAcceptOrder.setOnClickListener { onAcceptOrder(order) }
            return
        }

        // 2) Order assigned to this driver
        if (isAssignedToMe) {
            when (statusUpper) {
                // Waiting / preparing → disabled button
                "WAITING FOR SELLER APPROVAL",
                "WAITING_APPROVAL",
                "PENDING",
                "PREPARING" -> {
                    b.btnAcceptOrder.visibility = View.VISIBLE
                    b.btnAcceptOrder.isEnabled = false
                    b.btnAcceptOrder.text = "Waiting for order to be ready"
                    b.btnAcceptOrder.alpha = 0.6f
                }

                // Ready to start delivery
                "READY FOR DELIVERING",
                "READY",
                "READY FOR PICKUP/DELIVERING" -> {
                    b.btnDeliverOrder.visibility = View.VISIBLE
                    b.btnDeliverOrder.isEnabled = true
                    b.btnDeliverOrder.text = "Start delivery"
                    b.btnDeliverOrder.alpha = 1f
                    b.btnDeliverOrder.setOnClickListener { onStartDelivery(order) }
                }

                // Already delivering → can mark delivered
                "DELIVERING" -> {
                    b.btnDeliverOrder.visibility = View.VISIBLE
                    b.btnDeliverOrder.isEnabled = true
                    b.btnDeliverOrder.text = "Mark delivered"
                    b.btnDeliverOrder.alpha = 1f
                    b.btnDeliverOrder.setOnClickListener { onDeliverOrder(order) }
                }
            }
        }
    }

    override fun getItemCount(): Int = orders.size
}
