package com.payone.pcp_client_android_sdk.googlepay

import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

/** Configuration uses the PAYONE merchant ID, not a Google web merchant ID. */
class GooglePayExpressConfig(
    val gatewayMerchantId: String,
    val merchantName: String,
    val currencyCode: String = "EUR",
    val countryCode: String = "DE",
    val shippingCountries: Set<String> = setOf("DE"),
    val cardNetworks: Set<String> = setOf("VISA", "MASTERCARD"),
    val authMethods: Set<String> = setOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
) {
    init {
        require(gatewayMerchantId.isNotBlank() && merchantName.isNotBlank())
        require(currencyCode == currencyCode.uppercase(Locale.ROOT))
        Currency.getInstance(currencyCode)
        val countries = Locale.getISOCountries().toSet()
        require(countryCode in countries && shippingCountries.isNotEmpty() && shippingCountries.all { it in countries })
        require(cardNetworks.isNotEmpty() && cardNetworks.all { it in setOf("VISA", "MASTERCARD", "AMEX", "DISCOVER", "JCB", "INTERAC") })
        require(authMethods.isNotEmpty() && authMethods.all { it in setOf("PAN_ONLY", "CRYPTOGRAM_3DS") })
    }
}

/** Native Express request factory. Pricing and authorization remain merchant/backend responsibilities. */
class GooglePayExpress(private val config: GooglePayExpressConfig) {
    private fun card(tokenize: Boolean): Map<String, Any> = mutableMapOf<String, Any>(
        "type" to "CARD",
        "parameters" to mapOf(
            "allowedAuthMethods" to config.authMethods,
            "allowedCardNetworks" to config.cardNetworks,
            "billingAddressRequired" to true,
            "billingAddressParameters" to mapOf("format" to "FULL", "phoneNumberRequired" to true),
        ),
    ).apply {
        if (tokenize) put("tokenizationSpecification", mapOf(
            "type" to "PAYMENT_GATEWAY",
            "parameters" to mapOf("gateway" to "payonegmbh", "gatewayMerchantId" to config.gatewayMerchantId),
        ))
    }

    fun allowedPaymentMethodsJson(): String = Gson().toJson(listOf(card(true)))

    fun readinessRequest(): IsReadyToPayRequest = IsReadyToPayRequest.fromJson(Gson().toJson(mapOf(
        "apiVersion" to 2, "apiVersionMinor" to 0, "allowedPaymentMethods" to listOf(card(false)),
    )))

    /** Initial estimate; shipping options are supplied by the INITIALIZE callback. */
    fun requestJson(estimatedTotal: String, transactionId: String): String =
        buildRequest(estimatedTotal, transactionId, express = true)

    internal fun buildRequest(total: String, transactionId: String, express: Boolean): String {
        val amount = BigDecimal(total)
        require(amount.signum() > 0 && amount.scale() <= Currency.getInstance(config.currencyCode).defaultFractionDigits)
        require(transactionId.isNotBlank())
        val request = mutableMapOf<String, Any>(
            "apiVersion" to 2, "apiVersionMinor" to 0,
            "merchantInfo" to mapOf("merchantName" to config.merchantName),
            "allowedPaymentMethods" to listOf(card(true)),
            "transactionInfo" to mapOf(
                "totalPriceStatus" to if (express) "ESTIMATED" else "FINAL", "totalPrice" to amount.toPlainString(),
                "currencyCode" to config.currencyCode, "countryCode" to config.countryCode,
                "transactionId" to transactionId, "checkoutOption" to "DEFAULT",
            ),
            "emailRequired" to true, "shippingAddressRequired" to true,
            "shippingAddressParameters" to mapOf("allowedCountryCodes" to config.shippingCountries, "phoneNumberRequired" to true),
        )
        if (express) {
            request["callbackIntents"] = listOf("PAYMENT_AUTHORIZATION", "SHIPPING_ADDRESS", "SHIPPING_OPTION")
            request["shippingOptionRequired"] = true
        }
        return Gson().toJson(request)
    }

    fun paymentDataRequest(estimatedTotal: String, transactionId: String): PaymentDataRequest =
        PaymentDataRequest.fromJson(requestJson(estimatedTotal, transactionId))
}

/** Contains sensitive data. Deliberately has no generated toString()/logging representation. */
class GooglePayExpressPayment private constructor(
    val encryptedPaymentData: String,
    val email: String,
    val shippingAddressJson: String,
    val billingAddressJson: String,
    val shippingOptionId: String,
) {
    /** PAYONE product 320 input. Token contents are forwarded unchanged. */
    fun mobilePaymentMethodSpecificInputJson(authorizationMode: String = "PRE_AUTHORIZATION"): String {
        require(authorizationMode in setOf("SALE", "PRE_AUTHORIZATION"))
        return Gson().toJson(mapOf(
            "paymentProductId" to 320, "authorizationMode" to authorizationMode,
            "encryptedPaymentData" to encryptedPaymentData,
        ))
    }

    companion object {
        fun fromJson(json: String): GooglePayExpressPayment = parse(json, null)

        /** Standard checkout selects delivery in the shop, not in the Google Pay sheet. */
        fun fromStandardJson(json: String, shippingOptionId: String): GooglePayExpressPayment {
            require(shippingOptionId.isNotBlank()) { "A shop shipping option is required" }
            return parse(json, shippingOptionId)
        }

        private fun parse(json: String, shopShippingOption: String?): GooglePayExpressPayment {
            try {
                val root = JsonParser.parseString(json).asJsonObject
                val method = root.getAsJsonObject("paymentMethodData")
                require(method.get("type").asString == "CARD")
                val tokenization = method.getAsJsonObject("tokenizationData")
                require(tokenization.get("type").asString == "PAYMENT_GATEWAY")
                val token = tokenization.get("token")
                require(token.isJsonPrimitive && token.asJsonPrimitive.isString && token.asString.isNotBlank())
                val email = root.get("email").asString.also { require(it.isNotBlank()) }
                val shipping = root.getAsJsonObject("shippingAddress")
                val billing = method.getAsJsonObject("info").getAsJsonObject("billingAddress")
                listOf(shipping, billing).forEach { address ->
                    listOf("countryCode", "postalCode", "locality", "address1", "name").forEach {
                        require(address.get(it)?.asString?.isNotBlank() == true)
                    }
                }
                val option = (shopShippingOption ?: root.getAsJsonObject("shippingOptionData").get("id").asString)
                    .also { require(it.isNotBlank()) }
                return GooglePayExpressPayment(token.asString, email, shipping.toString(), billing.toString(), option)
            } catch (_: Exception) {
                // Do not include payloads or parser exceptions (which can contain the token).
                throw IllegalArgumentException("Invalid Google Pay Express payment data")
            }
        }
    }
}

enum class GooglePayExpressResult {
    SUCCESS, CANCELLED, ERROR;

    companion object {
        fun fromStatus(status: Status): GooglePayExpressResult = when {
            status.isSuccess -> SUCCESS
            status.statusCode == CommonStatusCodes.CANCELED -> CANCELLED
            else -> ERROR
        }

    }
}
