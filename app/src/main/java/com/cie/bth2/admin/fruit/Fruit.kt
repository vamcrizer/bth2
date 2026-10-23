package com.cie.bth2.admin.fruit

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Fruit(
    val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val expiryDate: String,
    val description: String
)

enum class ExpiryFilter { ALL, EXPIRED, TODAY, VALID }

object FruitRules {
    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun validDate(value: String): Boolean {
        if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) return false
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            format.parse(value) != null
        } catch (_: java.text.ParseException) { false }
    }

    fun validate(fruit: Fruit): String? = when {
        fruit.name.isBlank() -> "Vui lòng nhập tên trái cây."
        fruit.category.isBlank() -> "Vui lòng nhập danh mục."
        !fruit.price.isFinite() || fruit.price < 0 -> "Giá phải là số không âm."
        fruit.quantity < 0 -> "Số lượng phải là số nguyên không âm."
        !validDate(fruit.expiryDate) -> "Hạn sử dụng không hợp lệ (yyyy-MM-dd)."
        else -> null
    }

    // A fruit remains valid throughout its expiry date in the device's local timezone.
    fun expired(fruit: Fruit, today: String = today()): Boolean = fruit.expiryDate < today

    fun filter(items: List<Fruit>, query: String, category: String?, expiry: ExpiryFilter,
               today: String = today()): List<Fruit> = items.filter {
        (it.name.contains(query.trim(), ignoreCase = true) ||
            it.description.contains(query.trim(), ignoreCase = true)) &&
            (category == null || it.category == category) && when (expiry) {
                ExpiryFilter.ALL -> true
                ExpiryFilter.EXPIRED -> expired(it, today)
                ExpiryFilter.TODAY -> it.expiryDate == today
                ExpiryFilter.VALID -> !expired(it, today)
            }
    }
}
