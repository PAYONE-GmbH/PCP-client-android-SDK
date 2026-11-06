package com.payone.pcpclientandroiddemo.gpay.checkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CheckoutSessionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clearFixtureState() {
        context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun cancelBeforeGooglePayReturnsDoesNotCreateCheckoutAndReleasesBothModes() {
        val session = CheckoutSession(context)
        session.start("standard-ref", CheckoutMode.STANDARD, CheckoutConfig(), "standard")
        assertEquals("OPEN", CheckoutSession(context).state())
        assertThrows(IllegalStateException::class.java) { session.start("overlap", CheckoutMode.EXPRESS, CheckoutConfig(), "express") }
        session.cancelBeforeSubmission()
        assertEquals("IDLE", session.state())
        session.start("express-ref", CheckoutMode.EXPRESS, CheckoutConfig(basketAmount = 2000), "express")
        assertEquals(2000L, CheckoutSession(context).config().basketAmount)
        assertEquals(CheckoutMode.EXPRESS, CheckoutSession(context).mode())
    }

    @Test fun createdButRejectedCheckoutKeepsIdsAndNeverResubmits() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(CheckoutTest.response("REJECTED", false).toString()))
            val session = CheckoutSession(context)
            session.start("ref", CheckoutMode.EXPRESS, CheckoutConfig(backendUrl = server.url("/").toString()), "express")
            assertFalse(session.submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)))
            val recreated = CheckoutSession(context)
            assertEquals("CREATED", recreated.state())
            assertEquals(CheckoutTest.CHECKOUT_ID, recreated.outcome()!!.checkoutId)
            recreated.cancelBeforeSubmission()
            assertFalse(recreated.submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)))
            recreated.start("new", CheckoutMode.STANDARD, CheckoutConfig(), "standard")
            assertEquals("new", recreated.reference())
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun uncertainHttpFailureAllowsNewCheckoutButNeverReplaysPreviousPayment() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(503))
            val session = CheckoutSession(context)
            session.start("ref", CheckoutMode.STANDARD, CheckoutConfig(backendUrl = server.url("/").toString()), "express")
            assertThrows(IllegalStateException::class.java) { session.submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)) }
            assertEquals("SUBMITTED", session.state())
            assertFalse(session.submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)))
            assertEquals(1, server.requestCount)
            val saved = context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).all.toString()
            assertFalse(saved.contains("opaque-token"))
            assertFalse(saved.contains("buyer@example.test"))
            assertFalse(session.isSubmitting())
            session.start("new-ref", CheckoutMode.EXPRESS, CheckoutConfig(), "standard")
            assertEquals("new-ref", session.reference())
            val history = context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).getStringSet("history", emptySet())!!
            assertTrue(history.any { it.contains("ref") && it.contains("SUBMITTED") })
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun confirmedAuthorizationReturnsSuccessOnRepeatedCallbackWithoutSecondRequest() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(CheckoutTest.response("PENDING_CAPTURE").toString()))
            val session = CheckoutSession(context)
            session.start("ref", CheckoutMode.EXPRESS, CheckoutConfig(backendUrl = server.url("/").toString()), "express")
            assertTrue(session.submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)))
            assertTrue(CheckoutSession(context).submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)))
            assertEquals("CONFIRMED", session.state())
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun legacySubmittedStateDoesNotPermanentlyDisableNewCheckouts() {
        val prefs = context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE)
        prefs.edit().putString("state", "SUBMITTED").putString("reference", "legacy-ref").commit()
        val session = CheckoutSession(context)
        assertFalse(session.isSubmitting())
        session.start("new-ref", CheckoutMode.STANDARD, CheckoutConfig(), "standard")
        assertEquals("OPEN", session.state())
        assertTrue(prefs.getStringSet("history", emptySet())!!.any { it.contains("legacy-ref") })
        session.cancelBeforeSubmission()
    }

    @Test fun activelyRunningHttpRequestStillBlocksOverlappingCheckout() {
        val release = CountDownLatch(1)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                check(release.await(5, TimeUnit.SECONDS))
                return MockResponse().setBody(CheckoutTest.response("PENDING_CAPTURE").toString())
            }
        }
        server.start()
        val session = CheckoutSession(context)
        session.start("active-ref", CheckoutMode.STANDARD, CheckoutConfig(backendUrl = server.url("/").toString()), "express")
        val task = FutureTask { session.submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)) }
        try {
            Thread(task).start()
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            assertTrue(session.isSubmitting())
            assertThrows(IllegalStateException::class.java) {
                session.start("overlap", CheckoutMode.EXPRESS, CheckoutConfig(), "standard")
            }
        } finally {
            release.countDown()
            task.get(5, TimeUnit.SECONDS)
            server.shutdown()
        }
        assertFalse(session.isSubmitting())
    }
}
