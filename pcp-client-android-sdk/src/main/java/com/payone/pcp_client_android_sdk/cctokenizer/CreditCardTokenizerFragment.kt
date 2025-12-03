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
    private lateinit var tokenizerHtmlUrl: String

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val ARG_CONFIG = "config"
        private const val ARG_TOKENIZER_HTML_URL = "tokenizerHtmlUrl"
        fun newInstance(
            config: CreditcardTokenizerConfig,
            tokenizerHtmlUrl: String
        ): CreditcardTokenizerFragment {
            return CreditcardTokenizerFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CONFIG, config)
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
        fun onTokenizationSuccess(statusCode: Int, token: String, cardDetailsJson: String, inputMode: String) {
            handler.post {
                val cardDetails: CardDetails = Gson().fromJson(cardDetailsJson, CardDetails::class.java)
                config.tokenizationSuccessCallback?.invoke(statusCode, token, cardDetails, inputMode)
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
       // log config
        Log.d("CCTokenizer", "Using config: $config")
        // log token
        Log.d("CCTokenizer", "Using token: ${config.token}")
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
                "src" to "https://sdk.preprod.tokenization.secure.payone.com/1.3.0/hosted-tokenization-sdk.js",
                "integrity" to "sha384-2mqrh4mWkGZN9XmQeJFzKX5t+i9at3NYnUT9qvS2GiMRe8a6pigcsaxGh5y7KwbG"
            ),
            "live" to mapOf(
                "src" to "https://sdk.tokenization.secure.payone.com/1.3.0/hosted-tokenization-sdk.js",
                "integrity" to "sha384-2mqrh4mWkGZN9XmQeJFzKX5t+i9at3NYnUT9qvS2GiMRe8a6pigcsaxGh5y7KwbG"
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
        // Build iframe config with defaults
        val iframeConfig = mapOf(
            "iframeWrapperId" to config.iframe.iframeWrapperId,
            "height" to (config.iframe.height ?: "auto"),
            "width" to (config.iframe.width ?: 400),
            "zIndex" to (config.iframe.zIndex ?: 9999)
        )        
        val uiConfigJson = gson.toJson(config.uiConfig ?: emptyMap<String, Any>())
        val iframeConfigJson = gson.toJson(iframeConfig)
        val customTextConfigJson = gson.toJson(config.customTextConfig)
        val allowedCardSchemesJson = gson.toJson(config.allowedCardSchemes)
        val locale = config.locale ?: "de_DE"
        val token = config.token
        val mode = config.mode ?: "live"
        val submitButtonSelector = config.submitButton.selector ?: "#submit"
    
        webView.evaluateJavascript(
            """
        var sdkConfig = {
            iframe: $iframeConfigJson,
            uiConfig: $uiConfigJson,
            locale: '$locale',
            token: '$token',
            mode: '$mode',
            allowedCardSchemes: $allowedCardSchemesJson,
            customTextConfig: $customTextConfigJson
        };
        if (window.HostedTokenizationSdk) {
            window.HostedTokenizationSdk.init().then(function() {
                window.HostedTokenizationSdk.getPaymentPage(sdkConfig);
                var submitBtn = document.querySelector('$submitButtonSelector');
                if (submitBtn) {
                    submitBtn.onclick = function() {
                        window.HostedTokenizationSdk.submitForm(
                            (statusCode, token, cardDetails, inputMode) => window.AndroidInterface.onTokenizationSuccess(statusCode, token, JSON.stringify(cardDetails), inputMode),
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


