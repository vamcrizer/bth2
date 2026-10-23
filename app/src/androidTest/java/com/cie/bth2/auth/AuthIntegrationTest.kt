package com.cie.bth2.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cie.bth2.data.AccountRepository
import com.cie.bth2.data.CustomerRepository
import com.cie.bth2.data.DatabaseHelper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthIntegrationTest {
    private lateinit var context: Context
    private lateinit var repository: AuthRepository
    private lateinit var sessions: SessionManager
    private val usernames = mutableListOf<String>()

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = AuthRepository(context)
        sessions = SessionManager(context)
        sessions.clear()
    }

    @After fun tearDown() {
        sessions.clear()
        DatabaseHelper(context).use { helper ->
            usernames.forEach { helper.writableDatabase.delete("Account", "username = ?", arrayOf(it)) }
        }
    }

    private fun username(): String = "test_${System.nanoTime()}".also { usernames.add(it) }
    private fun register(name: String = username()) =
        repository.register("Lê Duy Anh", "0901234567", "anh@example.com", name, "secret123")

    @Test fun seedAccountsAuthenticateWithCorrectRoles() {
        assertEquals("Admin", repository.login("admin", "admin123")?.role)
        assertEquals("Customer", repository.login("customer", "customer123")?.role)
        assertNull(repository.login("admin", "wrong"))
        assertNull(repository.login("' OR 1=1 --", "wrong"))
    }

    @Test fun registrationCreatesLinkedCustomerAndCanLogin() {
        val account = register()
        assertEquals("Customer", account.role)
        assertEquals(account, repository.login(" ${account.username} ", "secret123"))
        DatabaseHelper(context).use { helper ->
            val profile = CustomerRepository(helper).getByAccountId(account.id)
            assertEquals("Lê Duy Anh", profile?.fullName)
            assertEquals("anh@example.com", profile?.email)
        }
    }

    @Test fun duplicateUsernameDoesNotChangeExistingAccount() {
        val original = register()
        try {
            register(original.username)
            fail("Expected duplicate username")
        } catch (_: AuthRepository.UsernameTakenException) { }
        assertEquals(original, repository.login(original.username, "secret123"))
    }

    @Test fun profileFailureRollsBackAccountCreation() {
        val name = username()
        DatabaseHelper(context).use { helper ->
            helper.writableDatabase.execSQL("CREATE TRIGGER auth_test_failure BEFORE INSERT ON Customer BEGIN SELECT RAISE(ABORT, 'test failure'); END")
            try {
                try { register(name); fail("Expected profile failure") }
                catch (_: android.database.sqlite.SQLiteConstraintException) { }
                assertNull(repository.login(name, "secret123"))
            } finally {
                helper.writableDatabase.execSQL("DROP TRIGGER auth_test_failure")
            }
        }
    }

    @Test fun sessionSurvivesNewManagerAndLogoutClearsEverything() {
        val account = register()
        sessions.save(account)
        assertEquals(account, SessionManager(context).current())
        val values = context.getSharedPreferences(SessionManager.FILE_NAME, Context.MODE_PRIVATE).all
        assertEquals(setOf("accountId", "username", "role"), values.keys)
        sessions.clear()
        assertNull(SessionManager(context).current())
    }

    @Test fun restoredRoleComesFromDatabaseAndDeletedAccountIsRejected() {
        val account = register()
        val forged = account.copy(role = "Admin")
        assertEquals("Customer", repository.restore(forged)?.role)
        DatabaseHelper(context).use { helper ->
            helper.writableDatabase.delete("Account", "id = ?", arrayOf(account.id.toString()))
        }
        assertNull(repository.restore(account))
    }

    @Test fun unsupportedRoleCannotBeSaved() {
        try {
            sessions.save(AccountRepository.Account(1, "test", "Unknown"))
            fail("Expected unsupported role rejection")
        } catch (_: IllegalArgumentException) { }
        assertNull(sessions.current())
    }
}
