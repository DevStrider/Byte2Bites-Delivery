package com.byte2bites.delivery

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class OrderDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val order = intent.getSerializableExtra("ORDER") as? Order

        // Root scroll view
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(
                ContextCompat.getColor(
                    this@OrderDetailsActivity,
                    R.color.bb_page_background_blue
                )
            )
        }

        // Main vertical layout
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // ---- Back row (back arrow + title) ----
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        val backBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            background = null
            contentDescription = "Back"
            val size = (32 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            setOnClickListener { finish() }
        }

        val titleView = TextView(this).apply {
            text = "Order details"
            textSize = 20f
            setTextColor(
                ContextCompat.getColor(
                    this@OrderDetailsActivity,
                    R.color.text_primary_dark
                )
            )
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }

        headerRow.addView(backBtn)
        headerRow.addView(titleView)
        content.addView(headerRow)

        // Helper for detail lines
        fun makeText(
            text: String,
            sizeSp: Float,
            bold: Boolean = false
        ): TextView {
            return TextView(this).apply {
                this.text = text
                textSize = sizeSp
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(
                    ContextCompat.getColor(
                        this@OrderDetailsActivity,
                        R.color.text_primary_dark
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (4 * resources.displayMetrics.density).toInt()
                }
            }
        }

        if (order != null) {
            content.addView(makeText("Order ID: ${order.orderId}", 16f, bold = true))
            content.addView(makeText("Buyer: ${order.buyerName}", 16f))
            content.addView(makeText("Restaurant: ${order.sellerName}", 16f))

            val address = """
                Apartment: ${order.apartmentNumber}
                Building: ${order.buildingName}
                Floor: ${order.floorNumber}
                Street: ${order.streetName}
            """.trimIndent()

            content.addView(makeText("Address:", 16f, bold = true))
            content.addView(makeText(address, 14f))

            content.addView(makeText("Status: ${order.status}", 16f))
            content.addView(makeText("Total: ${formatCurrency(order.totalCents)}", 16f, bold = true))
        } else {
            content.addView(makeText("Order information not available.", 18f, bold = true))
        }

        scrollView.addView(content)
        setContentView(scrollView)

        // We are using our own back button now; no need for action bar back.
        supportActionBar?.hide()
    }

    private fun formatCurrency(cents: Int): String {
        val whole = cents / 100
        val frac = (cents % 100).toString().padStart(2, '0')
        return "$$whole.$frac"
    }
}
