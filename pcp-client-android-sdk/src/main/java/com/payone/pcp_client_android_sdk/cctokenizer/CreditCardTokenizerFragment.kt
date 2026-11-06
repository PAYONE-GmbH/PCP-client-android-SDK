package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

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
import androidx.fragment.app.viewModels
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import com.google.gson.Gson
import com.payone.pcp_client_android_sdk.R
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.URI

internal class TokenizerState : ViewModel() {
    var config: CreditcardTokenizerConfig? = null
}

class CreditcardTokenizerFragment : Fragment() {
    /** Optional main-thread progress messages. Reattach after Activity/process recreation. */
    var onStatusChanged: ((String) -> Unit)? = null
    private var currentWebView: WebView? = null
    private val webView: WebView get() = checkNotNull(currentWebView)
    private var webAppInterface: WebAppInterface? = null
    private val state: TokenizerState by viewModels()
    private var initialConfig: CreditcardTokenizerConfig? = null
    private val config: CreditcardTokenizerConfig get() = checkNotNull(state.config)

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val ARG_CONFIG = "config"
        private const val ARG_TOKENIZER_HTML_URL = "tokenizerHtmlUrl"
        private const val ARG_HTML = "tokenizerHtml"
        private const val ARG_ASSET = "tokenizerAsset"
        private const val ARG_BASE_URL = "localHTMLBaseURL"

        /** Render app-provided HTML with an HTTPS origin; the base URL is not fetched. */
        fun newInstanceWithHtml(
            config: CreditcardTokenizerConfig,
            html: String,
            localHTMLBaseURL: String,
        ): CreditcardTokenizerFragment {
            require(html.isNotBlank()) { "HTML cannot be blank" }
            return localInstance(config, localHTMLBaseURL).apply {
                requireArguments().putString(ARG_HTML, html)
            }
        }

        /** Load a UTF-8 page from the host app's assets using an HTTPS origin. */
        fun newInstanceWithAsset(
            config: CreditcardTokenizerConfig,
            assetPath: String,
            localHTMLBaseURL: String,
        ): CreditcardTokenizerFragment {
            require(assetPath.isNotBlank()) { "Asset path cannot be blank" }
            return localInstance(config, localHTMLBaseURL).apply {
                requireArguments().putString(ARG_ASSET, assetPath)
            }
        }

        private fun localInstance(
            config: CreditcardTokenizerConfig,
            baseURL: String,
        ): CreditcardTokenizerFragment {
            val uri = runCatching { URI(baseURL) }.getOrNull()
            require(uri != null && uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                (uri.port == -1 || uri.port in 1..65535)) {
                "localHTMLBaseURL must be an HTTPS URL with a host and no credentials"
            }
            return newInstance(config, "").apply {
                requireArguments().putString(ARG_BASE_URL, baseURL)
            }
        }

