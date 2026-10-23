package com.cie.bth2.customer

import android.content.Context

/**
 * Contract đọc phiên với module Auth (feature/auth-sharedpref).
 * Auth cần ghi customer_id vào SharedPreferences "fruit_shop_session" sau khi login Customer.
 */
object CustomerSession {
    const val PREFS_NAME = "fruit_shop_session"
    const val KEY_CUSTOMER_ID = "customer_id"

    /** Tạm fallback về khách seed id=1 khi Auth chưa merge. */
    private const val FALLBACK_CUSTOMER_ID = 1L

    fun customerId(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_CUSTOMER_ID, FALLBACK_CUSTOMER_ID)
}
