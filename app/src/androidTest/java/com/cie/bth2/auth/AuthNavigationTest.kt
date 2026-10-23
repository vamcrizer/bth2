package com.cie.bth2.auth

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cie.bth2.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthNavigationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun clearSession() { SessionManager(context).clear() }
    @After fun cleanup() { SessionManager(context).clear() }

    private fun awaitText(text: String) {
        val deadline = System.currentTimeMillis() + 8000
        var failure: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try { onView(withText(text)).check(matches(isDisplayed())); return }
            catch (error: AssertionError) { failure = error }
            catch (error: androidx.test.espresso.NoMatchingViewException) { failure = error }
            Thread.sleep(100)
        }
        throw AssertionError("Expected visible text: $text", failure)
    }

    @Test fun adminLoginAndLogoutClearSessionAndBackStack() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.username)).perform(typeText("admin"))
            onView(withId(R.id.password)).perform(typeText("admin123"), closeSoftKeyboard())
            onView(withId(R.id.submit)).perform(scrollTo(), click())
            awaitText(context.getString(R.string.admin_description))
            onView(withId(R.id.submit)).perform(scrollTo(), click())
            onView(withId(android.R.id.button1)).perform(click())
            awaitText(context.getString(R.string.login_subtitle))
            org.junit.Assert.assertNull(SessionManager(context).current())
            pressBackUnconditionally()
            org.junit.Assert.assertNull(SessionManager(context).current())
        }
    }

    @Test fun savedCustomerSessionRoutesToCustomerHome() {
        val account = AuthRepository(context).login("customer", "customer123")!!
        SessionManager(context).save(account)
        ActivityScenario.launch(LoginActivity::class.java).use {
            awaitText(context.getString(R.string.customer_description))
        }
    }

    @Test fun wrongRoleCannotOpenAdminScreen() {
        val account = AuthRepository(context).login("customer", "customer123")!!
        SessionManager(context).save(account)
        ActivityScenario.launch(AdminHomeActivity::class.java).use {
            awaitText(context.getString(R.string.customer_description))
        }
    }

    @Test fun unauthenticatedAdminScreenReturnsToLogin() {
        ActivityScenario.launch(AdminHomeActivity::class.java).use {
            awaitText(context.getString(R.string.login_subtitle))
        }
    }

    @Test fun registerFormCreatesCustomerAndOpensCustomerHome() {
        val username = "ui_${System.nanoTime()}"
        try {
            ActivityScenario.launch(RegisterActivity::class.java).use {
                val inputs = listOf(
                    R.id.fullname to "Le Duy Anh", R.id.phone to "0901234567",
                    R.id.email to "anh@example.com", R.id.username to username,
                    R.id.password to "secret123", R.id.confirm_password to "secret123"
                )
                inputs.forEach { (id, value) ->
                    onView(withId(id)).perform(scrollTo(), replaceText(value), closeSoftKeyboard())
                }
                onView(withId(R.id.submit)).perform(scrollTo(), click())
                awaitText(context.getString(R.string.customer_description))
                org.junit.Assert.assertEquals(username, SessionManager(context).current()?.username)
            }
        } finally {
            com.cie.bth2.data.DatabaseHelper(context).use { helper ->
                helper.writableDatabase.delete("Account", "username = ?", arrayOf(username))
            }
        }
    }


    @Test fun customerHomeOpensShopProfileAndHistory() {
        SessionManager(context).save(AuthRepository(context).login("customer", "customer123")!!)
        ActivityScenario.launch(CustomerHomeActivity::class.java).use {
            awaitText(context.getString(R.string.customer_description))
            onView(withId(R.id.open_customer_shop)).perform(scrollTo(), click())
            onView(withId(R.id.etSearch)).check(matches(isDisplayed()))
            pressBackUnconditionally()
            awaitText(context.getString(R.string.customer_description))
            onView(withId(R.id.open_customer_profile)).perform(scrollTo(), click())
            onView(withId(R.id.inputFullName)).check(matches(isDisplayed()))
            onView(withId(R.id.buttonOrderHistory)).perform(scrollTo(), click())
            onView(withId(R.id.recyclerOrders)).check(matches(isDisplayed()))
            pressBackUnconditionally()
            pressBackUnconditionally()
        }
    }

    @Test fun adminHomeOpensStockAndOrderManagement() {
        SessionManager(context).save(AuthRepository(context).login("admin", "admin123")!!)
        ActivityScenario.launch(AdminHomeActivity::class.java).use {
            awaitText(context.getString(R.string.admin_description))
            onView(withId(R.id.open_admin_fruit)).perform(scrollTo(), click())
            awaitText("Kho trái cây")
            pressBackUnconditionally()
            awaitText(context.getString(R.string.admin_description))
            onView(withId(R.id.open_admin_orders)).perform(scrollTo(), click())
            onView(withId(R.id.textStatTotal)).check(matches(isDisplayed()))
            pressBackUnconditionally()
        }
    }
}
