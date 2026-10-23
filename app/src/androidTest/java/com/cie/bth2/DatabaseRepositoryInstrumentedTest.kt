package com.cie.bth2

import android.content.ContentValues
import android.content.Intent
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cie.bth2.data.AccountRepository
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.FruitRepository
import com.cie.bth2.data.OrderRepository
import com.cie.bth2.demo.DatabaseDemoActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun showcaseScreenOpensAndDisplaysDatabaseDemo() {
        val intent = Intent(context, DatabaseDemoActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
        try {
            assertTrue(activity is DatabaseDemoActivity)
            onView(withText("DATABASE SHOWCASE")).check(matches(isDisplayed()))
            onView(withText("Xem bảng")).check(matches(isDisplayed()))
        } finally {
            activity.finish()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }
    }

    @Test
    fun droppingAndResettingDemoTableRestoresSchemaAndSeedRows() {
        database.dropTable(DatabaseHelper.TABLE_FRUIT)
        val dropped = database.readableDatabase.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='Fruit'", null
        ).use { it.moveToFirst() }
        assertFalse(dropped)

        database.resetDemoDatabase()
        assertEquals(3, FruitRepository(database).getAll().size)
    }
}
