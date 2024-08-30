package com.payone.pcp_client_android_sdk.fingerprint_tokenizer

import android.content.Context
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.UUID

class FingerprintTokenizer(
    private val context: Context,
    private val paylaPartnerId: String,
    private val partnerMerchantId: String,
    private val environment: PCPEnvironment,
    sessionId: String? = null
) {

    private val snippetToken: String
    private var webView: WebView? = null
    private var onCompletion: ((Result<String>) -> Unit)? = null
    init {
        val uniqueId = sessionId ?: UUID.randomUUID().toString()
        snippetToken = "${paylaPartnerId}_${partnerMerchantId}_${uniqueId}"
    }

    fun getSnippetToken(onCompletion: (Result<String>) -> Unit) {
        this.onCompletion = onCompletion
        val script = makeScript()
        this.webView = makeInjectedWebView(script)
        webView?.loadDataWithBaseURL(null, makeHTML(), "text/html", "UTF-8", null)
    }


    private fun makeHTML(): String {
        return """
            <!doctype html>
            <html lang="en">
              <body></body>
            </html>
        """.trimIndent()
    }

    private fun makeScript(): String {
        return """
            window.paylaDcs = window.paylaDcs || {};
            var script = document.createElement('script');
            script.id = 'paylaDcs';
            script.type = 'text/javascript';
            script.src = 'https://d.payla.io/dcs/$paylaPartnerId/$partnerMerchantId/dcs.js';
            script.onload = function() {
                if (typeof window.paylaDcs !== 'undefined' && window.paylaDcs.init) {
                    window.paylaDcs.init('${environment.environment}', '$snippetToken');
                }
                else {
                    throw new Error('paylaDcs is not defined or does not have an init method.');
                }
            };
            document.body.appendChild(script);
        """.trimIndent()
    }

    private fun makeInjectedWebView(script: String): WebView {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(script) { result ->
                    if (result.contains("Error")) {
                        onCompletion?.invoke(Result.failure(FingerprintError.Undefined))
                    } else {
                        onCompletion?.invoke(Result.success(snippetToken))
                    }
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                onCompletion?.invoke(Result.failure(FingerprintError.ScriptError(Throwable(error?.description.toString()))))
            }
        }
        return webView
    }

}

sealed class FingerprintError : Throwable() {
    data class ScriptError(val error: Throwable) : FingerprintError()
    object Undefined : FingerprintError()
}