package com.cie.bth2.admin.fruit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FruitRepositoryTest {
    @Test fun crudPersistsAcrossReopen() {
        // Instrumentation APK context keeps this test away from the app's real database.
        val context = InstrumentationRegistry.getInstrumentation().context
        context.deleteDatabase("admin_fruit.db")
        var repository = SqliteFruitRepository(context)
        try {
            repository.save(Fruit(name = "Cam ' ngọt", category = "Có múi", price = 20000.0,
                quantity = 12, expiryDate = "2026-10-23", description = "Mô tả"))
            val saved = repository.all().single()
            assertTrue(saved.id > 0)
            repository.save(saved.copy(quantity = 0, name = "Cam mới"))
            repository.close()
            repository = SqliteFruitRepository(context)
            assertEquals("Cam mới", repository.all().single().name)
            assertEquals(0, repository.all().single().quantity)
            repository.delete(saved.id)
            assertTrue(repository.all().isEmpty())
        } finally {
            repository.close()
            context.deleteDatabase("admin_fruit.db")
        }
    }
}