        fun newInstance(
            config: CreditcardTokenizerConfig,
            tokenizerHtmlUrl: String
        ): CreditcardTokenizerFragment {
            return CreditcardTokenizerFragment().apply {
                initialConfig = config
                arguments = Bundle().apply {
                    putSerializable(ARG_CONFIG, config)
                    putString(ARG_TOKENIZER_HTML_URL, tokenizerHtmlUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state.config = initialConfig ?: state.config ?: arguments?.let {
            BundleCompat.getSerializable(it, ARG_CONFIG, CreditcardTokenizerConfig::class.java)
        } ?: throw IllegalArgumentException("Config cannot be null")
        initialConfig = null
    }

    /** Rebind callbacks to the current host, including after process-state restoration. */
    fun setTokenizationCallbacks(
        onSuccess: (Int, String, CardDetails, String) -> Unit,
        onFailure: (Int, Map<String, Any?>) -> Unit,
    ) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            state.config = config.copy(
                tokenizationSuccessCallback = onSuccess, tokenizationFailureCallback = onFailure,
            )
        } else {
            val settings = initialConfig ?: arguments?.let {
                BundleCompat.getSerializable(it, ARG_CONFIG, CreditcardTokenizerConfig::class.java)
            }
            initialConfig = checkNotNull(settings).copy(
                tokenizationSuccessCallback = onSuccess, tokenizationFailureCallback = onFailure,
            )
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
        currentWebView = view.findViewById(R.id.webView)
        setupWebView()
    }

    private class WebAppInterface(
        private val handler: Handler,
        private val onScriptLoadedCallbackFunc: () -> Unit,
        private val onStatus: (String) -> Unit,
        private val currentConfig: () -> CreditcardTokenizerConfig
    ) {
        @Volatile private var active = true

        fun deactivate() {
            active = false
        }

        private fun dispatch(callback: () -> Unit) {
            if (active) handler.post { if (active) callback() }
        }

        @JavascriptInterface
        fun onScriptLoaded() {
            dispatch {
                onStatus("Hosted script loaded. Initializing tokenizer…")
                onScriptLoadedCallbackFunc()
            }
        }

        @JavascriptInterface
        fun onStatusChanged(message: String) {
            dispatch { onStatus(message) }
        }

        @JavascriptInterface
        fun onSdkError(error: String) {
            dispatch { currentConfig().tokenizationFailureCallback(500, mapOf("error" to error)) }
        }

        @JavascriptInterface
        fun onScriptError() {
            dispatch {
                currentConfig().tokenizationFailureCallback(500, mapOf("error" to "LoadingScriptFailed"))
            }
        }

        @JavascriptInterface
        fun onTokenizationSuccess(statusCode: Int, token: String, cardDetailsJson: String, inputMode: String) {
            dispatch {
                val cardDetails: CardDetails = Gson().fromJson(cardDetailsJson, CardDetails::class.java)
                currentConfig().tokenizationSuccessCallback(statusCode, token, cardDetails, inputMode)
            }
        }

        @JavascriptInterface
        fun onTokenizationFailure(statusCode: Int, errorResponseJson: String) {
            dispatch {
                val errorResponse: Map<String, Any?> = Gson().fromJson(errorResponseJson, Map::class.java) as Map<String, Any?>
                currentConfig().tokenizationFailureCallback(statusCode, errorResponse)
            }
        }
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webAppInterface = WebAppInterface(handler, ::makeScriptToPopulateHTML,
            { onStatusChanged?.invoke(it) }) { config }.also {
            webView.addJavascriptInterface(it, "AndroidInterface")
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    config.tokenizationFailureCallback(500, mapOf("error" to "PageLoadingFailed",
                        "description" to error?.description?.toString()))
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onStatusChanged?.invoke("HTML loaded. Loading PAYONE hosted script…")
                makeScriptToLoadPayoneHostedScript()
            }
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }
        }
        loadPage()
    }

    private fun loadPage() {
        val args = requireArguments()
        val baseURL = args.getString(ARG_BASE_URL)
        if (baseURL == null) {
            webView.loadUrl(args.getString(ARG_TOKENIZER_HTML_URL) ?: "")
            return
        }
        val html = try {
            args.getString(ARG_HTML) ?: requireContext().assets
                .open(requireNotNull(args.getString(ARG_ASSET)))
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: java.io.IOException) {
            config.tokenizationFailureCallback(500, mapOf("error" to "LocalHTMLLoadingFailed"))
            return
        }
        webView.loadDataWithBaseURL(baseURL, html, "text/html", "UTF-8", null)
    }

