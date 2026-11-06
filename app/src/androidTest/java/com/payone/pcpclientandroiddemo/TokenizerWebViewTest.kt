package com.payone.pcpclientandroiddemo

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.IframeConfig
import com.payone.pcp_client_android_sdk.cctokenizer.SubmitButtonConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Local platform/bridge test: no PAYONE credentials or remote authorization. */
@RunWith(AndroidJUnit4::class)
class TokenizerWebViewTest {
    @Test
    fun javascriptCallbackSurvivesViewRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val received = AtomicReference<String>()
            val completed = AtomicReference(CountDownLatch(1))
            val fragment = CreditcardTokenizerFragment.newInstance(
                CreditcardTokenizerConfig(
                    iframe = IframeConfig("payment-IFrame"), token = "local-test",
                    uiConfig = com.payone.pcp_client_android_sdk.cctokenizer.UIConfig(fieldTextColor = "red"),
                    submitButton = SubmitButtonConfig(selector = "#submit"),
                    tokenizationSuccessCallback = { _, token, details, _ ->
                        received.set("$token:${details.cardType}:${details.cardNumber}")
                        completed.get().countDown()
                    },
                    tokenizationFailureCallback = { _, _ -> completed.get().countDown() },
                ), "about:blank",
            )
            fun loadFixture() {
                val webView = fragment.requireView().findViewById<WebView>(
                    com.payone.pcp_client_android_sdk.R.id.webView,
                )
                webView.stopLoading()
                webView.webViewClient = WebViewClient()
                webView.loadDataWithBaseURL("https://example.com/", """
                    <html><body><script>
                    AndroidInterface.onTokenizationSuccess(200, 'local-token',
                      JSON.stringify({cardholderName:'Test', cardNumber:'1234',
                        expiryDate:'12/30', cardType:'visa'}), 'manual');
                    </script></body></html>
                """.trimIndent(), "text/html", "UTF-8", null)
            }
            scenario.onActivity {
                it.supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment).commitNow()
                loadFixture()
            }
            assertTrue("JavaScript callback timed out", completed.get().await(20, TimeUnit.SECONDS))
            assertEquals("local-token:visa:1234", received.get())
            received.set(null)
            completed.set(CountDownLatch(1))
            scenario.onActivity {
                it.supportFragmentManager.beginTransaction().detach(fragment).commitNow()
                it.supportFragmentManager.beginTransaction().attach(fragment).commitNow()
                loadFixture()
            }
            assertTrue("Callback after recreation timed out", completed.get().await(20, TimeUnit.SECONDS))
            assertEquals("local-token:visa:1234", received.get())
        }
    }
}
