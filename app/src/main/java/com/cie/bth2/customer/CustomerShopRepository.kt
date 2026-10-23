package com.cie.bth2.customer

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.cie.bth2.data.Fruit
import com.cie.bth2.data.FruitShopDbHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerShopRepository(context: Context) {

    private val dbHelper = FruitShopDbHelper(context)

    /** keyword/category rỗng = không lọc. includeExpired=false chỉ lấy hàng còn hạn. */
    fun searchFruits(keyword: String, category: String?, includeExpired: Boolean): List<Fruit> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (keyword.isNotBlank()) {
            where += "name LIKE ?"
            args += "%${keyword.trim()}%"
        }
        if (!category.isNullOrEmpty()) {
            where += "category = ?"
            args += category
        }
        if (!includeExpired) {
            where += "expiryDate >= ?"
            args += OrderRules.today()
        }
        val sql = "SELECT * FROM Fruit" +
            (if (where.isEmpty()) "" else " WHERE " + where.joinToString(" AND ")) +
            " ORDER BY name"
        return dbHelper.readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            buildList { while (c.moveToNext()) add(c.toFruit()) }
        }
    }

    fun getCategories(): List<String> =
        dbHelper.readableDatabase.rawQuery("SELECT DISTINCT category FROM Fruit ORDER BY category", null)
            .use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }

    fun getFruit(id: Long): Fruit? =
        dbHelper.readableDatabase.rawQuery("SELECT * FROM Fruit WHERE id = ?", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) c.toFruit() else null }

    sealed class PlaceOrderResult {
        data class Success(val orderId: Long, val total: Double) : PlaceOrderResult()
        data class Rejected(val reason: OrderRules.Result) : PlaceOrderResult()
        object FruitNotFound : PlaceOrderResult()
    }

    /** Kiểm tra lại hạn + tồn kho trong cùng transaction rồi tạo Order (Pending) và trừ kho. */
    fun placeOrder(customerId: Long, fruitId: Long, amount: Int): PlaceOrderResult {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val fruit = getFruit(fruitId) ?: return PlaceOrderResult.FruitNotFound
            val check = OrderRules.check(fruit, amount)
            if (check != OrderRules.Result.OK) return PlaceOrderResult.Rejected(check)

            val total = fruit.price * amount
            db.execSQL(
                "UPDATE Fruit SET quantity = quantity - ? WHERE id = ?",
                arrayOf<Any>(amount, fruitId)
            )
            val orderId = db.insertOrThrow("\"Order\"", null, ContentValues().apply {
                put("customerId", customerId)
                put("orderDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                put("totalAmount", total)
                put("status", "Pending")
            })
            db.setTransactionSuccessful()
            return PlaceOrderResult.Success(orderId, total)
        } finally {
            db.endTransaction()
        }
    }

    private fun Cursor.toFruit() = Fruit(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        category = getString(getColumnIndexOrThrow("category")),
        price = getDouble(getColumnIndexOrThrow("price")),
        quantity = getInt(getColumnIndexOrThrow("quantity")),
        expiryDate = getString(getColumnIndexOrThrow("expiryDate")),
        description = getString(getColumnIndexOrThrow("description")) ?: ""
    )
}
