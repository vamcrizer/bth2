package com.cie.bth2.customer

import android.content.Context
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper
import com.cie.bth2.data.Fruit
import com.cie.bth2.data.OrderRepository

/** Customer-facing adapter backed by the shared database core. */
class CustomerShopRepository(context: Context, databaseName: String = DatabaseHelper.DATABASE_NAME) {

    private val database = DatabaseHelper(context.applicationContext, databaseName)
    private val fruits = com.cie.bth2.data.FruitRepository(database)
    private val customers = CustomerRepository(database)
    private val orders = OrderRepository(database)

    /** keyword/category rỗng = không lọc; includeExpired=false chỉ lấy hàng còn hạn. */
    fun searchFruits(keyword: String, category: String?, includeExpired: Boolean): List<Fruit> =
        fruits.search(keyword, category).map { it.toCustomerFruit() }
            .filter { includeExpired || !OrderRules.isExpired(it) }

    fun getCategories(): List<String> = fruits.getAll().map { it.category }.distinct().sorted()

    fun getFruit(id: Long): Fruit? = fruits.getById(id)?.toCustomerFruit()

    sealed class PlaceOrderResult {
        data class Success(val orderId: Long, val total: Double) : PlaceOrderResult()
        data class Rejected(val reason: OrderRules.Result) : PlaceOrderResult()
        object FruitNotFound : PlaceOrderResult()
        object CustomerNotFound : PlaceOrderResult()
    }

    /** Revalidates through the core order transaction and reserves stock atomically. */
    fun placeOrder(customerId: Long, fruitId: Long, amount: Int): PlaceOrderResult {
        if (customers.getById(customerId) == null) return PlaceOrderResult.CustomerNotFound
        val fruit = getFruit(fruitId) ?: return PlaceOrderResult.FruitNotFound
        val validation = OrderRules.check(fruit, amount)
        if (validation != OrderRules.Result.OK) return PlaceOrderResult.Rejected(validation)

        return try {
            val orderId = orders.placeOrder(
                customerId,
                listOf(OrderRepository.OrderItem(fruitId, amount))
            )
            val total = orders.getById(orderId)?.totalAmount ?: (fruit.price * amount)
            PlaceOrderResult.Success(orderId, total)
        } catch (error: IllegalArgumentException) {
            val latest = getFruit(fruitId) ?: return PlaceOrderResult.FruitNotFound
            val reason = OrderRules.check(latest, amount)
            PlaceOrderResult.Rejected(
                if (reason == OrderRules.Result.OK) OrderRules.Result.NOT_ENOUGH_STOCK else reason
            )
        }
    }

    fun close() = database.close()

    private fun com.cie.bth2.data.FruitRepository.Fruit.toCustomerFruit() = Fruit(
        id = id,
        name = name,
        category = category,
        price = price,
        quantity = quantity,
        expiryDate = expiryDate,
        description = description
    )
}
