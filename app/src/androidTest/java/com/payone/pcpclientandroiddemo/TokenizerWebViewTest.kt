package com.payone.pcpclientandroiddemo

import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Parcel
import androidx.lifecycle.Lifecycle
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
            var fragment = CreditcardTokenizerFragment.newInstance(
                CreditcardTokenizerConfig(
                    iframe = IframeConfig("payment-IFrame"), token = "local-test",
                    uiConfig = com.payone.pcp_client_android_sdk.cctokenizer.UIConfig(fieldTextColor = "red"),
                    submitButton = SubmitButtonConfig(selector = "#submit"),
                    tokenizationSuccessCallback = { _, token, details, _ ->
                        received.set("$token:${details.cardType}:${details.cardNumber}")
                        completed.get().countDown()
                    },
                    tokenizationFailureCallback = { _, _ -> },
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
                    .add(android.R.id.content, fragment, "tokenizer").commitNow()
                // Force the same serialization that Activity stopping triggers on API 26.
                val parcel = Parcel.obtain()
                try {
                    parcel.writeBundle(fragment.arguments)
                    val bytes = parcel.marshall()
                    assertTrue(bytes.isNotEmpty())
                    parcel.unmarshall(bytes, 0, bytes.size)
                    parcel.setDataPosition(0)
                    val saved = parcel.readBundle(CreditcardTokenizerConfig::class.java.classLoader)!!
                    // Use the platform reader so this test does not require keeping
                    // an AndroidX helper that R8 can inline out of the host APK.
                    @Suppress("DEPRECATION")
                    val restored = saved.getSerializable("config") as CreditcardTokenizerConfig
                    assertEquals("local-test", restored.token)
                    assertThrows(IllegalStateException::class.java) {
                        restored.tokenizationFailureCallback(500, emptyMap())
                    }
                } finally {
                    parcel.recycle()
                }
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
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            scenario.onActivity {
                received.set(null)
                completed.set(CountDownLatch(1))
                fragment = it.supportFragmentManager.findFragmentByTag("tokenizer") as CreditcardTokenizerFragment
                loadFixture()
            }
            assertTrue("Callback after Activity recreation timed out", completed.get().await(20, TimeUnit.SECONDS))
            assertEquals("local-token:visa:1234", received.get())
        }
    }
}
