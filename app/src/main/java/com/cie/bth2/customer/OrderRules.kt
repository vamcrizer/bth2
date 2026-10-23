package com.cie.bth2.customer

import com.cie.bth2.data.Fruit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Nghiệp vụ chặn mua hàng: hết hạn, hết số lượng, vượt tồn kho. */
object OrderRules {

    enum class Result { OK, EXPIRED, OUT_OF_STOCK, INVALID_QUANTITY, NOT_ENOUGH_STOCK }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Ngày yyyy-MM-dd so sánh được theo chuỗi; còn dùng được trong ngày hết hạn. */
    fun isExpired(fruit: Fruit, today: String = today()): Boolean = fruit.expiryDate < today

    fun canBuy(fruit: Fruit, today: String = today()): Boolean =
        !isExpired(fruit, today) && fruit.quantity > 0

    fun check(fruit: Fruit, amount: Int, today: String = today()): Result = when {
        isExpired(fruit, today) -> Result.EXPIRED
        fruit.quantity <= 0 -> Result.OUT_OF_STOCK
        amount <= 0 -> Result.INVALID_QUANTITY
        amount > fruit.quantity -> Result.NOT_ENOUGH_STOCK
        else -> Result.OK
    }
}
