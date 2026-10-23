package com.cie.bth2.admin.fruit

import org.junit.Assert.*
import org.junit.Test

class FruitRulesTest {
    private val fruit = Fruit(1, "Cam", "Có múi", 25000.0, 10, "2026-10-23", "Cam ngọt")

    @Test fun expiryIncludesEntireExpiryDay() {
        assertFalse(FruitRules.expired(fruit, "2026-10-23"))
        assertTrue(FruitRules.expired(fruit, "2026-10-24"))
        assertFalse(FruitRules.expired(fruit, "2026-10-22"))
        assertTrue(FruitRules.expired(fruit.copy(expiryDate = "2025-12-31"), "2026-01-01"))
    }

    @Test fun validatesCalendarDatesAndLeapYears() {
        assertTrue(FruitRules.validDate("2024-02-29"))
        listOf("2026-02-29", "2026-04-31", "2026-13-01", "2026-1-01", "2026-01-01junk", "").forEach {
            assertFalse(it, FruitRules.validDate(it))
        }
    }

    @Test fun rejectsInvalidFieldsButAllowsZeroStockAndPastDates() {
        assertNull(FruitRules.validate(fruit.copy(quantity = 0, price = 0.0, expiryDate = "2020-01-01")))
        listOf(fruit.copy(name = " "), fruit.copy(category = ""), fruit.copy(price = -1.0),
            fruit.copy(price = Double.NaN), fruit.copy(price = Double.POSITIVE_INFINITY),
            fruit.copy(quantity = -1), fruit.copy(expiryDate = "2026-02-30")).forEach {
            assertNotNull(FruitRules.validate(it))
        }
    }

    @Test fun combinesSearchCategoryAndExpiryFilters() {
        val yesterday = fruit.copy(id = 2, expiryDate = "2026-10-22")
        val other = fruit.copy(id = 3, name = "Táo", category = "Táo", description = "", expiryDate = "2026-10-24")
        val items = listOf(fruit, yesterday, other)
        assertEquals(listOf(yesterday), FruitRules.filter(items, " CAM ", "Có múi", ExpiryFilter.EXPIRED, "2026-10-23"))
        assertEquals(listOf(fruit), FruitRules.filter(items, "ngọt", null, ExpiryFilter.TODAY, "2026-10-23"))
        assertEquals(listOf(fruit, other), FruitRules.filter(items, "", null, ExpiryFilter.VALID, "2026-10-23"))
        assertTrue(FruitRules.filter(items, "Cam", "Táo", ExpiryFilter.ALL).isEmpty())
    }
}
