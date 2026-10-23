package com.cie.bth2.admin.fruit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Integration boundary: replace with the shared database's DAO adapter when available. */
interface FruitRepository {
    fun all(): List<Fruit>
    fun save(fruit: Fruit)
    fun delete(id: Long)
    fun close()
}

class SqliteFruitRepository(context: Context) : FruitRepository {
    private val helper = object : SQLiteOpenHelper(context.applicationContext, "admin_fruit.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""CREATE TABLE Fruit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL, category TEXT NOT NULL,
                price REAL NOT NULL CHECK(price >= 0),
                quantity INTEGER NOT NULL CHECK(quantity >= 0),
                expiryDate TEXT NOT NULL, description TEXT NOT NULL)""")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    override fun all(): List<Fruit> = helper.readableDatabase.query(
        "Fruit", arrayOf("id", "name", "category", "price", "quantity", "expiryDate", "description"),
        null, null, null, null, "name COLLATE NOCASE, id"
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(Fruit(cursor.getLong(0), cursor.getString(1),
                cursor.getString(2), cursor.getDouble(3), cursor.getInt(4),
                cursor.getString(5), cursor.getString(6)))
        }
    }

    override fun save(fruit: Fruit) {
        require(FruitRules.validate(fruit) == null)
        val values = ContentValues().apply {
            put("name", fruit.name.trim()); put("category", fruit.category.trim())
            put("price", fruit.price); put("quantity", fruit.quantity)
            put("expiryDate", fruit.expiryDate); put("description", fruit.description.trim())
        }
        if (fruit.id == 0L) helper.writableDatabase.insertOrThrow("Fruit", null, values)
        else check(helper.writableDatabase.update("Fruit", values, "id = ?", arrayOf(fruit.id.toString())) == 1)
    }

    override fun delete(id: Long) {
        check(helper.writableDatabase.delete("Fruit", "id = ?", arrayOf(id.toString())) == 1)
    }
    override fun close() = helper.close()
}
