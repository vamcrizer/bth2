package com.cie.bth2.data

/** expiryDate lưu dạng yyyy-MM-dd. */
data class Fruit(
    val id: Long,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val expiryDate: String,
    val description: String
)
