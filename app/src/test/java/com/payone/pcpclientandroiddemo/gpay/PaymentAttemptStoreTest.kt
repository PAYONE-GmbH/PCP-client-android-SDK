package com.payone.pcpclientandroiddemo.gpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Recreation, uncertain POSTs and authoritative recovery use the same persisted guard. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PaymentAttemptStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val config = ConfigService("https://example.com/api/", "merchant")
    @Before fun clear() { context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun cancellationOnlyReleasesUnsubmittedAttempt() {
        val store = PaymentAttemptStore(context)
        store.start("ref", "EXPRESS", config, "standard")
        store.selectShipping("ref", "express")
        assertEquals("express", PaymentAttemptStore(context).current()!!.shippingOption)
        assertThrows(IllegalStateException::class.java) { store.start("overlap", "STANDARD", config, "standard") }
        store.cancel("wrong-ref")
        assertTrue(store.blocked())
        store.cancel("ref")
        assertFalse(store.blocked())
    }

    @Test fun uncertainSubmissionSurvivesRecreationAndBlocksBothModes() {
        val store = PaymentAttemptStore(context)
        store.start("ref", "STANDARD", config, "standard")
        assertTrue(store.beginSubmission("ref"))
        val recreated = PaymentAttemptStore(context)
        assertFalse(recreated.beginSubmission("ref"))
        recreated.cancel("ref")
        assertTrue(recreated.blocked())
        assertThrows(IllegalStateException::class.java) { recreated.newOrder() }
        assertThrows(IllegalStateException::class.java) { recreated.start("new", "EXPRESS", config, "standard") }
        assertTrue(assertThrows(IllegalArgumentException::class.java) { recreated.reconcile() }.message!!.contains("ref"))
    }

    @Test fun onlyDefinitiveStatusGetAllowsExplicitNewOrder() {
        val server = MockWebServer()
        server.start()
        try {
            val store = PaymentAttemptStore(context)
            store.start("ref", "EXPRESS", ConfigService(server.url("/api/").toString(), "merchant"), "express")
            store.beginSubmission("ref")
            store.created("ref", PaymentOutcome("pending", "case", "checkout"))
            server.enqueue(MockResponse().setBody("""{"statusOutput":{"paymentStatus":"PAYMENT_PENDING"}}"""))
            assertEquals("pending", store.reconcile().state)
            assertThrows(IllegalStateException::class.java) { store.newOrder() }
            server.enqueue(MockResponse().setBody("""{"statusOutput":{"paymentStatus":"PAYMENT_COMPLETED"}}"""))
            assertEquals("succeeded", PaymentAttemptStore(context).reconcile().state)
            store.newOrder()
            assertFalse(store.blocked())
            assertEquals(2, server.requestCount)
            assertEquals("GET", server.takeRequest().method)
        } finally { server.shutdown() }
    }

    @Test fun legacyUncertainAttemptsRemainBlockedAcrossUpgrade() {
        context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).edit().putString("state", "SUBMITTED").putString("reference", "legacy-ref").commit()
        assertTrue(PaymentAttemptStore(context).blocked())
        assertThrows(IllegalStateException::class.java) { PaymentAttemptStore(context).start("new", "STANDARD", config, "standard") }
    }
}
