package com.payone.pcp_client_android_sdk.googlepay

import com.google.android.gms.common.api.Status
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

internal const val PAYMENT = """{"email":"buyer@example.test","shippingAddress":{"name":"Ada Lovelace","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Teststrasse 1"},"shippingOptionData":{"id":"standard"},"paymentMethodData":{"type":"CARD","info":{"billingAddress":{"name":"Ada Lovelace","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Teststrasse 1"}},"tokenizationData":{"type":"PAYMENT_GATEWAY","token":"fixture-token"}}}"""

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 28, 35, 36, 37], manifest = Config.NONE)
class GooglePayExpressTest {
    private val express = GooglePayExpress(GooglePayExpressConfig("payone-mid", "Merchant"))

    @Test fun requestUsesNativeCallbacksAndPayoneGateway() {
        val json = JsonParser.parseString(express.requestJson("10.00", "order-123")).asJsonObject
        assertEquals("ESTIMATED", json.getAsJsonObject("transactionInfo").get("totalPriceStatus").asString)
        assertEquals("DEFAULT", json.getAsJsonObject("transactionInfo").get("checkoutOption").asString)
        assertEquals("order-123", json.getAsJsonObject("transactionInfo").get("transactionId").asString)
        val intents = json.getAsJsonArray("callbackIntents").map { it.asString }
        assertEquals(setOf("PAYMENT_AUTHORIZATION", "SHIPPING_ADDRESS", "SHIPPING_OPTION"), intents.toSet())
        assertFalse(json.getAsJsonObject("merchantInfo").has("merchantId"))
        val params = json.getAsJsonArray("allowedPaymentMethods")[0].asJsonObject.getAsJsonObject("tokenizationSpecification").getAsJsonObject("parameters")
        assertEquals("payonegmbh", params.get("gateway").asString)
        assertEquals("payone-mid", params.get("gatewayMerchantId").asString)
        assertNotNull(express.readinessRequest())
        assertNotNull(express.paymentDataRequest("10.00", "order-123"))
    }

    @Test fun tokenIsPreservedExactlyAndMappedToProduct320() {
        val root = JsonParser.parseString(validPayment()).asJsonObject
        val payment = GooglePayExpressPayment.fromJson(root.toString())
        val input = JsonParser.parseString(payment.mobilePaymentMethodSpecificInputJson()).asJsonObject
        assertEquals(root.getAsJsonObject("paymentMethodData").getAsJsonObject("tokenizationData").get("token").asString, input.get("encryptedPaymentData").asString)
        assertEquals(320, input.get("paymentProductId").asInt)
        assertEquals("PRE_AUTHORIZATION", input.get("authorizationMode").asString)
        assertFalse(payment.toString().contains("opaque"))
    }

    @Test fun missingTokenAndAddressAreRejectedWithoutLeakingData() {
        for (field in listOf("token", "address1")) {
            val root = JsonParser.parseString(validPayment()).asJsonObject
            if (field == "token") root.getAsJsonObject("paymentMethodData").getAsJsonObject("tokenizationData").remove(field)
            else root.getAsJsonObject("shippingAddress").remove(field)
            val error = assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromJson(root.toString()) }
            assertEquals("Invalid Google Pay Express payment data", error.message)
            assertNull(error.cause)
        }
    }

    @Test fun malformedPaymentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromJson("not-json") }
    }

    @Test fun invalidConfigurationAndAmountsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { GooglePayExpressConfig("", "Merchant") }
        assertThrows(IllegalArgumentException::class.java) { GooglePayExpressConfig("mid", "Merchant", shippingCountries = setOf("XX")) }
        listOf("-1.00", "0.00", "1.001").forEach { amount ->
            assertThrows(IllegalArgumentException::class.java) { express.requestJson(amount, "order") }
        }
    }

    @Test fun walletSuccessCancellationAndErrorRemainDistinct() {
        assertEquals(GooglePayExpressResult.SUCCESS, GooglePayExpressResult.fromStatus(Status.RESULT_SUCCESS))
        assertEquals(GooglePayExpressResult.CANCELLED, GooglePayExpressResult.fromStatus(Status.RESULT_CANCELED))
        assertEquals(GooglePayExpressResult.ERROR, GooglePayExpressResult.fromStatus(Status.RESULT_INTERNAL_ERROR))
    }
}

// Use Gson to escape a deliberately opaque token, rather than depending on a handwritten token encoding.
internal fun validPayment(): String {
    val root = JsonParser.parseString(PAYMENT).asJsonObject
    root.getAsJsonObject("paymentMethodData").getAsJsonObject("tokenizationData").addProperty("token", "  opaque \"token\"\\payload  ")
    return root.toString()
}
