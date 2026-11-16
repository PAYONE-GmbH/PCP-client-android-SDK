package com.payone.pcp_client_android_sdk.cctokenizer

import android.os.Looper
import android.os.Bundle
import android.os.Parcel
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import com.payone.pcp_client_android_sdk.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37])
class CreditcardTokenizerLifecycleTest {
    @Test
    @Config(sdk = [26, 27, 37])
    fun `saving state excludes captured objects and recreation retains live callbacks`() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            val captured = Any() // Deliberately not Serializable.
            var successes = 0
            val fragment = CreditcardTokenizerFragment.newInstance(
                CreditcardTokenizerConfig(
                    iframe = IframeConfig("payment-IFrame"), token = "test-token",
                    submitButton = SubmitButtonConfig(selector = "#submit"),
                    tokenizationSuccessCallback = { _, _, _, _ ->
                        assertNotNull(captured)
                        successes++
                    },
                    tokenizationFailureCallback = { _, _ -> assertNotNull(captured) },
                ), "https://example.com/tokenizer",
            )
            controller.get().supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment, "tokenizer").commitNow()
            val saved = Bundle()
            controller.pause().saveInstanceState(saved).stop()
            val parcel = Parcel.obtain()
            try {
                parcel.writeBundle(saved)
                assertTrue(parcel.marshall().isNotEmpty())
            } finally {
                parcel.recycle()
            }
            controller.restart().start().resume().visible()
            controller.recreate()
            val restored = controller.get().supportFragmentManager
                .findFragmentByTag("tokenizer") as CreditcardTokenizerFragment
            invokeSuccess(restored)
            assertEquals(1, successes)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    @Config(sdk = [26, 27, 37])
    fun `deserialized settings can rebind callbacks after process restoration`() {
        val captured = Any()
        val original = CreditcardTokenizerConfig(
            iframe = IframeConfig("payment-IFrame"), token = "saved-token",
            submitButton = SubmitButtonConfig(selector = "#submit"),
            tokenizationSuccessCallback = { _, _, _, _ -> captured.hashCode(); Unit },
            tokenizationFailureCallback = { _, _ -> captured.hashCode(); Unit },
        )
        val bytes = ByteArrayOutputStream().also { output ->
            ObjectOutputStream(output).use { it.writeObject(original) }
        }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as CreditcardTokenizerConfig
        }
        assertEquals("saved-token", restored.token)
        assertThrows(IllegalStateException::class.java) {
            restored.tokenizationFailureCallback(500, emptyMap())
        }
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            // Simulate FragmentManager instantiating a Fragment with restored arguments.
            val fragment = CreditcardTokenizerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("config", restored)
                    putString("tokenizerHtmlUrl", "https://example.com/tokenizer")
                }
            }
            controller.get().supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment).commitNow()
            var token: String? = null
            fragment.setTokenizationCallbacks({ _, value, _, _ -> token = value }, { _, _ -> })
            invokeSuccess(fragment)
            assertEquals("token", token)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun invokeSuccess(fragment: CreditcardTokenizerFragment) {
        val webView = fragment.requireView().findViewById<WebView>(R.id.webView)
        val bridge = shadowOf(webView).getJavascriptInterface("AndroidInterface")
        bridge.javaClass.getDeclaredMethod(
            "onTokenizationSuccess", Int::class.javaPrimitiveType,
            String::class.java, String::class.java, String::class.java,
        ).apply { isAccessible = true }.invoke(
            bridge, 200, "token",
            """{"cardholderName":"Test","cardNumber":"1234","expiryDate":"12/30","cardType":"visa"}""",
            "manual",
        )
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `destroying and recreating the view disconnects the old bridge`() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            var successes = 0
            val config = CreditcardTokenizerConfig(
                iframe = IframeConfig("payment-IFrame"), token = "test-token",
                submitButton = SubmitButtonConfig(selector = "#submit"),
                tokenizationSuccessCallback = { _, _, _, _ -> successes++ },
                tokenizationFailureCallback = { _, _ -> },
            )
            val fragment = CreditcardTokenizerFragment.newInstance(config, "https://example.com/tokenizer")
            val manager = controller.get().supportFragmentManager
            manager.beginTransaction().add(android.R.id.content, fragment).commitNow()
            val oldView = fragment.requireView().findViewById<WebView>(R.id.webView)
            val bridge = shadowOf(oldView).getJavascriptInterface("AndroidInterface")
            val success = bridge.javaClass.getDeclaredMethod(
                "onTokenizationSuccess", Int::class.javaPrimitiveType,
                String::class.java, String::class.java, String::class.java,
            ).apply { isAccessible = true }
            val details = """{"cardholderName":"Test","cardNumber":"1234","expiryDate":"12/30","cardType":"visa"}"""
            success.invoke(bridge, 200, "token", details, "manual")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, successes)

            // A result queued before teardown must not reach the destroyed view.
            success.invoke(bridge, 200, "token", details, "manual")
            manager.beginTransaction().detach(fragment).commitNow()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(shadowOf(oldView).wasDestroyCalled())
            assertNull(shadowOf(oldView).getJavascriptInterface("AndroidInterface"))
            success.invoke(bridge, 200, "token", details, "manual")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, successes)

            manager.beginTransaction().attach(fragment).commitNow()
            val newView = fragment.requireView().findViewById<WebView>(R.id.webView)
            assertNotSame(oldView, newView)
            assertNotNull(shadowOf(newView).getJavascriptInterface("AndroidInterface"))
            assertEquals("https://example.com/tokenizer", newView.url)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
