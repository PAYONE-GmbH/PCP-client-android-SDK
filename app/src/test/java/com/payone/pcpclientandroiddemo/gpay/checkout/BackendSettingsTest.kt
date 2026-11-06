package com.payone.pcpclientandroiddemo.gpay.checkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackendSettingsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clearSettings() {
        BackendSettings(context).reset()
        context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun persistsNormalizesAndResetsSharedUrl() {
        val settings = BackendSettings(context)
        assertEquals(CheckoutConfig().backendUrl, settings.configuration().backendUrl)
        settings.save("  HTTP://192.168.178.42:4000/custom/api  ")
        assertEquals("http://192.168.178.42:4000/custom/api/", BackendSettings(context).configuration().backendUrl)
        settings.save("https://example.test")
        assertEquals("https://example.test/", BackendSettings(context).configuration().backendUrl)
        settings.reset()
        assertEquals(CheckoutConfig().backendUrl, BackendSettings(context).configuration().backendUrl)
    }

    @Test fun invalidInputDoesNotReplaceSavedUrl() {
        val settings = BackendSettings(context)
        settings.save("http://10.0.2.2:4000/api/")
        listOf("", "localhost:3009", "ftp://example.test/", "https:///api/", "http://host:0/",
            "http://host:65536/", "http://host:abc/", "http://user:secret@host/", "http://host/?token=x",
            "http://host/#fragment", "http://host/a path").forEach { value ->
            assertThrows("Accepted $value", IllegalArgumentException::class.java) { settings.save(value) }
        }
        assertEquals("http://10.0.2.2:4000/api/", BackendSettings(context).configuration().backendUrl)
    }

    @Test fun savedUrlDrivesDiscoveryAndSessionKeepsEndpointAfterSettingsChange() {
        val server = MockWebServer()
        server.start()
        try {
            val settings = BackendSettings(context)
            settings.save(server.url("/custom/api").toString())
            server.enqueue(MockResponse().setBody("{\"merchantId\":\"demo-merchant\"}"))
            assertEquals("demo-merchant", CheckoutBackend(settings.configuration()).merchantId())
            assertEquals("/ui/config", server.takeRequest().path)
            val session = CheckoutSession(context)
            session.start("settings-ref", CheckoutMode.EXPRESS, settings.configuration(), "standard")
            settings.save("https://other.example.test/v1/")
            server.enqueue(MockResponse().setBody(CheckoutTest.response("PENDING_CAPTURE").toString()))
            assertTrue(CheckoutSession(context).submit(GooglePayExpressPayment.fromJson(CheckoutTest.PAYMENT)))
            assertEquals("/custom/api/commerce-cases", server.takeRequest().path)
            assertEquals("https://other.example.test/v1/", settings.configuration().backendUrl)
        } finally { server.shutdown() }
    }

    @Test fun connectionHintsExplainRoutingAndPreserveSafeHttpStatus() {
        val settings = BackendSettings(context)
        val local = settings.connectionError("http://localhost:4000/api/", IOException("private detail"))
        assertTrue(local.contains("adb reverse tcp:4000 tcp:4000"))
        assertFalse(local.contains("private detail"))
        assertTrue(settings.connectionError("http://10.0.2.2:4000/", IOException()).contains("emulator"))
        val remote = settings.connectionError("https://demo.example.test/api/", CheckoutBackendException(404, null))
        assertTrue(remote.contains("HTTP 404"))
        assertTrue(remote.contains("GET /ui/config"))
    }
}
