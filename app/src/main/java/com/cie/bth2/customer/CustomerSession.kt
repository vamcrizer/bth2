package com.cie.bth2.customer

import android.content.Context
import com.cie.bth2.auth.SessionManager
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper

/** Resolves the authenticated account to the related Customer row. */
object CustomerSession {
    fun customerId(context: Context): Long? {
        val account = SessionManager(context).current() ?: return null
        if (account.role != DatabaseHelper.ROLE_CUSTOMER) return null
        return DatabaseHelper(context.applicationContext).use { database ->
            CustomerRepository(database).getByAccountId(account.id)?.id
        }
    }
}
