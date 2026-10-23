package com.cie.bth2.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** Basic account operations shared by authentication and customer modules. */
class AccountRepository(private val databaseHelper: DatabaseHelper) {

    data class Account(
        val id: Long,
        val username: String,
        val role: String
    )

    /** Returns an account on a matching username and password, or null. */
    fun authenticate(username: String, password: String): Account? {
        if (username.isBlank() || password.isBlank()) return null

        return databaseHelper.readableDatabase.query(
            DatabaseHelper.TABLE_ACCOUNT,
            arrayOf(DatabaseHelper.COL_ID, DatabaseHelper.COL_USERNAME, DatabaseHelper.COL_ROLE),
            "${DatabaseHelper.COL_USERNAME} = ? AND ${DatabaseHelper.COL_PASSWORD} = ?",
            arrayOf(username.trim(), password),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            Account(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROLE))
            )
        }
    }

    /** Creates an account and returns its row ID, or -1 when username already exists. */
    fun createAccount(username: String, password: String, role: String): Long {
        require(username.isNotBlank()) { "Username must not be blank" }
        require(password.isNotBlank()) { "Password must not be blank" }
        require(role == DatabaseHelper.ROLE_ADMIN || role == DatabaseHelper.ROLE_CUSTOMER) {
            "Unsupported account role"
        }

        val values = ContentValues().apply {
            put(DatabaseHelper.COL_USERNAME, username.trim())
            put(DatabaseHelper.COL_PASSWORD, password)
            put(DatabaseHelper.COL_ROLE, role)
        }
        return try {
            databaseHelper.writableDatabase.insertOrThrow(DatabaseHelper.TABLE_ACCOUNT, null, values)
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            -1L
        }
    }

    /** Runs a caller-supplied write transaction against the app database. */
    fun <T> inTransaction(block: (SQLiteDatabase) -> T): T {
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        return try {
            block(db).also { db.setTransactionSuccessful() }
        } finally {
            db.endTransaction()
        }
    }
}
