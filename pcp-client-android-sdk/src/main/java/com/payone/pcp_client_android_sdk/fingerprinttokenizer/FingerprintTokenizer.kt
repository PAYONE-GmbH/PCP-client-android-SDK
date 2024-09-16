package com.payone.pcp_client_android_sdk.fingerprinttokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import android.content.Context
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
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
        snippetToken = "${paylaPartnerId}_${partnerMerchantId}_$uniqueId"
    }

    fun getSnippetToken(onCompletion: (Result<String>) -> Unit) {
        this.onCompletion = onCompletion
        val script = makeScript()
        this.webView = makeInjectedWebView(script)
        webView?.loadData(makeHTML(), "text/html", "UTF-8")
    }

    private fun makeHTML(): String {
        return """
            <!doctype html>
            <html lang="en">
            <body></body>
            </html>
        """
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
                    window.paylaDcs.init('${environment.fingerprintTokenizerIdentifier}', '$snippetToken');
                } else {
                    throw new Error('paylaDcs is not defined or does not have an init method.');
                }
            };
            document.body.appendChild(script);
        """
    }

    private fun makeInjectedWebView(script: String): WebView {
        val webView = WebView(context)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(script) { result ->
                    try {
                        Log.i("Fingerprint", "Successfully loaded snippet token.")
                        onCompletion?.invoke(Result.success(snippetToken))
                    } catch (e: Exception) {
                        Log.e(
                            "Fingerprint",
                            "Fingerprinting script failed with error: ${e.message}"
                        )
                        onCompletion?.invoke(Result.failure(FingerprintError.ScriptError(Throwable(e.message.toString()))))
                    }
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }

        return webView
    }
}