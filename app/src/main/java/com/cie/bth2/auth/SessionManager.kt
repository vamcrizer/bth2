package com.cie.bth2.auth

import android.content.Context
import com.cie.bth2.data.AccountRepository.Account
import com.cie.bth2.data.DatabaseHelper

/** Stores only identity and role; credentials stay out of preferences. */
class SessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun save(account: Account) {
        require(account.id > 0 && isSupportedRole(account.role))
        prefs.edit().clear().putLong("accountId", account.id)
            .putString("username", account.username).putString("role", account.role).apply()
    }

    fun current(): Account? {
        val id = prefs.getLong("accountId", -1)
        val username = prefs.getString("username", null)
        val role = prefs.getString("role", null)
        if (id <= 0 || username.isNullOrBlank() || !isSupportedRole(role)) return null
        return Account(id, username, role!!)
    }

    fun clear() { prefs.edit().clear().apply() }

    companion object {
        const val FILE_NAME = "auth_session"
        fun isSupportedRole(role: String?) =
            role == DatabaseHelper.ROLE_ADMIN || role == DatabaseHelper.ROLE_CUSTOMER
    }
}
