package com.cie.bth2.admin.fruit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FruitRepositoryTest {
    @Test fun crudPersistsAcrossReopen() {
        // Use a dedicated test database in the target application context.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("admin_repository_tests.db")
        var repository = SqliteFruitRepository(context, "admin_repository_tests.db")
        try {
            repository.save(Fruit(name = "Cam ' ngọt", category = "Có múi", price = 20000.0,
                quantity = 12, expiryDate = "2026-10-23", description = "Mô tả"))
            val saved = repository.all().single { it.name == "Cam ' ngọt" }
            assertTrue(saved.id > 0)
            repository.save(saved.copy(quantity = 0, name = "Cam mới"))
            repository.close()
            repository = SqliteFruitRepository(context, "admin_repository_tests.db")
            assertEquals("Cam mới", repository.all().single { it.id == saved.id }.name)
            assertEquals(0, repository.all().single { it.id == saved.id }.quantity)
            repository.delete(saved.id)
            assertFalse(repository.all().any { it.id == saved.id })
        } finally {
            repository.close()
            context.deleteDatabase("admin_repository_tests.db")
        }
    }
}
