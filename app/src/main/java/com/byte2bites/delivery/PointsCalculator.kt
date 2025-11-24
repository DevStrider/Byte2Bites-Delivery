package com.byte2bites.delivery

object PointsCalculator {

    // Uses *cents* as Int
    fun calculatePoints(orderTotalCents: Int): Int {
        val orderTotalDollars = orderTotalCents / 100.0

        return when {
            orderTotalDollars <= 100 -> 10
            orderTotalDollars <= 150 -> 15
            orderTotalDollars <= 200 -> 20
            else -> {
                // For every $50 above 200, add 5 points
                20 + (((orderTotalDollars - 200) / 50).toInt() * 5)
            }
        }
    }

    // Simpler alternative if you ever want it
    fun calculatePointsSimple(orderTotalCents: Int): Int {
        val orderTotalDollars = orderTotalCents / 100.0
        val basePoints = 10
        val additionalPoints = ((orderTotalDollars / 50).toInt() * 5)
        return basePoints + additionalPoints
    }
}
