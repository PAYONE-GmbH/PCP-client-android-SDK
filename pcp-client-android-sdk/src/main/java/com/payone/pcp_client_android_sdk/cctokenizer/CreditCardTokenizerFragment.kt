package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.payone.pcp_client_android_sdk.R
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class CreditcardTokenizerFragment : Fragment() {
    private lateinit var webView: WebView
    private lateinit var config: CreditcardTokenizerConfig
    private lateinit var jwtToken: String
    private lateinit var tokenizerHtmlUrl: String

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val ARG_CONFIG = "config"
        private const val ARG_JWT_TOKEN = "jwtToken"
        private const val ARG_TOKENIZER_HTML_URL = "tokenizerHtmlUrl"
        fun newInstance(
            config: CreditcardTokenizerConfig,
            jwtToken: String,
            tokenizerHtmlUrl: String
        ): CreditcardTokenizerFragment {
            return CreditcardTokenizerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CONFIG, config)
                    putString(ARG_JWT_TOKEN, jwtToken)
                    putString(ARG_TOKENIZER_HTML_URL, tokenizerHtmlUrl)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_layout_cctokenizer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webView)
        arguments?.let {
            tokenizerHtmlUrl = it.getString(ARG_TOKENIZER_HTML_URL) ?: ""
            jwtToken = it.getString(ARG_JWT_TOKEN) ?: ""
            config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_CONFIG, CreditcardTokenizerConfig::class.java)
                    ?: throw IllegalArgumentException("Config cannot be null")
            } else {
                it.getSerializable(ARG_CONFIG) as? CreditcardTokenizerConfig
                    ?: throw IllegalArgumentException("Config cannot be null")
            }
        }
        setupWebView()
    }

    private class WebAppInterface(
        private val handler: Handler,
        private val onScriptLoadedCallbackFunc: () -> Unit,
        private val config: CreditcardTokenizerConfig
    ) {
        @JavascriptInterface
        fun onScriptLoaded() {
            handler.post {
                onScriptLoadedCallbackFunc()
            }
        }

        @JavascriptInterface
        fun onScriptError() {
            handler.post {
                config.tokenizationFailureCallback?.invoke(500, mapOf("error" to "LoadingScriptFailed"))
            }
        }

        @JavascriptInterface
        fun onTokenizationSuccess(statusCode: Int, token: String, cardDetailsJson: String) {
            handler.post {
                val cardDetails: Map<String, Any?> = Gson().fromJson(cardDetailsJson, Map::class.java) as Map<String, Any?>
                config.tokenizationSuccessCallback?.invoke(statusCode, token, cardDetails)
            }
        }

        @JavascriptInterface
        fun onTokenizationFailure(statusCode: Int, errorResponseJson: String) {
            handler.post {
                val errorResponse: Map<String, Any?> = Gson().fromJson(errorResponseJson, Map::class.java) as Map<String, Any?>
                config.tokenizationFailureCallback?.invoke(statusCode, errorResponse)
            }
        }
    }

    private fun setupWebView() {
        android.webkit.WebView.setWebContentsDebuggingEnabled(true)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.addJavascriptInterface(WebAppInterface(handler, ::makeScriptToPopulateHTML, config), "AndroidInterface")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                makeScriptToLoadPayoneHostedScript()
            }
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }
        }
        webView.loadUrl(tokenizerHtmlUrl)
    }

    private fun makeScriptToLoadPayoneHostedScript() {
        val sdkScriptEnv = mapOf(
            "test" to mapOf(
                "src" to "https://sdk.preprod.tokenization.secure.payone.com/1.0.1/hosted-tokenization-sdk.js",
                "integrity" to "sha384-Ec6OPQvn8poHUzTwcUYWC/pwd5wgVuVB+jKl+Eml5MWou154pm6j2MdhhJb9uqML"
            ),
            "live" to mapOf(
                "src" to "https://sdk.tokenization.secure.payone.com/1.0.1/hosted-tokenization-sdk",
                "integrity" to "sha384-Ec6OPQvn8poHUzTwcUYWC/pwd5wgVuVB+jKl+Eml5MWou154pm6j2MdhhJb9uqML"
            )
        )
        val env = config.environment ?: "test"
        val scriptInfo = sdkScriptEnv[env] ?: sdkScriptEnv["test"]!!
        val scriptSrc = scriptInfo["src"]
        val scriptIntegrity = scriptInfo["integrity"]
        webView.evaluateJavascript(
            """
        (function() {
            if (!document.getElementById('hosted-tokenization-sdk')) {
                const script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = '$scriptSrc';
                script.id = 'hosted-tokenization-sdk';
                script.setAttribute('integrity', '$scriptIntegrity');
                script.setAttribute('crossorigin', 'anonymous');
                script.onload = function() {
                    window.AndroidInterface.onScriptLoaded();
                }
                script.onerror = function() {
                    console.error('Error loading Hosted Tokenization SDK script');
                    window.AndroidInterface.onScriptError();
                }
                document.head.appendChild(script);
            }
        })();
    """.trimIndent()
        ) { result ->
            Log.d("CCTokenizer", "makeScriptToLoadPayoneHostedScript() executed: $result")
        }
    }

    private fun makeScriptToPopulateHTML() {
        val gson = Gson()
        val uiConfigJson = gson.toJson(config.uiConfig)
        val iframeConfigJson = gson.toJson(config.iframe)
        val locale = config.locale ?: "de_DE"
        val submitButtonSelector = config.submitButton?.selector ?: "#submit"
        webView.evaluateJavascript(
            """
        var sdkConfig = {
            iframe: $iframeConfigJson,
            uiConfig: $uiConfigJson,
            locale: '$locale',
            token: '$jwtToken'
        };
        if (window.HostedTokenizationSdk) {
            window.HostedTokenizationSdk.init().then(function() {
                window.HostedTokenizationSdk.getPaymentPage(sdkConfig);
                var submitBtn = document.querySelector('$submitButtonSelector');
                if (submitBtn) {
                    submitBtn.onclick = function() {
                        window.HostedTokenizationSdk.submitForm(
                            (statusCode, token, cardDetails) => window.AndroidInterface.onTokenizationSuccess(statusCode, token, JSON.stringify(cardDetails)),
                            (statusCode, errorResponse) => window.AndroidInterface.onTokenizationFailure(statusCode, JSON.stringify(errorResponse))
                        );
                    };
                }
            }).catch(function(error) {
                console.error('Error initializing Hosted Tokenization SDK:', error);
                window.AndroidInterface.onScriptError(); 
            });
        }
        """.trimIndent()
        ) { result ->
            Log.d("CCTokenizer", "makeScriptToPopulateHTML() executed: $result")
        }
    }

}