    override fun onDestroyView() {
        webAppInterface?.deactivate()
        webAppInterface = null
        handler.removeCallbacksAndMessages(null)
        currentWebView?.let {
            it.stopLoading()
            it.removeJavascriptInterface("AndroidInterface")
            it.webViewClient = WebViewClient()
            it.webChromeClient = null
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        currentWebView = null
        super.onDestroyView()
    }

    private fun makeScriptToLoadPayoneHostedScript() {
        val sdkScriptEnv = mapOf(
            "test" to mapOf(
                "src" to "https://sdk.preprod.tokenization.secure.payone.com/1.4.0/hosted-tokenization-sdk.js",
                "integrity" to "sha384-gLgHigakYvqqMAmx6FuAl2EaUoWvG24i0xCyDH8YC7+mWpqgFjuzPM0xD3orrMZ4"
            ),
            "live" to mapOf(
                "src" to "https://sdk.tokenization.secure.payone.com/1.4.0/hosted-tokenization-sdk.js",
                "integrity" to "sha384-gLgHigakYvqqMAmx6FuAl2EaUoWvG24i0xCyDH8YC7+mWpqgFjuzPM0xD3orrMZ4"
            )
        )
        val env = config.mode ?: "test"
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
        val customTextConfigJson = gson.toJson(config.customTextConfig ?: emptyMap<String, Any>())
        val allowedCardSchemesJson = gson.toJson(config.allowedCardSchemes ?: emptyList<String>())
        val customIconsConfigJson = gson.toJson(config.customIconsConfig)
        val ctpConfigJson = config.ctpConfig?.let { gson.toJson(it) } ?: "{}"
        val locale = gson.toJson(config.locale ?: "de_DE")
        val token = gson.toJson(config.token)
        val mode = gson.toJson(config.mode ?: "test")
        val submitButtonSelector = gson.toJson(config.submitButton.selector ?: "#submit")

        // Build optional fields snippet: only include non-null optional top-level properties
        val optionalFields = buildString {
            if (config.customIconsConfig != null) {
                append("    customIconsConfig: $customIconsConfigJson,\n")
            }
            append("    CTPConfig: $ctpConfigJson,\n")
        }

        webView.evaluateJavascript(
            """
        var sdkConfig = {
            iframe: $iframeConfigJson,
            uiConfig: $uiConfigJson,
            locale: $locale,
            token: $token,
            mode: $mode,
            allowedCardSchemes: $allowedCardSchemesJson,
            customTextConfig: $customTextConfigJson,
            $optionalFields
        };
        if (window.HostedTokenizationSdk) {
            Promise.resolve().then(function() { return window.HostedTokenizationSdk.init(); }).then(function() {
                window.AndroidInterface.onStatusChanged('Tokenizer initialized. Loading payment form…');
                window.HostedTokenizationSdk.getPaymentPage(sdkConfig, function(message, statusCode) {
                    window.AndroidInterface.onStatusChanged('Payment form response: ' + statusCode + ' — ' + String(message));
                    if (statusCode >= 400 || statusCode === 0) {
                        window.AndroidInterface.onTokenizationFailure(statusCode || 500,
                            JSON.stringify({error: 'PaymentPageLoadingFailed', message: String(message)}));
                    }
                });
                var submitBtn = document.querySelector($submitButtonSelector);
                if (submitBtn) {
                    window.AndroidInterface.onStatusChanged('Submit button connected. Complete the form, then press Run credit card check.');
                    submitBtn.onclick = function(event) {
                        event.preventDefault();
                        window.AndroidInterface.onStatusChanged('Submitting card form… Waiting for tokenization response.');
                        try { window.HostedTokenizationSdk.submitForm(
                            (statusCode, token, cardDetails, inputMode) => window.AndroidInterface.onTokenizationSuccess(statusCode, token, JSON.stringify(cardDetails), inputMode),
                            (statusCode, errorResponse) => window.AndroidInterface.onTokenizationFailure(statusCode, JSON.stringify(errorResponse))
                        ); } catch (error) { window.AndroidInterface.onSdkError('SubmissionFailed'); }
                        return false;
                    };
                } else {
                    window.AndroidInterface.onSdkError('SubmitButtonNotFound');
                }
            }).catch(function(error) {
                console.error('Error initializing Hosted Tokenization SDK:', error);
                window.AndroidInterface.onSdkError('InitializationFailed');
            });
        } else {
            window.AndroidInterface.onSdkError('LoadingScriptFailed');
        }
        """.trimIndent()
        ) { result ->
            Log.d("CCTokenizer", "makeScriptToPopulateHTML() executed: $result")
        }
    }

}
