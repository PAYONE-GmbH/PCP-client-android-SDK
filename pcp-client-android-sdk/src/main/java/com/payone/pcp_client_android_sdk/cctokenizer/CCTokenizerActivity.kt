package com.payone.pcp_client_android_sdk.cctokenizer

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.payone.pcp_client_android_sdk.R

class CCTokenizerActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cctokenizer)

        WebView.setWebContentsDebuggingEnabled(true)

        webView = findViewById(R.id.webview)


        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.d("webviewlogs", consoleMessage.toString())
                return super.onConsoleMessage(consoleMessage)
            }
        }
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.addJavascriptInterface(WebAppInterface(this), "AndroidInterface")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript("""
        (function() {
            if (!document.getElementById('payone-hosted-script')) {
                const script = document.createElement('script');
                script.type = 'text/javascript';
                script.src = 'https://secure.prelive.pay1-test.de/client-api/js/v1/payone_hosted_min.js';
                script.id = 'payone-hosted-script';
                script.onload = function() {
                    window.AndroidInterface.onScriptLoaded('Script loaded.');
                }
                script.onerror = function() {
                    window.AndroidInterface.onScriptError('Failed to load Payone script.');
                }
                document.head.appendChild(script);
            }
        })();
    """.trimIndent()) { result ->
                    // Handle any result from the JavaScript evaluation
                    Log.d("WebView", "JavaScript executed: $result")
                }

                webView.evaluateJavascript(injectButtonClickScript()){
                    Log.d("WebView", "JavaScript inject button executed: $it")
                }
            }
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.loadUrl("https://djirlic.com/paytestapp.html")


        // Inject the JavaScript after the page loads

    }



    private fun makeSecondScript(request: Request): String {
        return """
            (function() {
        var supportedCardtypes = ["V", "M"];
        var config = {
            fields: {
                cardpan: {
                    selector: "cardpan",
                    style: "font-size: 14px; border: 1px solid #000;",
                    type: "input"
                },
                cardcvc2: {
                    selector: "cardcvc2",
                    type: "password",
                    style: "font-size: 14px; border: 1px solid #000;",
                    size: "4",
                    maxlength: "4",
                    length: { "V": 3, "M": 3 }
                },
                cardexpiremonth: {
                    selector: "cardexpiremonth",
                    type: "text",
                    size: "2",
                    maxlength: "2",
                    iframe: {
                        width: "40px"
                    },
                    style: "font-size: 14px; width: 30px; border: solid 1px #000; height: 22px;"
                },
                cardexpireyear: {
                    selector: "cardexpireyear",
                    type: "text",
                    iframe: {
                        width: "60px"
                    },
                    style: "font-size: 14px; width: 50px; border: solid 1px #000; height: 22px;"
                }
            },
            defaultStyle: {
                input: "font-size: 1em; border: 1px solid #000; width: 175px;",
                select: "font-size: 1em; border: 1px solid #000;",
                iframe: {
                    height: "22px",
                    width: "180px"
                }
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
            language: Payone.ClientApi.Language.de,
            error: "error"
        };
        var request = {
            request: '${request.request}',
            responsetype: '${request.responsetype}',
            mode: '${request.mode}',
            mid: '${request.mid}',
            aid: '${request.aid}',
            portalid: '${request.portalid}',
            encoding: '${request.encoding}',
            storecarddata: '${request.storecarddata}',
            hash: '${request.hash}'
        };
        var iframes = new window.Payone.ClientApi.HostedIFrames(config, request);
        window.payoneIFrames = iframes;
        ;null
        })()
    """.trimIndent()
    }

    private fun injectButtonClickScript(): String {
        return """
        document.getElementById('submit').onclick = function() {
            if (typeof window.payoneIFrames !== 'undefined') {
                // Call creditCardCheck and pass the 'payCallback' function as the callback
                window.payoneIFrames.creditCardCheck('payCallback');
            } else {
                console.error('payoneIFrames is not initialized.');
                alert('payoneIFrames is not initialized.');
            }
        };

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
    """.trimIndent()
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun onScriptLoaded(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            handler.post {

                val request = Request(
                    request = "creditcardcheck",
                    responsetype = "JSON",
                    mode = "test",
                    mid = "",
                    aid = "",
                    portalid = "",
                    encoding = "UTF-8",
                    storecarddata = "yes",
                    api_version = "",
                    hash = ""
                )

                val secondScript = makeSecondScript(request)
                webView.evaluateJavascript(secondScript) { result ->
                    Log.d("WebAppInterface", "Second script executed: $result")
                }
            }
        }

        @JavascriptInterface
        fun onScriptError(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun onPayCallback(response: String) {
            // Handle the JSON response from JavaScript
            Log.d("WebAppInterface", "Received JSON response: $response")
            Toast.makeText(context, "Received response from WebView", Toast.LENGTH_SHORT).show()

            // You can process the JSON string here, or pass it to other parts of your Android app
        }
    }
}