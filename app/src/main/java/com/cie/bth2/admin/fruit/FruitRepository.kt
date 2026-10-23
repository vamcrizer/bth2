package com.cie.bth2.admin.fruit

import android.content.Context
import com.cie.bth2.data.DatabaseHelper

/** Admin UI adapter over the shared application database repository. */
interface FruitRepository {
    fun all(): List<Fruit>
    fun save(fruit: Fruit)
    fun delete(id: Long)
    fun close()
}

class SqliteFruitRepository(
    context: Context,
    databaseName: String = DatabaseHelper.DATABASE_NAME
) : FruitRepository {
    private val helper = DatabaseHelper(context.applicationContext ?: context, databaseName)
    private val repository = com.cie.bth2.data.FruitRepository(helper)

    override fun all(): List<Fruit> = repository.getAll().map { it.toAdminFruit() }

    override fun save(fruit: Fruit) {
        require(FruitRules.validate(fruit) == null)
        val sharedFruit = com.cie.bth2.data.FruitRepository.Fruit(
            id = fruit.id,
            name = fruit.name,
            category = fruit.category,
            price = fruit.price,
            quantity = fruit.quantity,
            expiryDate = fruit.expiryDate,
            description = fruit.description
        )
        if (fruit.id == 0L) repository.insert(sharedFruit)
        else check(repository.update(sharedFruit)) { "Fruit no longer exists" }
    }

    override fun delete(id: Long) {
        check(repository.delete(id)) { "Fruit no longer exists" }
    }

    override fun close() = helper.close()

    private fun com.cie.bth2.data.FruitRepository.Fruit.toAdminFruit() = Fruit(
        id = id,
        name = name,
        category = category,
        price = price,
        quantity = quantity,
        expiryDate = expiryDate,
        description = description
    )
}
