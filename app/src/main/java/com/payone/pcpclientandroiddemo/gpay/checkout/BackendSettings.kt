package com.payone.pcpclientandroiddemo.gpay.checkout

import android.content.Context
import com.payone.pcpclientandroiddemo.R
import java.net.URI

/** Demo-only settings. A running checkout keeps its own CheckoutSession configuration. */
class BackendSettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("demo-backend", Context.MODE_PRIVATE)

    fun configuration(): CheckoutConfig = CheckoutConfig(
        backendUrl = prefs.getString("url", null) ?: CheckoutConfig().backendUrl,
    )

    fun save(url: String) {
        val normalized = normalize(url)
        check(prefs.edit().putString("url", normalized).commit())
    }

    fun reset() {
        check(prefs.edit().remove("url").commit())
    }

    fun connectionError(url: String, error: Exception): String {
        val host = URI(url).host
        val hint = when (host) {
            "localhost", "127.0.0.1", "[::1]", "::1" -> {
                val uri = URI(url)
                val port = if (uri.port != -1) uri.port else if (uri.scheme == "https") 443 else 80
                context.getString(R.string.backend_loopback_hint, port)
            }
            "10.0.2.2" -> context.getString(R.string.backend_emulator_hint)
            else -> context.getString(R.string.backend_network_hint)
        }
        val detail = if (error is CheckoutBackendException) error.message.orEmpty()
            else context.getString(R.string.backend_connection_failed)
        return context.getString(R.string.backend_unavailable, url, detail, hint)
    }

    companion object {
        fun normalize(value: String): String {
            val uri = try { URI(value.trim()).parseServerAuthority() }
                catch (error: Exception) { throw IllegalArgumentException("Invalid backend URL", error) }
            val scheme = uri.scheme?.lowercase()
            require(scheme in setOf("http", "https") && !uri.host.isNullOrBlank())
            require(uri.port == -1 || uri.port in 1..65535)
            require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            return "$scheme://${uri.rawAuthority}${uri.rawPath.orEmpty().trimEnd('/')}/"
        }
    }
}
