package com.cie.bth2.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

internal object DateValidation {
    fun isValid(value: String, pattern: String): Boolean {
        val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
        val position = ParsePosition(0)
        val date = format.parse(value, position) ?: return false
        return position.index == value.length && format.format(date) == value
    }
}
