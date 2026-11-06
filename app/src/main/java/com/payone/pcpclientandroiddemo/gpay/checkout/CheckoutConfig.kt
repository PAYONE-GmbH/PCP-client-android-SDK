package com.payone.pcpclientandroiddemo.gpay.checkout

import com.google.android.gms.wallet.WalletConstants
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressConfig
import java.math.BigDecimal
import java.util.Currency

enum class CheckoutMode { STANDARD, EXPRESS }

data class DeliveryOption(val id: String, val label: String, val amount: Long)

/** Demo parameters contain no credentials, tokens or customer data. Amounts are minor currency units. */
data class CheckoutConfig(
    // API base URL, including any deployment prefix; endpoints are relative to it.
    val backendUrl: String = "http://localhost:3009/api/",
    val merchantName: String = "PAYONE Google Pay Demo",
    val basketAmount: Long = 1000,
    val currencyCode: String = "EUR",
    val countryCode: String = "DE",
    val deliveryOptions: List<DeliveryOption> = listOf(
        DeliveryOption("standard", "Standard (3–5 business days)", 0),
        DeliveryOption("express", "Express (1–2 business days)", 499),
    ),
    val cardNetworks: Set<String> = setOf("VISA", "MASTERCARD"),
    val authMethods: Set<String> = setOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
    val authorizationMode: String = "PRE_AUTHORIZATION",
    val executePaymentOnCreation: Boolean = false,
    val walletEnvironment: Int = WalletConstants.ENVIRONMENT_TEST,
) {
    init {
        require(backendUrl.startsWith("http://") || backendUrl.startsWith("https://"))
        require(basketAmount > 0 && deliveryOptions.isNotEmpty())
        require(deliveryOptions.map { it.id }.distinct().size == deliveryOptions.size)
        require(deliveryOptions.all { it.id.isNotBlank() && it.label.isNotBlank() && it.amount >= 0 })
        deliveryOptions.forEach { Math.addExact(basketAmount, it.amount) }
        require(authorizationMode in setOf("PRE_AUTHORIZATION", "SALE"))
        require(walletEnvironment in setOf(WalletConstants.ENVIRONMENT_TEST, WalletConstants.ENVIRONMENT_PRODUCTION))
        googlePayConfig("validation")
    }

    fun googlePayConfig(merchantId: String) = GooglePayExpressConfig(
        merchantId, merchantName, currencyCode, countryCode, setOf(countryCode), cardNetworks, authMethods,
    )

    fun total(country: String, option: String): Long {
        require(country == countryCode) { "Unsupported delivery country" }
        return Math.addExact(basketAmount, deliveryOptions.single { it.id == option }.amount)
    }

    fun money(amount: Long): String = BigDecimal.valueOf(amount, Currency.getInstance(currencyCode).defaultFractionDigits).toPlainString()
}
