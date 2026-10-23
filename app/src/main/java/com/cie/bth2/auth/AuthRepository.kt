package com.cie.bth2.auth

import android.content.Context
import com.cie.bth2.data.AccountRepository
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper

/** Each operation owns its database connection and is called on a worker thread. */
class AuthRepository(private val context: Context) {
    fun login(username: String, password: String): AccountRepository.Account? =
        DatabaseHelper(context).use { helper ->
            AccountRepository(helper).authenticate(username.trim(), password)
                ?.takeIf { SessionManager.isSupportedRole(it.role) }
        }

    fun register(fullName: String, phone: String, email: String, username: String,
                 password: String): AccountRepository.Account = DatabaseHelper(context).use { helper ->
        require(fullName.isNotBlank() && username.isNotBlank() && password.isNotBlank() && password.length >= 6)
        val accounts = AccountRepository(helper)
        accounts.inTransaction {
            // Public registration can never create an administrator.
            val id = accounts.createAccount(username.trim(), password, DatabaseHelper.ROLE_CUSTOMER)
            if (id == -1L) throw UsernameTakenException()
            CustomerRepository(helper).insert(fullName.trim(), phone.trim(), email.trim(), id)
            AccountRepository.Account(id, username.trim(), DatabaseHelper.ROLE_CUSTOMER)
        }
    }

    /** Re-check SQLite instead of trusting a cached role when restoring a session. */
    fun restore(saved: AccountRepository.Account?): AccountRepository.Account? {
        if (saved == null) return null
        return DatabaseHelper(context).use { helper ->
            helper.readableDatabase.query(DatabaseHelper.TABLE_ACCOUNT,
                arrayOf(DatabaseHelper.COL_ID, DatabaseHelper.COL_USERNAME, DatabaseHelper.COL_ROLE),
                "id = ?", arrayOf(saved.id.toString()), null, null, null, "1").use { cursor ->
                if (!cursor.moveToFirst()) null else AccountRepository.Account(
                    cursor.getLong(0), cursor.getString(1), cursor.getString(2)
                ).takeIf { it.username == saved.username && SessionManager.isSupportedRole(it.role) }
            }
        }
    }

    class UsernameTakenException : Exception()
}
