package com.cie.bth2.auth

import android.app.Activity
import android.content.Intent
import com.cie.bth2.data.AccountRepository.Account
import com.cie.bth2.data.DatabaseHelper

object AuthNavigator {
    fun home(activity: Activity, account: Account) {
        val destination = when (account.role) {
            DatabaseHelper.ROLE_ADMIN -> AdminHomeActivity::class.java
            DatabaseHelper.ROLE_CUSTOMER -> CustomerHomeActivity::class.java
            else -> { logout(activity); return }
        }
        SessionManager(activity).save(account)
        activity.startActivity(Intent(activity, destination).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        activity.finish()
    }

    fun logout(activity: Activity) {
        SessionManager(activity).clear()
        activity.startActivity(Intent(activity, LoginActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        activity.finish()
    }
}
