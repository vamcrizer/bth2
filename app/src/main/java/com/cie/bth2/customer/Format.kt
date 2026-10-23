package com.cie.bth2.customer

import java.text.NumberFormat
import java.util.Locale

internal fun formatVnd(amount: Double): String =
    NumberFormat.getInstance(Locale("vi", "VN")).format(amount.toLong()) + " ₫"
