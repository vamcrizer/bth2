package com.cie.bth2.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cie.bth2.R
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.ui.order.CustomerOrderHistoryActivity

/** View and edit the signed-in customer's profile. Start with [newIntent]. */
class CustomerProfileActivity : AppCompatActivity() {

    private lateinit var customerRepository: CustomerRepository
    private lateinit var customer: CustomerRepository.Customer

    private lateinit var inputFullName: EditText
    private lateinit var inputPhone: EditText
    private lateinit var inputEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_profile)
        title = getString(R.string.profile_title)

        customerRepository = CustomerRepository(DatabaseHelper(this))
        val customerId = intent.getLongExtra(EXTRA_CUSTOMER_ID, -1)
        customer = customerRepository.getById(customerId) ?: run {
            Toast.makeText(this, R.string.customer_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        inputFullName = findViewById(R.id.inputFullName)
        inputPhone = findViewById(R.id.inputPhone)
        inputEmail = findViewById(R.id.inputEmail)

        findViewById<TextView>(R.id.textUsername).text =
            getString(R.string.profile_username, customer.username)
        if (savedInstanceState == null) {
            inputFullName.setText(customer.fullName)
            inputPhone.setText(customer.phone)
            inputEmail.setText(customer.email)
        }

        findViewById<Button>(R.id.buttonSaveProfile).setOnClickListener { saveProfile() }
        findViewById<Button>(R.id.buttonOrderHistory).setOnClickListener {
            startActivity(CustomerOrderHistoryActivity.newIntent(this, customer.id))
        }
    }

    private fun saveProfile() {
        val fullName = inputFullName.text.toString().trim()
        val phone = inputPhone.text.toString().trim()
        val email = inputEmail.text.toString().trim()

        if (fullName.isEmpty()) {
            inputFullName.error = getString(R.string.profile_error_full_name)
            return
        }
        if (phone.isNotEmpty() && !phone.matches(PHONE_REGEX)) {
            inputPhone.error = getString(R.string.profile_error_phone)
            return
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.error = getString(R.string.profile_error_email)
            return
        }

        val updatedCustomer = customer.copy(fullName = fullName, phone = phone, email = email)
        val saved = try {
            customerRepository.update(updatedCustomer)
        } catch (_: IllegalArgumentException) {
            false
        }
        if (saved) customer = updatedCustomer
        Toast.makeText(
            this,
            if (saved) R.string.profile_saved else R.string.profile_save_failed,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        /** Customer.id (not Account.id), matching the customer_id session key. */
        const val EXTRA_CUSTOMER_ID = "extra_customer_id"
        private val PHONE_REGEX = Regex("^0\\d{9,10}$")

        fun newIntent(context: Context, customerId: Long): Intent =
            Intent(context, CustomerProfileActivity::class.java)
                .putExtra(EXTRA_CUSTOMER_ID, customerId)
    }
}
