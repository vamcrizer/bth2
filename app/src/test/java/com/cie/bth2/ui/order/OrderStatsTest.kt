package com.cie.bth2.ui.order

import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.OrderRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderStatsTest {

    private fun order(id: Long, status: String, total: Double) =
        OrderRepository.Order(id, 1, "Khách", "2026-10-23 10:00:00", total, status)

    @Test
    fun nextStatusFollowsPendingConfirmedCompleted() {
        assertEquals(DatabaseHelper.STATUS_CONFIRMED, OrderStats.nextStatus(DatabaseHelper.STATUS_PENDING))
        assertEquals(DatabaseHelper.STATUS_COMPLETED, OrderStats.nextStatus(DatabaseHelper.STATUS_CONFIRMED))
        assertNull(OrderStats.nextStatus(DatabaseHelper.STATUS_COMPLETED))
        assertNull(OrderStats.nextStatus("Cancelled"))
    }

    @Test
    fun statsCountEachStatusAndCompletedRevenue() {
        val stats = OrderStats.from(
            listOf(
                order(1, DatabaseHelper.STATUS_PENDING, 10_000.0),
                order(2, DatabaseHelper.STATUS_PENDING, 20_000.0),
                order(3, DatabaseHelper.STATUS_CONFIRMED, 30_000.0),
                order(4, DatabaseHelper.STATUS_COMPLETED, 40_000.0),
                order(5, DatabaseHelper.STATUS_COMPLETED, 50_000.0)
            )
        )
        assertEquals(OrderStats(5, 2, 1, 2, 90_000.0), stats)
    }

    @Test
    fun statsOfEmptyListAreZero() {
        assertEquals(OrderStats(0, 0, 0, 0, 0.0), OrderStats.from(emptyList()))
    }
}
