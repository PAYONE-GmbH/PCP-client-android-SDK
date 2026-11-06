package com.payone.pcp_client_android_sdk.cctokenizer

import android.os.Looper
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37])
class CreditcardTokenizerLifecycleTest {
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
