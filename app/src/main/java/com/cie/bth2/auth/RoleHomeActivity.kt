package com.cie.bth2.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.cie.bth2.R
import com.cie.bth2.data.AccountRepository.Account
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.admin.fruit.AdminFruitActivity
import com.cie.bth2.customer.CustomerFruitListActivity
import com.cie.bth2.ui.order.AdminOrderManagementActivity
import com.cie.bth2.ui.order.CustomerOrderHistoryActivity
import com.cie.bth2.ui.profile.CustomerProfileActivity

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
                configureModuleNavigation(account)
                findViewById<View>(R.id.home_content).visibility = View.VISIBLE
            }
        }
    }

    override fun onError(duplicateUsername: Boolean) {
        super.onError(duplicateUsername)
        // Keep module entry points hidden if session verification fails, but allow logout.
        listOf(R.id.open_admin_fruit, R.id.open_admin_orders,
            R.id.open_customer_shop, R.id.open_customer_profile, R.id.open_customer_orders
        ).forEach { findViewById<View>(it).visibility = View.GONE }
        findViewById<TextView>(R.id.role_description).setText(R.string.auth_error)
        findViewById<View>(R.id.home_content).visibility = View.VISIBLE
        findViewById<View>(R.id.submit).isEnabled = true
    }

    private fun configureModuleNavigation(account: Account) {
        val isAdmin = account.role == DatabaseHelper.ROLE_ADMIN
        findViewById<View>(R.id.open_admin_fruit).apply {
            visibility = if (isAdmin) View.VISIBLE else View.GONE
            setOnClickListener { startActivity(Intent(this@RoleHomeActivity, AdminFruitActivity::class.java)) }
        }
        findViewById<View>(R.id.open_admin_orders).apply {
            visibility = if (isAdmin) View.VISIBLE else View.GONE
            setOnClickListener { startActivity(AdminOrderManagementActivity.newIntent(this@RoleHomeActivity)) }
        }

        val customer = if (isAdmin) null else DatabaseHelper(this).use {
            CustomerRepository(it).getByAccountId(account.id)
        }
        findViewById<View>(R.id.open_customer_shop).apply {
            visibility = if (customer != null) View.VISIBLE else View.GONE
            setOnClickListener { startActivity(Intent(this@RoleHomeActivity, CustomerFruitListActivity::class.java)) }
        }
        findViewById<View>(R.id.open_customer_profile).apply {
            visibility = if (customer != null) View.VISIBLE else View.GONE
            setOnClickListener {
                customer?.let { startActivity(CustomerProfileActivity.newIntent(this@RoleHomeActivity, it.id)) }
            }
        }
        findViewById<View>(R.id.open_customer_orders).apply {
            visibility = if (customer != null) View.VISIBLE else View.GONE
            setOnClickListener {
                customer?.let { startActivity(CustomerOrderHistoryActivity.newIntent(this@RoleHomeActivity, it.id)) }
            }
        }
    }
}

class AdminHomeActivity : RoleHomeActivity() {
    override val requiredRole = DatabaseHelper.ROLE_ADMIN
}

class CustomerHomeActivity : RoleHomeActivity() {
    override val requiredRole = DatabaseHelper.ROLE_CUSTOMER
}
