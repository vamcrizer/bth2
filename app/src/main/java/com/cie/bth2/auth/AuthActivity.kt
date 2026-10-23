package com.cie.bth2.auth

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cie.bth2.R

abstract class AuthActivity : AppCompatActivity(), AuthPresenter.View {
    protected lateinit var presenter: AuthPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = AuthPresenter(AuthRepository(applicationContext), this)
    }

    protected fun showLayout(layout: Int) {
        setContentView(layout)
        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    override fun onBusy(busy: Boolean) {
        findViewById<View>(R.id.progress).visibility = if (busy) View.VISIBLE else View.GONE
        findViewById<View>(R.id.submit)?.isEnabled = !busy
        findViewById<View>(R.id.alternate)?.isEnabled = !busy
        if (busy) findViewById<TextView>(R.id.message).text = ""
    }

    override fun onError(duplicateUsername: Boolean) {
        findViewById<TextView>(R.id.message).setText(
            if (duplicateUsername) R.string.username_taken else R.string.auth_error)
    }

    override fun onDestroy() {
        presenter.detach()
        super.onDestroy()
    }
}
