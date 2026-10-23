package com.cie.bth2

import android.content.ContentValues
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cie.bth2.data.AccountRepository
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.FruitRepository
import com.cie.bth2.data.OrderRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseRepositoryInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: DatabaseHelper

    @Before
    fun setUp() {
        context.deleteDatabase("repository_tests.db")
        database = DatabaseHelper(context, "repository_tests.db")
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase("repository_tests.db")
    }

    @Test
    fun createsFourTablesAndSeedsAccountsCustomerAndFruit() {
        val tables = database.readableDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' ORDER BY name",
            null
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

        assertEquals(listOf("Account", "Customer", "Fruit", "Order"), tables)
        assertEquals(2, AccountRepository(database).let {
            listOf(
                it.authenticate("admin", "admin123"),
                it.authenticate("customer", "customer123")
            ).count { account -> account != null }
        })
        assertEquals(3, FruitRepository(database).getAll().size)
        assertNotNull(CustomerRepository(database).getByAccountId(2))
    }

    @Test
    fun fruitRepositorySupportsSearchAndCrud() {
        val fruits = FruitRepository(database)
        val id = fruits.insert(
            FruitRepository.Fruit(
                name = "Kiwi", category = "Tropical", price = 12000.0,
                quantity = 5, expiryDate = "2099-01-01", description = "Green kiwi"
            )
        )
        assertEquals("Kiwi", fruits.getById(id)?.name)
        assertEquals(1, fruits.search("green", "Tropical").size)
        assertTrue(fruits.update(fruits.getById(id)!!.copy(quantity = 8)))
        assertEquals(8, fruits.getById(id)?.quantity)
        assertTrue(fruits.delete(id))
        assertNull(fruits.getById(id))
    }

    @Test
    fun foreignKeysRejectCustomerForMissingAccount() {
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_FULL_NAME, "Invalid")
            put(DatabaseHelper.COL_ACCOUNT_ID, 9999)
        }
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            database.writableDatabase.insertOrThrow(DatabaseHelper.TABLE_CUSTOMER, null, values)
        }
    }

    @Test
    fun placingOrderSubtractsStockCalculatesTotalAndEnforcesStatusFlow() {
        val fruit = FruitRepository(database).getAll().first()
        val orders = OrderRepository(database)
        val customerId = CustomerRepository(database).getByAccountId(2)!!.id

        val orderId = orders.placeOrder(
            customerId,
            listOf(OrderRepository.OrderItem(fruit.id, 2)),
            "2026-10-23 10:00:00"
        )

        assertEquals(fruit.price * 2, orders.getById(orderId)!!.totalAmount, 0.001)
        assertEquals(fruit.quantity - 2, FruitRepository(database).getById(fruit.id)!!.quantity)
        assertEquals(DatabaseHelper.STATUS_PENDING, orders.getById(orderId)!!.status)
        assertTrue(orders.updateStatus(orderId, DatabaseHelper.STATUS_CONFIRMED))
        assertTrue(orders.updateStatus(orderId, DatabaseHelper.STATUS_COMPLETED))
        assertThrows(IllegalArgumentException::class.java) {
            orders.updateStatus(orderId, DatabaseHelper.STATUS_PENDING)
        }
    }

    @Test
    fun failedMultiItemOrderRollsBackStockAndOrder() {
        val fruits = FruitRepository(database).getAll()
        val first = fruits[0]
        val second = fruits[1]
        val orders = OrderRepository(database)
        val customerId = CustomerRepository(database).getByAccountId(2)!!.id

        assertThrows(IllegalArgumentException::class.java) {
            orders.placeOrder(
                customerId,
                listOf(
                    OrderRepository.OrderItem(first.id, 1),
                    OrderRepository.OrderItem(second.id, second.quantity + 1)
                ),
                "2026-10-23 10:00:00"
            )
        }

        assertEquals(first.quantity, FruitRepository(database).getById(first.id)!!.quantity)
        assertEquals(0, orders.getForCustomer(customerId).size)
    }

    @Test
    fun expiredFruitCannotBeOrdered() {
        val fruits = FruitRepository(database)
        val id = fruits.insert(
            FruitRepository.Fruit(
                name = "Expired", category = "Test", price = 100.0,
                quantity = 4, expiryDate = "2020-01-01"
            )
        )
        val customerId = CustomerRepository(database).getByAccountId(2)!!.id

        assertThrows(IllegalArgumentException::class.java) {
            OrderRepository(database).placeOrder(
                customerId,
                listOf(OrderRepository.OrderItem(id, 1)),
                "2026-10-23 10:00:00"
            )
        }
        assertEquals(4, fruits.getById(id)!!.quantity)
    }

    @Test
    fun rejectsInvalidDatesAndNonFinitePrices() {
        val fruits = FruitRepository(database)
        val fruit = fruits.getAll().first()
        listOf("2026-02-31", "2026-13-01", "2026-1-01").forEach { date ->
            assertThrows(IllegalArgumentException::class.java) { fruits.update(fruit.copy(expiryDate = date)) }
        }
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { price ->
            assertThrows(IllegalArgumentException::class.java) { fruits.update(fruit.copy(price = price)) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            OrderRepository(database).create(1, 100.0, "2026-02-31 25:00:00")
        }
        assertEquals(fruit, fruits.getById(fruit.id))
    }

    @Test
    fun backdatingOrderCannotBuyExpiredFruit() {
        val fruits = FruitRepository(database)
        val fruit = fruits.getAll().first()
        fruits.update(fruit.copy(expiryDate = "2000-01-01"))
        assertThrows(IllegalArgumentException::class.java) {
            OrderRepository(database).placeOrder(1,
                listOf(OrderRepository.OrderItem(fruit.id, 1)), "1999-01-01 00:00:00")
        }
        assertEquals(fruit.quantity, fruits.getById(fruit.id)!!.quantity)
        assertTrue(OrderRepository(database).getAll().isEmpty())
    }

    @Test
    fun duplicateQuantitiesCannotOverflowAndIncreaseStock() {
        val fruit = FruitRepository(database).getAll().first()
        assertThrows(IllegalArgumentException::class.java) {
            OrderRepository(database).placeOrder(1, listOf(
                OrderRepository.OrderItem(fruit.id, Int.MAX_VALUE),
                OrderRepository.OrderItem(fruit.id, Int.MAX_VALUE)))
        }
        assertEquals(fruit.quantity, FruitRepository(database).getById(fruit.id)!!.quantity)
    }

    @Test
    fun insertFailureAfterStockUpdateRollsBackAllItems() {
        val before = FruitRepository(database).getAll()
        // Trigger only rejects the order once the first fruit has actually been decremented.
        val first = before.first()
        database.writableDatabase.execSQL("""
            CREATE TRIGGER reject_order AFTER INSERT ON "Order"
            WHEN (SELECT quantity FROM Fruit WHERE id = ${first.id}) = ${first.quantity - 1}
            BEGIN SELECT RAISE(ABORT, 'simulated order storage failure'); END
        """.trimIndent())
        try {
            assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
                OrderRepository(database).placeOrder(1, before.map { OrderRepository.OrderItem(it.id, 1) })
            }
            assertEquals(before, FruitRepository(database).getAll())
            assertTrue(OrderRepository(database).getAll().isEmpty())
        } finally {
            database.writableDatabase.execSQL("DROP TRIGGER reject_order")
        }
    }

    @Test
    fun adminCatalogCustomerPurchaseAndOrderHistoryShareDatabase() {
        val admin = com.cie.bth2.admin.fruit.SqliteFruitRepository(context, "repository_tests.db")
        val shop = com.cie.bth2.customer.CustomerShopRepository(context, "repository_tests.db")
        try {
            admin.save(com.cie.bth2.admin.fruit.Fruit(name = "Integration fruit", category = "Test",
                price = 15000.0, quantity = 7, expiryDate = "2099-12-31", description = ""))
            val fruit = shop.searchFruits("Integration fruit", "Test", false).single()
            val customer = CustomerRepository(database).getByAccountId(2)!!
            val result = shop.placeOrder(customer.id, fruit.id, 2)
                as com.cie.bth2.customer.CustomerShopRepository.PlaceOrderResult.Success
            assertEquals(30000.0, result.total, 0.001)
            assertEquals(5, admin.all().single { it.id == fruit.id }.quantity)
            val orders = OrderRepository(database)
            assertTrue(orders.updateStatus(result.orderId, DatabaseHelper.STATUS_CONFIRMED))
            assertTrue(orders.updateStatus(result.orderId, DatabaseHelper.STATUS_COMPLETED))
            val history = orders.getForCustomer(customer.id).single()
            assertEquals(DatabaseHelper.STATUS_COMPLETED, history.status)
            assertEquals(result.orderId, history.id)
            assertEquals(30000.0, com.cie.bth2.ui.order.OrderStats.from(listOf(history)).completedRevenue, 0.001)
        } finally {
            shop.close()
            admin.close()
        }
    }
}
