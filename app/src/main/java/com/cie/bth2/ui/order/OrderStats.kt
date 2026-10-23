package com.cie.bth2.ui.order

import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.OrderRepository

/** Order counts per status for the admin dashboard, aggregated from an order list. */
data class OrderStats(
    val total: Int,
    val pending: Int,
    val confirmed: Int,
    val completed: Int,
    val completedRevenue: Double
) {
    companion object {
        fun from(orders: List<OrderRepository.Order>): OrderStats {
            val completedOrders = orders.filter { it.status == DatabaseHelper.STATUS_COMPLETED }
            return OrderStats(
                total = orders.size,
                pending = orders.count { it.status == DatabaseHelper.STATUS_PENDING },
                confirmed = orders.count { it.status == DatabaseHelper.STATUS_CONFIRMED },
                completed = completedOrders.size,
                completedRevenue = completedOrders.sumOf { it.totalAmount }
            )
        }

        /** Next status in Pending -> Confirmed -> Completed, or null when there is no further step. */
        fun nextStatus(status: String): String? = when (status) {
            DatabaseHelper.STATUS_PENDING -> DatabaseHelper.STATUS_CONFIRMED
            DatabaseHelper.STATUS_CONFIRMED -> DatabaseHelper.STATUS_COMPLETED
            else -> null
        }
    }
}
