package com.cie.bth2.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import com.cie.bth2.R
import com.cie.bth2.data.AccountRepository.Account

class RegisterActivity : AuthActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.register_title)
        showLayout(R.layout.activity_register)
        findViewById<View>(R.id.alternate).setOnClickListener { finish() }
        findViewById<View>(R.id.submit).setOnClickListener { register() }
    }

    private fun register() {
        val ids = listOf(R.id.fullname, R.id.phone, R.id.email, R.id.username, R.id.password, R.id.confirm_password)
        val fields = ids.map { findViewById<EditText>(it) }
        fields.forEach { it.error = null }
        val (name, phone, email, username, password) = fields.take(5).map { it.text.toString() }
        val confirmation = fields[5].text.toString()
        val errors = listOf(
            if (name.isBlank()) R.string.required else null,
            if (!phone.trim().matches(Regex("[+0-9 ()-]{8,20}")) || phone.count { it.isDigit() } !in 8..15) R.string.invalid_phone else null,
            if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) R.string.invalid_email else null,
            if (!username.trim().matches(Regex("[A-Za-z0-9_.]{3,30}"))) R.string.invalid_username else null,
            if (password.isBlank() || password.length < 6) R.string.short_password else null,
            if (confirmation != password) R.string.password_mismatch else null
        )
        errors.forEachIndexed { i, error -> if (error != null) fields[i].error = getString(error) }
        val first = errors.indexOfFirst { it != null }
        if (first >= 0) { fields[first].requestFocus(); return }
        presenter.register(name, phone, email, username, password)
    }

    override fun onResult(account: Account?) {
        if (account != null) AuthNavigator.home(this, account)
    }
}
