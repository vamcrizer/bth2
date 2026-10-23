package com.cie.bth2.data

import android.content.ContentValues

/** Customer profile operations, linked to a Customer-role account. */
class CustomerRepository(private val databaseHelper: DatabaseHelper) {

    data class Customer(
        val id: Long,
        val fullName: String,
        val phone: String,
        val email: String,
        val accountId: Long,
        val username: String
    )

    fun getById(id: Long): Customer? = queryCustomers(
        "c.${DatabaseHelper.COL_ID} = ?", arrayOf(id.toString())
    ).firstOrNull()

    fun getByAccountId(accountId: Long): Customer? = queryCustomers(
        "c.${DatabaseHelper.COL_ACCOUNT_ID} = ?", arrayOf(accountId.toString())
    ).firstOrNull()

    fun getAll(): List<Customer> = queryCustomers(null, null)

    /** Creates a customer profile for an existing Customer-role account. */
    fun insert(fullName: String, phone: String, email: String, accountId: Long): Long {
        validate(fullName, accountId)
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_FULL_NAME, fullName.trim())
            put(DatabaseHelper.COL_PHONE, phone.trim())
            put(DatabaseHelper.COL_EMAIL, email.trim())
            put(DatabaseHelper.COL_ACCOUNT_ID, accountId)
        }
        return databaseHelper.writableDatabase.insertOrThrow(DatabaseHelper.TABLE_CUSTOMER, null, values)
    }

    fun update(customer: Customer): Boolean {
        require(customer.id > 0) { "Customer ID must be positive" }
        validate(customer.fullName, customer.accountId)
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_FULL_NAME, customer.fullName.trim())
            put(DatabaseHelper.COL_PHONE, customer.phone.trim())
            put(DatabaseHelper.COL_EMAIL, customer.email.trim())
        }
        return databaseHelper.writableDatabase.update(
            DatabaseHelper.TABLE_CUSTOMER, values,
            "${DatabaseHelper.COL_ID} = ? AND ${DatabaseHelper.COL_ACCOUNT_ID} = ?",
            arrayOf(customer.id.toString(), customer.accountId.toString())
        ) > 0
    }

    fun delete(id: Long): Boolean = databaseHelper.writableDatabase.delete(
        DatabaseHelper.TABLE_CUSTOMER,
        "${DatabaseHelper.COL_ID} = ?",
        arrayOf(id.toString())
    ) > 0

    private fun queryCustomers(selection: String?, args: Array<String>?): List<Customer> =
        databaseHelper.readableDatabase.rawQuery(
            """SELECT c.${DatabaseHelper.COL_ID}, c.${DatabaseHelper.COL_FULL_NAME},
                c.${DatabaseHelper.COL_PHONE}, c.${DatabaseHelper.COL_EMAIL},
                c.${DatabaseHelper.COL_ACCOUNT_ID}, a.${DatabaseHelper.COL_USERNAME}
                FROM ${DatabaseHelper.TABLE_CUSTOMER} c
                INNER JOIN ${DatabaseHelper.TABLE_ACCOUNT} a
                    ON a.${DatabaseHelper.COL_ID} = c.${DatabaseHelper.COL_ACCOUNT_ID}
                ${selection?.let { "WHERE $it" } ?: ""}
                ORDER BY c.${DatabaseHelper.COL_FULL_NAME} COLLATE NOCASE ASC""".trimIndent(),
            args
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(
                    Customer(
                        id = cursor.getLong(0),
                        fullName = cursor.getString(1),
                        phone = cursor.getString(2),
                        email = cursor.getString(3),
                        accountId = cursor.getLong(4),
                        username = cursor.getString(5)
                    )
                )
            }
        }

    private fun validate(fullName: String, accountId: Long) {
        require(fullName.isNotBlank()) { "Customer name must not be blank" }
        require(accountId > 0) { "Account ID must be positive" }
        val account = databaseHelper.readableDatabase.query(
            DatabaseHelper.TABLE_ACCOUNT,
            arrayOf(DatabaseHelper.COL_ROLE),
            "${DatabaseHelper.COL_ID} = ?",
            arrayOf(accountId.toString()), null, null, null, "1"
        ).use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == DatabaseHelper.ROLE_CUSTOMER
        }
        require(account) { "Customer profile requires an existing Customer account" }
    }
}
