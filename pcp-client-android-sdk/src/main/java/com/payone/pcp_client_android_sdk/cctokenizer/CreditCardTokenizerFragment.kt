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
    private lateinit var tokenizerUrl: String
    private lateinit var request: CCTokenizerRequest
    private lateinit var supportedCardTypes: List<String>
    private lateinit var config: CreditcardTokenizerConfig

    private val handler = Handler(Looper.getMainLooper())

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
            tokenizerUrl = it.getString(ARG_TOKENIZER_URL) ?: ""
            request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_REQUEST, CCTokenizerRequest::class.java)
                    ?: throw IllegalArgumentException("Request cannot be null")
            } else {
                it.getSerializable(ARG_REQUEST) as? CCTokenizerRequest
                    ?: throw IllegalArgumentException("Request cannot be null")
            }
            supportedCardTypes =
                it.getStringArrayList(ARG_SUPPORTED_CARD_TYPES)?.toList() ?: emptyList()
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

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.addJavascriptInterface(WebAppInterface(), "AndroidInterface")

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

        webView.loadUrl(tokenizerUrl)
    }

    private inner class WebAppInterface {
        @JavascriptInterface
        fun onScriptLoaded() {
            handler.post {
                makeScriptToPopulateHTML()
            }
        }

        @JavascriptInterface
        fun onScriptError() {
            handler.post {
                config.creditCardCheckCallback(Result.failure(CCTokenizerError.LoadingScriptFailed))
            }
        }

        @JavascriptInterface
        fun onPayCallback(response: String) {
            handler.post {
                try {
                    val response = Json.decodeFromString<CCTokenizerResponse>(response)
                    config.creditCardCheckCallback(Result.success(response))
                } catch (e: Exception) {
                    config.creditCardCheckCallback(Result.failure(CCTokenizerError.InvalidResponse))
                }
            }
        }
    }

    private fun makeScriptToLoadPayoneHostedScript() {
        webView.evaluateJavascript(
            """
        (function() {
            if (!document.getElementById('payone-hosted-script')) {
                const script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = 'https://secure.prelive.pay1-test.de/client-api/js/v1/payone_hosted_min.js';
                script.id = 'payone-hosted-script';
                script.onload = function() {
                    window.AndroidInterface.onScriptLoaded();
                }
                script.onerror = function() {
                    window.AndroidInterface.onScriptError();
                }
                document.head.appendChild(script);
            }
        })();
    """.trimIndent()
        ) { result ->
            // Handle any result from the JavaScript evaluation
            Log.d("CCTokenizer", "makeScriptToLoadPayoneHostedScript() executed: $result")
        }
    }

    private fun makeScriptToPopulateHTML() {
        val gson = Gson()
        // Generate and return the script to populate HTML
        webView.evaluateJavascript(
            """
        var supportedCardtypes = ${gson.toJson(supportedCardTypes)};
        var config = {
            fields: {
                cardpan: ${gson.toJson(config.cardPan)},
                cardcvc2: ${gson.toJson(config.cardCvc2)},
                cardexpiremonth: ${gson.toJson(config.cardExpireMonth)},
                cardexpireyear: ${gson.toJson(config.cardExpireYear)}
            },
            defaultStyle: {
                ${generateDefaultStyleKeyValuePairs()}
            },
            autoCardtypeDetection: {
                supportedCardtypes: supportedCardtypes,
                callback: function(detectedCardtype) {
                    document.getElementById('autodetectionResponsePre').innerHTML = detectedCardtype;
                    
                    if (detectedCardtype === 'V') {
                        document.getElementById('visa').style.borderColor = '#00F';
                        document.getElementById('mastercard').style.borderColor = '#FFF';
                    } else if (detectedCardtype === 'M') {
                        document.getElementById('visa').style.borderColor = '#FFF';
                        document.getElementById('mastercard').style.borderColor = '#00F';
                    } else {
                        document.getElementById('visa').style.borderColor = '#FFF';
                        document.getElementById('mastercard').style.borderColor = '#FFF';
                    }
                }
            },
            language: ${config.language.configValue},
            error: "${config.error}"
        };
        var request = {
            request: 'creditcardcheck',
            responsetype: 'JSON',
            mode: '${request.environment.ccTokenizerIdentifier}',
            mid: '${request.mid}',
            aid: '${request.aid}',
            portalid: '${request.portalId}',
            encoding: 'UTF-8',
            storecarddata: 'yes',
            hash: '${request.generatedHash}'
        };
        
        var iframes = new window.Payone.ClientApi.HostedIFrames(config, request);
        window.payoneIFrames = iframes;
        
        function payCallback(response) {
            // Display the response in the jsonResponsePre div
            document.getElementById('jsonResponsePre').textContent = JSON.stringify(response, null, 2);
                    
            // Send the JSON response back to Android
            if (window.AndroidInterface && typeof window.AndroidInterface.onPayCallback === 'function') {
                window.AndroidInterface.onPayCallback(JSON.stringify(response));
            } else {
                console.error('AndroidInterface is not defined or onPayCallback is not a function.');
            }
        }
        
        document.getElementById('${config.submitButtonId}').onclick = function() {
            if (typeof window.payoneIFrames !== 'undefined') {
                // Call creditCardCheck and pass the 'payCallback' function as the callback
                window.payoneIFrames.creditCardCheck('payCallback');
            } else {
                console.error('payoneIFrames is not initialized.');
                alert('payoneIFrames is not initialized.');
            }
        };
        ;
        """.trimIndent()
        ) { result ->
            Log.d("CCTokenizer", "makeScriptToPopulateHTML() executed: $result")
        }
    }

    private fun makeScriptToInitiateAndHandleCheck() {
        Log.d("CCTokenizer", "makeScriptToInitiateAndHandleCheck() executed")
    }

    private fun generateDefaultStyleKeyValuePairs(): String {
        return config.defaultStyles.entries.joinToString(separator = ",\n") {
            val key = it.key
            val value = it.value
            "\"$key\": \"$value\""
        }
    }

    companion object {
        private const val ARG_TOKENIZER_URL = "tokenizerUrl"
        private const val ARG_REQUEST = "request"
        private const val ARG_SUPPORTED_CARD_TYPES = "supportedCardTypes"
        private const val ARG_CONFIG = "config"

        fun newInstance(
            tokenizerUrl: String,
            request: CCTokenizerRequest,
            supportedCardTypes: List<String>,
            config: CreditcardTokenizerConfig
        ): CreditcardTokenizerFragment {
            return CreditcardTokenizerFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOKENIZER_URL, tokenizerUrl)
                    putSerializable(ARG_REQUEST, request)
                    putStringArrayList(ARG_SUPPORTED_CARD_TYPES, ArrayList(supportedCardTypes))
                    putSerializable(ARG_CONFIG, config)
                }
            }
        }
    }

}


