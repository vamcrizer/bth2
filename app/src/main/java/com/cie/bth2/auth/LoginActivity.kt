package com.cie.bth2.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.view.View
import com.cie.bth2.R
import com.cie.bth2.data.AccountRepository.Account

class LoginActivity : AuthActivity() {
    private var restoring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.login_title)
        showLayout(R.layout.activity_login)
        val username = findViewById<EditText>(R.id.username)
        val password = findViewById<EditText>(R.id.password)
        findViewById<View>(R.id.submit).setOnClickListener {
            username.error = null
            password.error = null
            when {
                username.text.isBlank() -> { username.error = getString(R.string.required); username.requestFocus() }
                password.text.isBlank() -> { password.error = getString(R.string.required); password.requestFocus() }
                else -> presenter.login(username.text.toString(), password.text.toString())
            }
        }
        findViewById<View>(R.id.alternate).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        val saved = SessionManager(this).current()
        if (saved != null) {
            restoring = true
            presenter.restore(saved)
        }
    }

    override fun onResult(account: Account?) {
        if (account != null) AuthNavigator.home(this, account)
        else {
            SessionManager(this).clear()
            if (!restoring) findViewById<TextView>(R.id.message).setText(R.string.invalid_credentials)
        }
        restoring = false
    }

    override fun onError(duplicateUsername: Boolean) {
        restoring = false
        super.onError(duplicateUsername)
    }
}
