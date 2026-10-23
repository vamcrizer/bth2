package com.cie.bth2.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.cie.bth2.R
import com.cie.bth2.data.AccountRepository.Account
import com.cie.bth2.data.DatabaseHelper

/** Integration point: keep this guard when adding each team's role-specific screens. */
abstract class RoleHomeActivity : AuthActivity() {
    abstract val requiredRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLayout(R.layout.activity_role_home)
        findViewById<View>(R.id.submit).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout) { _, _ -> AuthNavigator.logout(this) }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.home_content).visibility = View.INVISIBLE
        presenter.restore(SessionManager(this).current())
    }

    override fun onResult(account: Account?) {
        when {
            account == null -> AuthNavigator.logout(this)
            account.role != requiredRole -> AuthNavigator.home(this, account)
            else -> {
                SessionManager(this).save(account)
                title = getString(if (requiredRole == DatabaseHelper.ROLE_ADMIN) R.string.admin_title else R.string.customer_title)
                findViewById<TextView>(R.id.welcome).text = getString(R.string.welcome, account.username)
                findViewById<TextView>(R.id.role_description).setText(
                    if (requiredRole == DatabaseHelper.ROLE_ADMIN) R.string.admin_description else R.string.customer_description)
                findViewById<View>(R.id.home_content).visibility = View.VISIBLE
            }
        }
    }

    override fun onError(duplicateUsername: Boolean) {
        super.onError(duplicateUsername)
        findViewById<View>(R.id.submit).isEnabled = false
        // Fail closed; the user can reopen the screen to retry a database failure.
    }
}

class AdminHomeActivity : RoleHomeActivity() {
    override val requiredRole = DatabaseHelper.ROLE_ADMIN
}

class CustomerHomeActivity : RoleHomeActivity() {
    override val requiredRole = DatabaseHelper.ROLE_CUSTOMER
}
