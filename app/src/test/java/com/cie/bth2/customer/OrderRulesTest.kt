package com.cie.bth2.customer

import com.cie.bth2.data.Fruit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRulesTest {

    private val today = "2026-10-23"
    private fun fruit(quantity: Int = 10, expiry: String = "2026-12-31") =
        Fruit(1, "Táo", "Táo", 10000.0, quantity, expiry, "")

    @Test fun expiredYesterday() = assertTrue(OrderRules.isExpired(fruit(expiry = "2026-10-22"), today))
    @Test fun expiryTodayStillValid() = assertFalse(OrderRules.isExpired(fruit(expiry = today), today))

    @Test fun blocksExpired() =
        assertEquals(OrderRules.Result.EXPIRED, OrderRules.check(fruit(expiry = "2020-01-01"), 1, today))

    @Test fun blocksOutOfStock() =
        assertEquals(OrderRules.Result.OUT_OF_STOCK, OrderRules.check(fruit(quantity = 0), 1, today))

    @Test fun blocksNonPositiveAmount() =
        assertEquals(OrderRules.Result.INVALID_QUANTITY, OrderRules.check(fruit(), 0, today))

    @Test fun blocksMoreThanStock() =
        assertEquals(OrderRules.Result.NOT_ENOUGH_STOCK, OrderRules.check(fruit(quantity = 3), 4, today))

    @Test fun allowsExactStock() =
        assertEquals(OrderRules.Result.OK, OrderRules.check(fruit(quantity = 3), 3, today))

    @Test fun canBuy() {
        assertTrue(OrderRules.canBuy(fruit(), today))
        assertFalse(OrderRules.canBuy(fruit(quantity = 0), today))
        assertFalse(OrderRules.canBuy(fruit(expiry = "2026-01-01"), today))
    }
}
