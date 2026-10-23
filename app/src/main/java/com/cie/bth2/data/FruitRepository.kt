package com.cie.bth2.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** CRUD operations for fruits in the local catalog. */
class FruitRepository(private val databaseHelper: DatabaseHelper) {

    data class Fruit(
        val id: Long = 0,
        val name: String,
        val category: String,
        val price: Double,
        val quantity: Int,
        /** ISO-8601 date (yyyy-MM-dd), so dates sort lexicographically. */
        val expiryDate: String,
        val description: String = ""
    )

    fun getAll(): List<Fruit> = queryFruits(null, null)

    fun getById(id: Long): Fruit? = queryFruits(
        "${DatabaseHelper.COL_ID} = ?", arrayOf(id.toString())
    ).firstOrNull()

    fun search(query: String, category: String? = null): List<Fruit> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (query.isNotBlank()) {
            clauses += "(${DatabaseHelper.COL_NAME} LIKE ? OR ${DatabaseHelper.COL_DESCRIPTION} LIKE ?)"
            args += "%${query.trim()}%"
            args += "%${query.trim()}%"
        }
        if (!category.isNullOrBlank()) {
            clauses += "${DatabaseHelper.COL_CATEGORY} = ?"
            args += category
        }
        return queryFruits(clauses.takeIf { it.isNotEmpty() }?.joinToString(" AND "), args.toTypedArray().takeIf { args.isNotEmpty() })
    }

    fun insert(fruit: Fruit): Long {
        validate(fruit)
        return databaseHelper.writableDatabase.insertOrThrow(
            DatabaseHelper.TABLE_FRUIT, null, fruit.toContentValues()
        )
    }

    fun update(fruit: Fruit): Boolean {
        require(fruit.id > 0) { "Fruit ID must be positive" }
        validate(fruit)
        return databaseHelper.writableDatabase.update(
            DatabaseHelper.TABLE_FRUIT,
            fruit.toContentValues(),
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(fruit.id.toString())
        ) > 0
    }

    fun delete(id: Long): Boolean = databaseHelper.writableDatabase.delete(
        DatabaseHelper.TABLE_FRUIT,
        "${DatabaseHelper.COL_ID} = ?",
        arrayOf(id.toString())
    ) > 0

    private fun queryFruits(selection: String?, selectionArgs: Array<String>?): List<Fruit> =
        databaseHelper.readableDatabase.query(
            DatabaseHelper.TABLE_FRUIT,
            arrayOf(
                DatabaseHelper.COL_ID, DatabaseHelper.COL_NAME, DatabaseHelper.COL_CATEGORY,
                DatabaseHelper.COL_PRICE, DatabaseHelper.COL_QUANTITY,
                DatabaseHelper.COL_EXPIRY_DATE, DatabaseHelper.COL_DESCRIPTION
            ),
            selection, selectionArgs, null, null, "${DatabaseHelper.COL_NAME} COLLATE NOCASE ASC"
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    Fruit(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)),
                        category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY)),
                        price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PRICE)),
                        quantity = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUANTITY)),
                        expiryDate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EXPIRY_DATE)),
                        description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DESCRIPTION))
                    )
                )
            }
        }

    private fun Fruit.toContentValues() = ContentValues().apply {
        put(DatabaseHelper.COL_NAME, name.trim())
        put(DatabaseHelper.COL_CATEGORY, category.trim())
        put(DatabaseHelper.COL_PRICE, price)
        put(DatabaseHelper.COL_QUANTITY, quantity)
        put(DatabaseHelper.COL_EXPIRY_DATE, expiryDate)
        put(DatabaseHelper.COL_DESCRIPTION, description.trim())
    }

    private fun validate(fruit: Fruit) {
        require(fruit.name.isNotBlank()) { "Fruit name must not be blank" }
        require(fruit.category.isNotBlank()) { "Fruit category must not be blank" }
        require(fruit.price >= 0) { "Price cannot be negative" }
        require(fruit.quantity >= 0) { "Quantity cannot be negative" }
        require(ISO_DATE.matches(fruit.expiryDate)) { "Expiry date must use yyyy-MM-dd" }
    }

    companion object {
        private val ISO_DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
