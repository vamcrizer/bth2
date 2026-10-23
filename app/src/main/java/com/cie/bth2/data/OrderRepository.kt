package com.cie.bth2.data

import android.content.ContentValues
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Order creation and status/history queries for customer and admin modules. */
class OrderRepository(private val databaseHelper: DatabaseHelper) {

    data class Order(
        val id: Long,
        val customerId: Long,
        val customerName: String,
        val orderDate: String,
        val totalAmount: Double,
        val status: String
    )

    data class OrderItem(val fruitId: Long, val quantity: Int)

    fun getAll(): List<Order> = queryOrders(null, null)

    fun getById(id: Long): Order? = queryOrders(
        "o.${DatabaseHelper.COL_ID} = ?", arrayOf(id.toString())
    ).firstOrNull()

    fun getForCustomer(customerId: Long): List<Order> = queryOrders(
        "o.${DatabaseHelper.COL_CUSTOMER_ID} = ?", arrayOf(customerId.toString())
    )

    /** Creates a pending order and returns its ID. */
    fun create(customerId: Long, totalAmount: Double, orderDate: String = today()): Long {
        require(customerId > 0) { "Customer ID must be positive" }
        require(totalAmount >= 0) { "Order total cannot be negative" }
        require(ISO_DATE_TIME.matches(orderDate)) { "Order date must use yyyy-MM-dd HH:mm:ss" }

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CUSTOMER_ID, customerId)
            put(DatabaseHelper.COL_ORDER_DATE, orderDate)
            put(DatabaseHelper.COL_TOTAL_AMOUNT, totalAmount)
            put(DatabaseHelper.COL_STATUS, DatabaseHelper.STATUS_PENDING)
        }
        return databaseHelper.writableDatabase.insertOrThrow(DatabaseHelper.TABLE_ORDER, null, values)
    }

    /**
     * Validates and reserves all requested fruit, then creates the order atomically.
     * The caller supplies the order total from the selected fruit prices.
     */
    fun placeOrder(
        customerId: Long,
        items: List<OrderItem>,
        orderDate: String = today()
    ): Long {
        require(customerId > 0) { "Customer ID must be positive" }
        require(items.isNotEmpty()) { "An order must contain at least one item" }
        require(items.all { it.fruitId > 0 && it.quantity > 0 }) {
            "Fruit IDs and quantities must be positive"
        }
        require(ISO_DATE_TIME.matches(orderDate)) { "Order date must use yyyy-MM-dd HH:mm:ss" }

        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            val customerExists = db.query(
                DatabaseHelper.TABLE_CUSTOMER,
                arrayOf(DatabaseHelper.COL_ID),
                "${DatabaseHelper.COL_ID} = ?",
                arrayOf(customerId.toString()), null, null, null, "1"
            ).use { it.moveToFirst() }
            require(customerExists) { "Customer does not exist" }

            // Merge duplicate fruit IDs before checking and decrementing stock.
            val quantities = items.groupBy { it.fruitId }.mapValues { (_, rows) -> rows.sumOf { it.quantity } }
            var totalAmount = 0.0
            quantities.forEach { (fruitId, quantity) ->
                val fruit = db.query(
                    DatabaseHelper.TABLE_FRUIT,
                    arrayOf(DatabaseHelper.COL_PRICE, DatabaseHelper.COL_QUANTITY, DatabaseHelper.COL_EXPIRY_DATE),
                    "${DatabaseHelper.COL_ID} = ?",
                    arrayOf(fruitId.toString()), null, null, null, "1"
                ).use { cursor ->
                    if (!cursor.moveToFirst()) null else Triple(
                        cursor.getDouble(0), cursor.getInt(1), cursor.getString(2)
                    )
                } ?: throw IllegalArgumentException("Fruit $fruitId does not exist")

                require(fruit.second >= quantity) { "Insufficient stock for fruit $fruitId" }
                require(fruit.third >= orderDate.substring(0, 10)) { "Fruit $fruitId is expired" }
                totalAmount += fruit.first * quantity
            }

            quantities.forEach { (fruitId, quantity) ->
                val updated = db.update(
                    DatabaseHelper.TABLE_FRUIT,
                    ContentValues().apply {
                        put(DatabaseHelper.COL_QUANTITY, "${DatabaseHelper.COL_QUANTITY} - $quantity")
                    },
                    "${DatabaseHelper.COL_ID} = ? AND ${DatabaseHelper.COL_QUANTITY} >= ?",
                    arrayOf(fruitId.toString(), quantity.toString())
                )
                if (updated != 1) throw IllegalStateException("Stock changed while placing the order")
            }

            val orderId = db.insertOrThrow(
                DatabaseHelper.TABLE_ORDER,
                null,
                ContentValues().apply {
                    put(DatabaseHelper.COL_CUSTOMER_ID, customerId)
                    put(DatabaseHelper.COL_ORDER_DATE, orderDate)
                    put(DatabaseHelper.COL_TOTAL_AMOUNT, totalAmount)
                    put(DatabaseHelper.COL_STATUS, DatabaseHelper.STATUS_PENDING)
                }
            )
            db.setTransactionSuccessful()
            return orderId
        } finally {
            db.endTransaction()
        }
    }

    /** Only allows forward transitions in the documented order lifecycle. */
    fun updateStatus(orderId: Long, status: String): Boolean {
        require(orderId > 0) { "Order ID must be positive" }
        require(status in ALLOWED_STATUSES) { "Unsupported order status" }
        val current = getById(orderId) ?: return false
        val validTransition = when (current.status) {
            DatabaseHelper.STATUS_PENDING -> status == DatabaseHelper.STATUS_CONFIRMED
            DatabaseHelper.STATUS_CONFIRMED -> status == DatabaseHelper.STATUS_COMPLETED
            else -> false
        }
        require(validTransition || current.status == status) { "Invalid order status transition" }
        if (current.status == status) return true

        val values = ContentValues().apply { put(DatabaseHelper.COL_STATUS, status) }
        return databaseHelper.writableDatabase.update(
            DatabaseHelper.TABLE_ORDER,
            values,
            "${DatabaseHelper.COL_ID} = ? AND ${DatabaseHelper.COL_STATUS} = ?",
            arrayOf(orderId.toString(), current.status)
        ) > 0
    }

    private fun queryOrders(selection: String?, args: Array<String>?): List<Order> =
        databaseHelper.readableDatabase.rawQuery(
            """SELECT o.${DatabaseHelper.COL_ID}, o.${DatabaseHelper.COL_CUSTOMER_ID},
                c.${DatabaseHelper.COL_FULL_NAME}, o.${DatabaseHelper.COL_ORDER_DATE},
                o.${DatabaseHelper.COL_TOTAL_AMOUNT}, o.${DatabaseHelper.COL_STATUS}
                FROM ${DatabaseHelper.TABLE_ORDER} o
                INNER JOIN ${DatabaseHelper.TABLE_CUSTOMER} c
                    ON c.${DatabaseHelper.COL_ID} = o.${DatabaseHelper.COL_CUSTOMER_ID}
                ${selection?.let { "WHERE $it" } ?: ""}
                ORDER BY o.${DatabaseHelper.COL_ORDER_DATE} DESC, o.${DatabaseHelper.COL_ID} DESC""".trimIndent(),
            args
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    Order(
                        id = cursor.getLong(0),
                        customerId = cursor.getLong(1),
                        customerName = cursor.getString(2),
                        orderDate = cursor.getString(3),
                        totalAmount = cursor.getDouble(4),
                        status = cursor.getString(5)
                    )
                )
            }
        }

    private fun today(): String = SimpleDateFormat(DATE_FORMAT, Locale.US).format(Date())
    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private val ISO_DATE_TIME = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
        private val ALLOWED_STATUSES = setOf(
            DatabaseHelper.STATUS_PENDING,
            DatabaseHelper.STATUS_CONFIRMED,
            DatabaseHelper.STATUS_COMPLETED
        )
    }
}
