package com.payone.pcpclientandroiddemo.gpay

import android.content.Context
import com.payone.pcpclientandroiddemo.BuildConfig
import com.google.android.gms.wallet.WalletConstants
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressConfig
import java.net.URI

/** Public demo settings. Credentials belong to the merchant server, never to this app. */
class ConfigService(backendUrl: String, val gatewayMerchantId: String) {
    val backendUrl = normalize(backendUrl)
    val merchantName = "PAYONE Testshop"
    val walletEnvironment = WalletConstants.ENVIRONMENT_TEST
    val currencyCode = "EUR"
    val countryCode = "DE"

    constructor(context: Context) : this(
        context.getSharedPreferences("demo-backend", Context.MODE_PRIVATE).getString("url", BuildConfig.BACKEND_BASE_URL).orEmpty(),
        context.getSharedPreferences("demo-backend", Context.MODE_PRIVATE).getString("merchantId", BuildConfig.GATEWAY_MERCHANT_ID).orEmpty(),
    )

    init {
        require(gatewayMerchantId.isNotBlank()) { "Set the PAYONE merchant ID in Checkout settings." }
    }

    fun walletConfig() = GooglePayExpressConfig(
        gatewayMerchantId, merchantName, currencyCode, countryCode,
        shippingCountries = java.util.Locale.getISOCountries().toSet(), cardNetworks = setOf("VISA", "MASTERCARD"),
        authMethods = setOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
    )

    companion object {
        fun normalize(value: String): String {
            require(value.isNotBlank()) { "Set the backend API URL in Checkout settings." }
            val uri = try { URI(value.trim()).parseServerAuthority() }
            catch (_: Exception) { throw IllegalArgumentException("Invalid backend API URL") }
            val scheme = uri.scheme?.lowercase()
            require(scheme in setOf("http", "https") && !uri.host.isNullOrBlank()) { "Invalid backend API URL" }
            require(uri.port == -1 || uri.port in 1..65535) { "Invalid backend port" }
            require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "Invalid backend API URL" }
            return "$scheme://${uri.rawAuthority}${uri.rawPath.orEmpty().trimEnd('/')}/"
        }
    }
}
