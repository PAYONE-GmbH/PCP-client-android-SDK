package com.payone.pcpclientandroiddemo

import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Parcel
import android.content.Intent
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    fun demoPlacesOutputBelowEntireHtmlAndKeepsNumericInputFocused() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, CCTokenizerActivity::class.java)
            .putExtra("com.payone.pcpclientandroiddemo.MERCHANT_TOKEN", "local-test")
        val ready = CountDownLatch(1)
        ActivityScenario.launch<CCTokenizerActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
                val webView = activity.findViewById<WebView>(com.payone.pcp_client_android_sdk.R.id.webView)
                webView.stopLoading()
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) { ready.countDown() }
                }
                webView.loadDataWithBaseURL("https://merchant.example/", """
                    <html><head><meta name="viewport" content="width=device-width"></head><body>
                    <input id="card" type="number"
                      oninput="AndroidInterface.onStatusChanged('Numeric input changed')">
                    <div style="height:1400px">Hosted form</div>
                    <button id="submit">Run credit card check</button>
                    </body></html>
                """.trimIndent(), "text/html", "UTF-8", null)
            }
            assertTrue(ready.await(20, TimeUnit.SECONDS))
            val checked = CountDownLatch(1)
            scenario.onActivity { activity ->
                val webView = activity.findViewById<WebView>(com.payone.pcp_client_android_sdk.R.id.webView)
                webView.postDelayed({
                    webView.requestFocus()
                    webView.evaluateJavascript("""
                        document.getElementById('card').focus();
                        document.getElementById('card').value = '4';
                        document.getElementById('card').dispatchEvent(new Event('input'));
                        document.getElementById('submit').getBoundingClientRect().bottom + window.scrollY;
                    """.trimIndent()) { buttonBottom ->
                        val frame = activity.findViewById<View>(R.id.fragment_container)
                        @Suppress("DEPRECATION")
                        val requiredHeight = buttonBottom.toDouble() * webView.scale
                        assertTrue("Check button must fit inside the frame", frame.height >= requiredHeight)
                        val output = activity.findViewById<View>(R.id.tokenizerOutputScroll)
                        assertTrue("Output must follow the entire frame", output.top >= frame.bottom)
                        webView.postDelayed({
                            webView.evaluateJavascript("document.activeElement.id") { activeElement ->
                                assertEquals("\"card\"", activeElement)
                                assertTrue(webView.hasFocus())
                                assertTrue(activity.findViewById<TextView>(R.id.tokenizerOutput)
                                    .text.contains("Numeric input changed"))
                                checked.countDown()
                            }
                        }, 300)
                    }
                }, 500)
            }
            assertTrue("Layout/focus check timed out", checked.await(20, TimeUnit.SECONDS))
        }
    }

    @Test
    fun inlineHtmlUsesHttpsOriginAndSubmitBridgeAfterRecreation() {
        val received = AtomicReference<String>()
        val statuses = java.util.Collections.synchronizedList(mutableListOf<String>())
        val completed = AtomicReference(CountDownLatch(1))
        val config = CreditcardTokenizerConfig(
            iframe = IframeConfig("payment-IFrame"), token = "local-test",
            submitButton = SubmitButtonConfig(selector = "#submit"),
            tokenizationSuccessCallback = { _, token, _, _ ->
                received.set(token)
                completed.get().countDown()
            },
            tokenizationFailureCallback = { _, _ -> },
        )
        // Stub the hosted service, but exercise the production page-loading and init/submit path.
        val html = """
            <!DOCTYPE html><html><head><script id="hosted-tokenization-sdk">
            window.HostedTokenizationSdk = {
              init: function() { return Promise.resolve(); },
              getPaymentPage: function(config) {
                document.getElementById(config.iframe.iframeWrapperId).textContent = 'Ready';
                setTimeout(function() { document.getElementById('submit').click(); }, 0);
              },
              submitForm: function(success, failure) {
                success(201, location.origin + '|' + new URL('icon.svg', document.baseURI).href,
                  {cardholderName:'Test', cardNumber:'1234', expiryDate:'12/30', cardType:'visa'}, 'manual');
              }
            };
            </script></head><body>
            <div id="payment-IFrame"></div><button id="submit" type="button">Pay</button>
            <script>AndroidInterface.onScriptLoaded();</script>
            </body></html>
        """.trimIndent()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val fragment = CreditcardTokenizerFragment.newInstanceWithHtml(
                config, html, "https://merchant.example/checkout/",
            )
            fragment.onStatusChanged = { statuses.add(it) }
            scenario.onActivity {
                it.supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment, "inline-html").commitNow()
            }
            assertTrue("Inline HTML submit timed out", completed.get().await(20, TimeUnit.SECONDS))
            assertEquals("https://merchant.example|https://merchant.example/checkout/icon.svg", received.get())
            assertTrue(statuses.any { it.startsWith("Submitting card form") })
            completed.set(CountDownLatch(1))
            scenario.recreate()
            assertTrue("Inline HTML after recreation timed out", completed.get().await(20, TimeUnit.SECONDS))
            assertEquals("https://merchant.example|https://merchant.example/checkout/icon.svg", received.get())
        }
    }

    @Test
    fun bundledHtmlLoadsFromAppAssetsWithHttpsOrigin() {
        val ready = CountDownLatch(1)
        val page = AtomicReference<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                val fragment = CreditcardTokenizerFragment.newInstanceWithAsset(
                    CreditcardTokenizerConfig(
                        iframe = IframeConfig("payment-IFrame"), token = "local-test",
                        submitButton = SubmitButtonConfig(selector = "#submit"),
                        tokenizationSuccessCallback = { _, _, _, _ -> },
                        tokenizationFailureCallback = { _, _ -> },
                    ), "creditcard-tokenizer-example.html", "https://merchant.example/checkout/",
                )
                it.supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment).commitNow()
                val webView = fragment.requireView().findViewById<WebView>(
                    com.payone.pcp_client_android_sdk.R.id.webView,
                )
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.evaluateJavascript(
                            "location.origin + '|' + document.title + '|' + document.querySelector('#submit').id",
                        ) { result -> page.set(result); ready.countDown() }
                    }
                }
            }
            assertTrue("Asset HTML timed out", ready.await(20, TimeUnit.SECONDS))
            assertEquals("\"https://merchant.example|Credit Card Tokenizer Demo|submit\"", page.get())
        }
    }

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
