package com.cie.bth2.ui.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cie.bth2.R

/** Child screens share the same visible back action and Android back stack. */
abstract class BackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeActionContentDescription(R.string.navigate_back)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
