package com.payone.pcp_client_android_sdk.cctokenizer

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 27, 37])
class CreditcardTokenizerHtmlTest {
    private val html = "<html><body><div id='payment-IFrame'></div><button id='submit'>Pay</button></body></html>"
    private val baseURL = "https://merchant.example/checkout/"

    private fun config(onFailure: (Int, Map<String, Any?>) -> Unit = { _, _ -> }) =
        CreditcardTokenizerConfig(
            iframe = IframeConfig("payment-IFrame"), token = "test-token",
            submitButton = SubmitButtonConfig(selector = "#submit"),
            tokenizationSuccessCallback = { _, _, _, _ -> },
            tokenizationFailureCallback = onFailure,
        )

    @Test
    fun `HTML and origin survive parcel restoration and view recreation`() {
        val original = CreditcardTokenizerFragment.newInstanceWithHtml(config(), html, baseURL)
        val parcel = Parcel.obtain()
        val args: Bundle
        try {
            parcel.writeBundle(original.arguments)
            val bytes = parcel.marshall()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            args = parcel.readBundle(CreditcardTokenizerConfig::class.java.classLoader)!!
        } finally {
            parcel.recycle()
        }
        val restored = CreditcardTokenizerFragment().apply { arguments = args }
        restored.setTokenizationCallbacks({ _, _, _, _ -> }, { _, _ -> })
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            val manager = controller.get().supportFragmentManager
            manager.beginTransaction().add(android.R.id.content, restored).commitNow()
            fun assertPage() {
                val webView = restored.requireView().findViewById<WebView>(R.id.webView)
                val loaded = shadowOf(webView).lastLoadDataWithBaseURL
                assertEquals(baseURL, loaded.baseUrl)
                assertEquals(html, loaded.data)
                assertEquals("text/html", loaded.mimeType)
                assertEquals("UTF-8", loaded.encoding)
                assertNotNull(shadowOf(webView).getJavascriptInterface("AndroidInterface"))
            }
            assertPage()
            manager.beginTransaction().detach(restored).commitNow()
            manager.beginTransaction().attach(restored).commitNow()
            assertPage()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `missing asset reports loading failure without navigating`() {
        var error: Map<String, Any?>? = null
        val fragment = CreditcardTokenizerFragment.newInstanceWithAsset(
            config { status, details -> assertEquals(500, status); error = details },
            "missing.html", baseURL,
        )
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        try {
            controller.get().supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment).commitNow()
            assertEquals("LocalHTMLLoadingFailed", error?.get("error"))
            val webView = fragment.requireView().findViewById<WebView>(R.id.webView)
            assertNull(shadowOf(webView).lastLoadDataWithBaseURL)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `local sources reject invalid HTTPS origins`() {
        listOf("", "http://merchant.example/", "file:///page.html", "data:text/html,page",
            "https:///page", "https://user:password@merchant.example/", "https://merchant.example:65536/")
            .forEach { invalid ->
                assertThrows(IllegalArgumentException::class.java) {
                    CreditcardTokenizerFragment.newInstanceWithHtml(config(), html, invalid)
                }
                assertThrows(IllegalArgumentException::class.java) {
                    CreditcardTokenizerFragment.newInstanceWithAsset(config(), "page.html", invalid)
                }
            }
    }
}
