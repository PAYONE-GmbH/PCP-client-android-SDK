package com.payone.pcpclientandroiddemo.gpay

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException

/** Merchant API transport only. POST is single-shot; GET reconciles an existing checkout. */
class CommerceApiService(baseUrl: String) {
    private val base = URL(ConfigService.normalize(baseUrl))

    fun createCommerceCase(body: JsonObject): ApiResponse<CreateResponse> =
        request("POST", "commerce-cases", body, CreateResponse::class.java)

    fun getCheckout(caseId: String, checkoutId: String): ApiResponse<CheckoutResponse> =
        request("GET", "commerce-cases/${encode(caseId)}/checkouts/${encode(checkoutId)}", null, CheckoutResponse::class.java)

    private fun encode(value: String): String {
        require(value.isNotBlank()) { "Missing checkout identifiers" }
        return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private fun <T> request(method: String, path: String, body: JsonObject?, type: Class<T>): ApiResponse<T> {
        val connection = URL(base, path).openConnection() as HttpURLConnection
        var status: Int? = null
        try {
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            status = connection.responseCode
            val stream = if (status in 200..299) { connection.inputStream } else { connection.errorStream }
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val parsed = try { JsonParser.parseString(text).asJsonObject } catch (_: Exception) { null }
            if (status !in 200..299) {
                throw CommerceApiError("http", method, status, safeErrorCodes(parsed))
            }
            if (parsed == null) {
                throw CommerceApiError("invalid-response", method, status)
            }
            val result = try { Gson().fromJson(parsed, type) }
            catch (_: Exception) { throw CommerceApiError("invalid-response", method, status) }
            return ApiResponse(result, status)
        } catch (error: IOException) {
            throw CommerceApiError("network", method, status)
        } finally {
            connection.disconnect()
        }
    }
}

/** Whitelist diagnostic codes only; never display server messages or echoed payment data. */
fun safeErrorCodes(error: JsonObject?): List<String> = try {
    error?.getAsJsonArray("errors")?.mapNotNull {
        it.asJsonObject.get("errorCode")?.takeIf { code -> code.isJsonPrimitive && code.asJsonPrimitive.isString }
            ?.asString?.takeIf { code -> code.matches(Regex("[A-Za-z0-9_.-]{1,100}")) }
    }.orEmpty()
} catch (_: Exception) { emptyList() }

/** Only safe error codes are displayed; server messages can echo sensitive payment data. */
class CommerceApiError(val kind: String, val method: String, val status: Int? = null, val codes: List<String> = emptyList()) :
    IllegalStateException("Backend $method $kind${status?.let { " (HTTP $it)" }.orEmpty()}${if (codes.isEmpty()) { "" } else { ": ${codes.joinToString()}" }}")

data class ApiResponse<T>(val body: T, val status: Int)
data class CreateResponse(val commerceCaseId: String?, val checkout: CreatedCheckout?)
data class CreatedCheckout(val checkoutId: String?, val errorResponse: JsonObject?, val paymentResponse: PaymentResponse?)
data class PaymentResponse(val payment: Payment?, val merchantAction: MerchantAction?)
data class Payment(val id: String?, val status: String?, val statusOutput: PaymentStatus?)
data class PaymentStatus(val isAuthorized: Boolean?)
data class MerchantAction(val redirectData: RedirectData?)
data class RedirectData(val redirectURL: String?)
data class CheckoutResponse(val statusOutput: CheckoutStatus?, val paymentExecutions: List<PaymentExecution>?)
data class CheckoutStatus(val paymentStatus: String?)
data class PaymentExecution(val events: List<PaymentEvent>?)
data class PaymentEvent(val paymentStatus: String?)
