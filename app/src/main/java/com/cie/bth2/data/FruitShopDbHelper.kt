package com.cie.bth2.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Placeholder DB cho nhánh feature/customer-shopping để chạy độc lập.
 * Schema bám đúng README (Account, Fruit, Customer, Order).
 * Khi feature/database-core merge, thay class này bằng DB core của nhóm.
 */
class FruitShopDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE Account (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL)"""
        )
        db.execSQL(
            """CREATE TABLE Fruit (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                price REAL NOT NULL,
                quantity INTEGER NOT NULL,
                expiryDate TEXT NOT NULL,
                description TEXT)"""
        )
        db.execSQL(
            """CREATE TABLE Customer (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fullname TEXT NOT NULL,
                phone TEXT,
                email TEXT,
                accountId INTEGER NOT NULL REFERENCES Account(id))"""
        )
        db.execSQL(
            """CREATE TABLE "Order" (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customerId INTEGER NOT NULL REFERENCES Customer(id),
                orderDate TEXT NOT NULL,
                totalAmount REAL NOT NULL,
                status TEXT NOT NULL)"""
        )
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS \"Order\"")
        db.execSQL("DROP TABLE IF EXISTS Customer")
        db.execSQL("DROP TABLE IF EXISTS Fruit")
        db.execSQL("DROP TABLE IF EXISTS Account")
        onCreate(db)
    }

    private fun seed(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO Account(username, password, role) VALUES ('admin', 'admin123', 'Admin')")
        db.execSQL("INSERT INTO Account(username, password, role) VALUES ('user', 'user123', 'Customer')")
        db.execSQL(
            "INSERT INTO Customer(fullname, phone, email, accountId) " +
                "VALUES ('Khách Demo', '0900000000', 'user@fruitshop.vn', 2)"
        )
        val fruits = listOf(
            arrayOf("Táo Envy", "Táo", 120000.0, 50, "2099-12-31", "Táo Envy New Zealand, giòn ngọt."),
            arrayOf("Táo Fuji", "Táo", 65000.0, 0, "2099-12-31", "Táo Fuji Nhật, tạm hết hàng."),
            arrayOf("Cam sành", "Cam", 35000.0, 80, "2099-06-30", "Cam sành Vĩnh Long, nhiều nước."),
            arrayOf("Cam Úc", "Cam", 90000.0, 20, "2020-01-15", "Lô cam Úc đã quá hạn sử dụng."),
            arrayOf("Xoài cát Hoà Lộc", "Xoài", 75000.0, 30, "2099-08-20", "Xoài cát chín cây."),
            arrayOf("Nho mẫu đơn", "Nho", 250000.0, 15, "2099-09-01", "Nho mẫu đơn Hàn Quốc."),
            arrayOf("Nho đen", "Nho", 110000.0, 10, "2021-03-01", "Lô nho đen đã hết hạn.")
        )
        for (f in fruits) {
            db.insert("Fruit", null, ContentValues().apply {
                put("name", f[0] as String)
                put("category", f[1] as String)
                put("price", f[2] as Double)
                put("quantity", f[3] as Int)
                put("expiryDate", f[4] as String)
                put("description", f[5] as String)
            })
        }
    }

    companion object {
        const val DB_NAME = "fruit_shop.db"
        const val DB_VERSION = 1
    }
}
