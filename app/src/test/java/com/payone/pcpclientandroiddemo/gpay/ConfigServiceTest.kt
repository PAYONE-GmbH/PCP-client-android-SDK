package com.payone.pcpclientandroiddemo.gpay

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payone.pcpclientandroiddemo.BuildConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Public configuration must be explicit; API prefixes survive normalization. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ConfigServiceTest {
    @Test fun savedSettingsOverrideBuildDefaultsAndClearingRestoresThem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("demo-backend", Context.MODE_PRIVATE)
        try {
            prefs.edit().putString("url", "https://override.example/api/")
                .putString("merchantId", "override-merchant").commit()
            val saved = ConfigService(context)
            assertEquals("https://override.example/api/", saved.backendUrl)
            assertEquals("override-merchant", saved.gatewayMerchantId)
            prefs.edit().clear().commit()
            val expected = runCatching { ConfigService(BuildConfig.BACKEND_BASE_URL, BuildConfig.GATEWAY_MERCHANT_ID) }
            val actual = runCatching { ConfigService(context) }
            assertEquals(expected.exceptionOrNull()?.message, actual.exceptionOrNull()?.message)
            if (expected.isSuccess) {
                assertEquals(expected.getOrThrow().backendUrl, actual.getOrThrow().backendUrl)
                assertEquals(expected.getOrThrow().gatewayMerchantId, actual.getOrThrow().gatewayMerchantId)
            }
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun missingOrUnsafeSettingsFailClearly() {
        assertTrue(assertThrows(IllegalArgumentException::class.java) { ConfigService("", "merchant") }.message!!.contains("backend"))
        assertTrue(assertThrows(IllegalArgumentException::class.java) { ConfigService("https://example.com/api", "") }.message!!.contains("merchant"))
        listOf("file:///tmp", "https://user:pass@example.com", "https://example.com?key=value", "https://example.com#fragment").forEach {
            assertThrows(IllegalArgumentException::class.java) { ConfigService(it, "merchant") }
        }
        assertEquals("https://example.com/v1/", ConfigService(" https://example.com/v1 ", "merchant").backendUrl)
    }
}
