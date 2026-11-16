package com.payone.pcpclientandroiddemo.gpay

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigDecimal
import kotlin.io.encoding.Base64

/** Stateless address/token adapters and backend result mapping used by both demo flows. */
fun money(amount: Long): String = BigDecimal.valueOf(amount, 2).toPlainString()

fun googleAddress(source: JsonObject): JsonObject {
    val country = source.get("countryCode")?.asString.orEmpty()
    require(country.matches(Regex("[A-Z]{2}"))) { "Invalid address country" }
    return JsonObject().apply {
        mapOf("countryCode" to "countryCode", "postalCode" to "zip", "locality" to "city", "address1" to "street").forEach { (from, to) ->
            val value = source.get(from)?.asString
            require(!value.isNullOrBlank()) { "Incomplete address" }
            addProperty(to, value)
        }
        val extra = listOf("address2", "address3").mapNotNull { source.get(it)?.asString }.filter { it.isNotBlank() }
        if (extra.isNotEmpty()) {
            addProperty("additionalInfo", extra.joinToString(", "))
        }
        val name = source.get("name")?.asString.orEmpty().trim().split(Regex("\\s+"))
        require(name.first().isNotBlank()) { "Missing customer name" }
        add("name", Gson().toJsonTree(mapOf("firstName" to name.dropLast(1).joinToString(" ").ifBlank { name.first() }, "surname" to name.last())))
    }
}

fun googleToken(json: String): String {
    try {
        val method = JsonParser.parseString(json).asJsonObject.getAsJsonObject("paymentMethodData")
        require(method.get("type").asString == "CARD")
        val data = method.getAsJsonObject("tokenizationData")
        require(data.get("type").asString == "PAYMENT_GATEWAY")
        val token = data.get("token")
        require(token.isJsonPrimitive && token.asJsonPrimitive.isString && token.asString.isNotBlank())
        return Base64.encode(token.asString.toByteArray(Charsets.UTF_8))
    } catch (_: Exception) {
        throw IllegalArgumentException("Invalid Google Pay token")
    }
}

data class PaymentOutcome(val state: String, val commerceCaseId: String, val checkoutId: String, val redirectUrl: String? = null,
    val errorCodes: List<String>? = null, val errorId: String? = null) {
    val authorized: Boolean get() = state in setOf("authorized", "succeeded")
}

fun createdOutcome(response: ApiResponse<CreateResponse>): PaymentOutcome {
    val body = response.body
    val checkout = requireNotNull(body.checkout) { "Missing checkout in backend response" }
    val caseId = requireNotNull(body.commerceCaseId?.takeIf { it.isNotBlank() }) { "Missing commerce case ID" }
    val checkoutId = requireNotNull(checkout.checkoutId?.takeIf { it.isNotBlank() }) { "Missing checkout ID" }
    val payment = checkout.paymentResponse?.payment
    val redirect = checkout.paymentResponse?.merchantAction?.redirectData?.redirectURL
    val state = when {
        checkout.errorResponse != null || payment?.status in setOf("REJECTED", "REJECTED_CAPTURE", "CANCELLED") -> "failed"
        redirect != null -> "requires-action"
        response.status == 202 -> "pending"
        payment?.status == "CAPTURED" -> "succeeded"
        payment?.status == "PENDING_CAPTURE" && payment.id?.isNotBlank() == true && payment.statusOutput?.isAuthorized == true -> "authorized"
        else -> "pending"
    }
    val error = checkout.errorResponse
    val errorId = error?.get("errorId")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
        ?.asString?.takeIf { it.matches(Regex("[A-Za-z0-9_.-]{1,100}")) }
    return PaymentOutcome(state, caseId, checkoutId, redirect, safeErrorCodes(error), errorId)
}

fun checkoutState(response: CheckoutResponse): String {
    if (response.statusOutput?.paymentStatus == "PAYMENT_COMPLETED") {
        return "succeeded"
    }
    val last = response.paymentExecutions.orEmpty().flatMap { it.events.orEmpty() }.lastOrNull()?.paymentStatus
    return if (last in setOf("REJECTED", "REJECTED_CAPTURE", "CANCELLED")) { "failed" } else { "pending" }
}

fun describe(outcome: PaymentOutcome): String =
    "Payment: ${outcome.state}\nCommerce case: ${outcome.commerceCaseId}\nCheckout: ${outcome.checkoutId}" +
        (if (outcome.redirectUrl != null) { "\nAdditional action required. Continue via your merchant backend." } else { "" }) +
        (if (outcome.errorCodes.isNullOrEmpty()) { "" } else { "\nPAYONE codes: ${outcome.errorCodes.joinToString()}" }) +
        (outcome.errorId?.let { "\nPAYONE error ID: $it" } ?: "")
