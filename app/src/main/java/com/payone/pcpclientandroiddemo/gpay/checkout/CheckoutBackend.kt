package com.payone.pcpclientandroiddemo.gpay.checkout

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** The only checkout-creation entry point requires validated Google Pay payment data. */
class CheckoutBackend(private val config: CheckoutConfig) {
    private val apiBase = URL(config.backendUrl.trimEnd('/') + "/")

    // Merchant discovery belongs to the demo proxy UI, outside its API prefix.
    fun merchantId(): String = request("GET", URL(apiBase, "/ui/config")).get("merchantId").asString

    fun createCheckout(payment: GooglePayExpressPayment, reference: String): CheckoutOutcome =
        parseOutcome(request("POST", URL(apiBase, "commerce-cases"), createRequest(payment, reference, config)), reference)

    private fun request(method: String, url: URL, body: String? = null): JsonObject {
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            if (connection.responseCode !in 200..299) {
                // Never expose the raw body: a proxy may echo payment/contact data.
                val codes = try {
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        JsonParser.parseString(reader.readText()).asJsonObject.getAsJsonArray("errors")
                            ?.joinToString("; ") { error ->
                                listOf("errorCode", "propertyName").mapNotNull { key ->
                                    error.asJsonObject.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                                        ?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]{1,200}")) }
                                }.joinToString(": ")
                            }
                    }
                } catch (_: Exception) { null }
                throw CheckoutBackendException(connection.responseCode, codes)
            }
            return connection.inputStream.bufferedReader().use { JsonParser.parseString(it.readText()).asJsonObject }
        } finally { connection.disconnect() }
    }

    companion object {
        fun createRequest(payment: GooglePayExpressPayment, reference: String, config: CheckoutConfig): String {
            // Order references are limited to 20, unlike checkout references (40).
            require(reference.isNotBlank() && reference.length <= 20)
            val shipping = JsonParser.parseString(payment.shippingAddressJson).asJsonObject
            val billing = JsonParser.parseString(payment.billingAddressJson).asJsonObject
            val total = config.total(shipping.get("countryCode").asString, payment.shippingOptionId)
            val name = shipping.get("name").asString.trim().split(Regex("\\s+"), limit = 2)
            val shippingAddress = address(shipping).apply {
                add("name", Gson().toJsonTree(mapOf("firstName" to name.first(), "surname" to name.getOrElse(1) { name.first() })))
            }
            return Gson().toJson(mapOf(
                "merchantReference" to reference,
                "customer" to mapOf(
                    "billingAddress" to address(billing),
                    "contactDetails" to mapOf("emailAddress" to payment.email, "phoneNumber" to (billing.get("phoneNumber")?.asString ?: "")),
                ),
                "checkout" to mapOf(
                    "amountOfMoney" to mapOf("amount" to total, "currencyCode" to config.currencyCode),
                    "references" to mapOf("merchantReference" to reference),
                    "shipping" to mapOf("address" to shippingAddress),
                    "autoExecuteOrder" to config.executePaymentOnCreation,
                    // Google Pay data is provided even when automatic payment execution is disabled.
                    "orderRequest" to mapOf("orderType" to "FULL", "orderReferences" to mapOf("merchantReference" to reference), "paymentMethodSpecificInput" to mapOf(
                        "mobilePaymentMethodSpecificInput" to JsonParser.parseString(payment.mobilePaymentMethodSpecificInputJson(config.authorizationMode)),
                    )),
                ),
            ))
        }

        private fun address(source: JsonObject): JsonObject = JsonObject().apply {
            mapOf("countryCode" to "countryCode", "postalCode" to "zip", "locality" to "city", "address1" to "street").forEach { (from, to) ->
                addProperty(to, source.get(from).asString)
            }
            val extra = listOf("address2", "address3").mapNotNull { source.get(it)?.asString }.filter { it.isNotBlank() }
            if (extra.isNotEmpty()) addProperty("additionalInfo", extra.joinToString(", "))
        }

        fun parseOutcome(response: JsonObject, reference: String): CheckoutOutcome {
            val caseId = UUID.fromString(response.get("commerceCaseId").asString).toString()
            val checkout = response.getAsJsonObject("checkout")
            val checkoutId = UUID.fromString(checkout.get("checkoutId").asString).toString()
            return CheckoutOutcome(caseId, checkoutId, reference, isAuthorized(response))
        }

        /** A created checkout or HTTP 2xx is not proof of a successful payment. */
        fun isAuthorized(response: JsonObject): Boolean {
            val checkout = response.getAsJsonObject("checkout") ?: return false
            if (checkout.has("errorResponse")) return false
            val result = checkout.getAsJsonObject("paymentResponse") ?: return false
            if (result.has("merchantAction")) return false
            val payment = result.getAsJsonObject("payment") ?: return false
            return payment.get("id")?.asString?.isNotBlank() == true &&
                payment.get("status")?.asString in setOf("PENDING_CAPTURE", "CAPTURED") &&
                payment.getAsJsonObject("statusOutput")?.get("isAuthorized")?.asBoolean == true
        }
    }
}

/** Safe to display/store: contains identifiers and a verified result, no token/contact data. */
data class CheckoutOutcome(val commerceCaseId: String, val checkoutId: String, val reference: String, val authorized: Boolean)

class CheckoutBackendException(status: Int, codes: String?) : IllegalStateException(
    "Backend HTTP $status" + if (codes.isNullOrBlank()) "" else " (codes: $codes)",
)
