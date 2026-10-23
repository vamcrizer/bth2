package com.cie.bth2.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Creates and seeds the local Fruit Shop database. */
class DatabaseHelper(context: Context, databaseName: String = DATABASE_NAME) :
    SQLiteOpenHelper(context.applicationContext ?: context, databaseName, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_ACCOUNT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_ROLE TEXT NOT NULL CHECK ($COL_ROLE IN ('$ROLE_ADMIN', '$ROLE_CUSTOMER'))
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE $TABLE_FRUIT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_PRICE REAL NOT NULL CHECK ($COL_PRICE >= 0),
                $COL_QUANTITY INTEGER NOT NULL DEFAULT 0 CHECK ($COL_QUANTITY >= 0),
                $COL_EXPIRY_DATE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT NOT NULL DEFAULT ''
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE $TABLE_CUSTOMER (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FULL_NAME TEXT NOT NULL,
                $COL_PHONE TEXT NOT NULL DEFAULT '',
                $COL_EMAIL TEXT NOT NULL DEFAULT '',
                $COL_ACCOUNT_ID INTEGER NOT NULL UNIQUE,
                FOREIGN KEY ($COL_ACCOUNT_ID) REFERENCES $TABLE_ACCOUNT($COL_ID) ON DELETE CASCADE
            )""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE $TABLE_ORDER (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CUSTOMER_ID INTEGER NOT NULL,
                $COL_ORDER_DATE TEXT NOT NULL,
                $COL_TOTAL_AMOUNT REAL NOT NULL CHECK ($COL_TOTAL_AMOUNT >= 0),
                $COL_STATUS TEXT NOT NULL DEFAULT '$STATUS_PENDING'
                    CHECK ($COL_STATUS IN ('$STATUS_PENDING', '$STATUS_CONFIRMED', '$STATUS_COMPLETED')),
                FOREIGN KEY ($COL_CUSTOMER_ID) REFERENCES $TABLE_CUSTOMER($COL_ID) ON DELETE CASCADE
            )""".trimIndent()
        )
        insertSeedData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit, data-preserving migrations here when the schema changes.
    }

    /** Drops one known application table. Used by the database showcase screen. */
    fun dropTable(table: String) {
        require(table in DEMO_TABLES) { "Unsupported table" }
        writableDatabase.execSQL("DROP TABLE IF EXISTS $table")
    }

    /** Recreates the local demo database and restores its sample rows. */
    fun resetDemoDatabase() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Drop dependents first so foreign-key constraints remain enabled.
            listOf(TABLE_ORDER, TABLE_CUSTOMER, TABLE_FRUIT, TABLE_ACCOUNT).forEach { table -> db.execSQL("DROP TABLE IF EXISTS $table") }
            onCreate(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun createSqlFor(table: String): String {
        require(table in DEMO_TABLES) { "Unsupported table" }
        return when (table) {
            TABLE_ACCOUNT -> """CREATE TABLE Account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('Admin', 'Customer'))
)"""
            TABLE_FRUIT -> """CREATE TABLE Fruit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    price REAL NOT NULL CHECK (price >= 0),
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    expiryDate TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT ''
)"""
            TABLE_CUSTOMER -> """CREATE TABLE Customer (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fullname TEXT NOT NULL,
    phone TEXT NOT NULL DEFAULT '',
    email TEXT NOT NULL DEFAULT '',
    accountId INTEGER NOT NULL UNIQUE,
    FOREIGN KEY (accountId) REFERENCES Account(id) ON DELETE CASCADE
)"""
            else -> """CREATE TABLE "Order" (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customerId INTEGER NOT NULL,
    orderDate TEXT NOT NULL,
    totalAmount REAL NOT NULL CHECK (totalAmount >= 0),
    status TEXT NOT NULL DEFAULT 'Pending'
        CHECK (status IN ('Pending', 'Confirmed', 'Completed')),
    FOREIGN KEY (customerId) REFERENCES Customer(id) ON DELETE CASCADE
)"""
        }
    }

    fun dropSqlFor(table: String): String {
        require(table in DEMO_TABLES) { "Unsupported table" }
        return "DROP TABLE IF EXISTS $table"
    }

    private fun insertSeedData(db: SQLiteDatabase) {
        db.insertOrThrow(TABLE_ACCOUNT, null, ContentValues().apply {
            put(COL_USERNAME, "admin")
            put(COL_PASSWORD, "admin123")
            put(COL_ROLE, ROLE_ADMIN)
        })
        val customerAccountId = db.insertOrThrow(TABLE_ACCOUNT, null, ContentValues().apply {
            put(COL_USERNAME, "customer")
            put(COL_PASSWORD, "customer123")
            put(COL_ROLE, ROLE_CUSTOMER)
        })
        db.insertOrThrow(TABLE_CUSTOMER, null, ContentValues().apply {
            put(COL_FULL_NAME, "Khách hàng mẫu")
            put(COL_PHONE, "0900000000")
            put(COL_EMAIL, "customer@example.com")
            put(COL_ACCOUNT_ID, customerAccountId)
        })

        val fruits = listOf(
            FruitSeed("Táo đỏ", "Táo", 45000.0, 30, "2099-12-31", "Táo đỏ tươi"),
            FruitSeed("Cam vàng", "Cam", 35000.0, 25, "2099-12-31", "Cam mọng nước"),
            FruitSeed("Chuối", "Chuối", 20000.0, 40, "2099-12-31", "Chuối chín tự nhiên")
        )
        fruits.forEach { fruit ->
            db.insertOrThrow(TABLE_FRUIT, null, ContentValues().apply {
                put(COL_NAME, fruit.name)
                put(COL_CATEGORY, fruit.category)
                put(COL_PRICE, fruit.price)
                put(COL_QUANTITY, fruit.quantity)
                put(COL_EXPIRY_DATE, fruit.expiryDate)
                put(COL_DESCRIPTION, fruit.description)
            })
        }
    }

    private data class FruitSeed(
        val name: String,
        val category: String,
        val price: Double,
        val quantity: Int,
        val expiryDate: String,
        val description: String
    )

    companion object {
        const val DATABASE_NAME = "fruit_shop.db"
        const val DATABASE_VERSION = 1

        const val TABLE_ACCOUNT = "Account"
        const val TABLE_FRUIT = "Fruit"
        const val TABLE_CUSTOMER = "Customer"
        const val TABLE_ORDER = "\"Order\""

        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD = "password"
        const val COL_ROLE = "role"
        const val COL_NAME = "name"
        const val COL_CATEGORY = "category"
        const val COL_PRICE = "price"
        const val COL_QUANTITY = "quantity"
        const val COL_EXPIRY_DATE = "expiryDate"
        const val COL_DESCRIPTION = "description"
        const val COL_FULL_NAME = "fullname"
        const val COL_PHONE = "phone"
        const val COL_EMAIL = "email"
        const val COL_ACCOUNT_ID = "accountId"
        const val COL_CUSTOMER_ID = "customerId"
        const val COL_ORDER_DATE = "orderDate"
        const val COL_TOTAL_AMOUNT = "totalAmount"
        const val COL_STATUS = "status"

        const val ROLE_ADMIN = "Admin"
        const val ROLE_CUSTOMER = "Customer"
        const val STATUS_PENDING = "Pending"
        const val STATUS_CONFIRMED = "Confirmed"
        const val STATUS_COMPLETED = "Completed"

        val DEMO_TABLES = listOf(TABLE_ACCOUNT, TABLE_FRUIT, TABLE_CUSTOMER, TABLE_ORDER)
    }
}
