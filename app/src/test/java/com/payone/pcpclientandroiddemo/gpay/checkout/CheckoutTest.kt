package com.payone.pcpclientandroiddemo.gpay.checkout

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class CheckoutTest {
    private val config = CheckoutConfig()

    @Test fun allShippingTriggersAndOptionsReturnFinalTotals() {
        listOf("INITIALIZE", "SHIPPING_ADDRESS", "SHIPPING_OPTION").forEach { trigger ->
            listOf("standard" to "10.00", "express" to "14.99").forEach { (option, total) ->
                val update = JsonParser.parseString(CheckoutPricing(config).update("""{"callbackTrigger":"$trigger","shippingAddress":{"countryCode":"DE"},"shippingOptionData":{"id":"$option"}}""")).asJsonObject
                assertEquals(total, update.getAsJsonObject("newTransactionInfo").get("totalPrice").asString)
                assertEquals("FINAL", update.getAsJsonObject("newTransactionInfo").get("totalPriceStatus").asString)
                assertEquals(option, update.getAsJsonObject("newShippingOptionParameters").get("defaultSelectedOptionId").asString)
            }
        }
    }

    @Test fun nativeInitialOptionGetsRealDefaultAndUnsupportedDeliveryFails() {
        listOf("shipping_option_unselected", "").forEach { option ->
            val update = JsonParser.parseString(CheckoutPricing(config).update("""{"shippingAddress":{"countryCode":"DE"},"shippingOptionData":{"id":"$option"}}""")).asJsonObject
            assertEquals("10.00", update.getAsJsonObject("newTransactionInfo").get("totalPrice").asString)
        }
        val error = JsonParser.parseString(CheckoutPricing(config).update("""{"shippingAddress":{"countryCode":"US"}}""")).asJsonObject
        assertEquals("SHIPPING_ADDRESS_UNSERVICEABLE", error.getAsJsonObject("error").get("reason").asString)
        assertThrows(IllegalArgumentException::class.java) { config.total("US", "standard") }
        assertThrows(NoSuchElementException::class.java) { config.total("DE", "fake-free") }
    }

    @Test fun configurationControlsPricingAndPaymentInputInBothModes() {
        val custom = config.copy(basketAmount = 2500, deliveryOptions = listOf(DeliveryOption("next-day", "Next day", 700)), authorizationMode = "SALE")
        listOf(CheckoutMode.STANDARD, CheckoutMode.EXPRESS).forEach { mode ->
            val root = JsonParser.parseString(PAYMENT).asJsonObject.apply {
                getAsJsonObject("shippingOptionData").addProperty("id", "next-day")
                if (mode == CheckoutMode.STANDARD) remove("shippingOptionData")
            }
            val payment = if (mode == CheckoutMode.EXPRESS) GooglePayExpressPayment.fromJson(root.toString())
                else GooglePayExpressPayment.fromStandardJson(root.toString(), "next-day")
            val body = JsonParser.parseString(CheckoutBackend.createRequest(payment, "order-123", custom)).asJsonObject
            val checkout = body.getAsJsonObject("checkout")
            assertEquals(3200, checkout.getAsJsonObject("amountOfMoney").get("amount").asInt)
            assertFalse(checkout.get("autoExecuteOrder").asBoolean)
            assertEquals("order-123", checkout.getAsJsonObject("orderRequest").getAsJsonObject("orderReferences").get("merchantReference").asString)
            val mobile = checkout.getAsJsonObject("orderRequest").getAsJsonObject("paymentMethodSpecificInput").getAsJsonObject("mobilePaymentMethodSpecificInput")
            assertEquals(320, mobile.get("paymentProductId").asInt)
            assertEquals("opaque-token", mobile.get("encryptedPaymentData").asString)
            assertEquals("SALE", mobile.get("authorizationMode").asString)
            assertEquals("Berlin", body.getAsJsonObject("customer").getAsJsonObject("billingAddress").get("city").asString)
        }
        assertThrows(IllegalArgumentException::class.java) { config.copy(deliveryOptions = listOf(DeliveryOption("x", "X", -1))) }
        assertThrows(IllegalArgumentException::class.java) { config.copy(authorizationMode = "FAKE") }
    }

    @Test fun noPaymentInputCanBeBuiltFromEmptyOrIncompleteGooglePayData() {
        listOf("{}", """{"paymentMethodData":{"type":"CARD","tokenizationData":{"type":"PAYMENT_GATEWAY","token":""}}}""").forEach { json ->
            assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromJson(json) }
            assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromStandardJson(json, "standard") }
        }
    }

    @Test fun orderReferenceHonorsTheStricterTwentyCharacterLimitAndPaymentExecutionIsExplicit() {
        val payment = GooglePayExpressPayment.fromJson(PAYMENT)
        val reference = "12345678901234567890"
        val checkout = JsonParser.parseString(CheckoutBackend.createRequest(payment, reference,
            config.copy(executePaymentOnCreation = true))).asJsonObject.getAsJsonObject("checkout")
        assertTrue(checkout.get("autoExecuteOrder").asBoolean)
        assertEquals(reference, checkout.getAsJsonObject("orderRequest").getAsJsonObject("orderReferences").get("merchantReference").asString)
        assertThrows(IllegalArgumentException::class.java) { CheckoutBackend.createRequest(payment, reference + "1", config) }
    }

    @Test fun backendErrorsExposeOnlyCodesAndFieldNamesWithoutEchoedPaymentData() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(400).setBody(
                """{"errors":[{"errorCode":"50308003","propertyName":"orderReferences.merchantReference","message":"opaque-token buyer@example.test"}]}"""))
            val error = assertThrows(CheckoutBackendException::class.java) {
                CheckoutBackend(config.copy(backendUrl = server.url("/").toString()))
                    .createCheckout(GooglePayExpressPayment.fromJson(PAYMENT), "order-123")
            }
            assertTrue(error.message!!.contains("50308003"))
            assertTrue(error.message!!.contains("orderReferences.merchantReference"))
            assertFalse(error.message!!.contains("opaque-token"))
            assertFalse(error.message!!.contains("buyer@example.test"))
        } finally { server.shutdown() }
    }

    @Test fun createdCheckoutAndSuccessfulPaymentAreDifferentOutcomes() {
        val created = response("CREATED", authorized = false)
        assertFalse(CheckoutBackend.parseOutcome(created, "order-123").authorized)
        listOf("CREATED", "PENDING_PAYMENT", "REDIRECTED", "REJECTED", "CANCELLED", "AUTHORIZATION_REQUESTED").forEach {
            assertFalse(CheckoutBackend.isAuthorized(response(it)))
        }
        assertTrue(CheckoutBackend.isAuthorized(response("PENDING_CAPTURE")))
        assertTrue(CheckoutBackend.isAuthorized(response("CAPTURED")))
        assertFalse(CheckoutBackend.isAuthorized(response("PENDING_CAPTURE", authorized = false)))
        val redirect = response("PENDING_CAPTURE").apply { getAsJsonObject("checkout").getAsJsonObject("paymentResponse").add("merchantAction", JsonObject()) }
        assertFalse(CheckoutBackend.isAuthorized(redirect))
        val declined = response("PENDING_CAPTURE").apply { getAsJsonObject("checkout").add("errorResponse", JsonObject()) }
        assertFalse(CheckoutBackend.parseOutcome(declined, "order-123").authorized)
        assertThrows(Exception::class.java) { CheckoutBackend.parseOutcome(JsonObject(), "order-123") }
    }

    @Test fun realHttpContractAlwaysIncludesGooglePayAndDoesNotFallbackOnFailure() {
        val server = MockWebServer()
        server.start()
        try {
            val backend = CheckoutBackend(config.copy(backendUrl = server.url("/api/").toString()))
            server.enqueue(MockResponse().setResponseCode(201).setBody(response("REJECTED", false).toString()))
            val result = backend.createCheckout(GooglePayExpressPayment.fromJson(PAYMENT), "order-123")
            assertEquals(CHECKOUT_ID, result.checkoutId)
            assertFalse(result.authorized)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/commerce-cases", request.path)
            val body = JsonParser.parseString(request.body.readUtf8()).asJsonObject
            assertFalse(body.getAsJsonObject("checkout").get("autoExecuteOrder").asBoolean)
            assertTrue(request.bodySize > 0)
            val mobile = body.getAsJsonObject("checkout").getAsJsonObject("orderRequest").getAsJsonObject("paymentMethodSpecificInput").getAsJsonObject("mobilePaymentMethodSpecificInput")
            assertEquals("opaque-token", mobile.get("encryptedPaymentData").asString)
            server.enqueue(MockResponse().setResponseCode(503))
            assertThrows(IllegalStateException::class.java) { backend.createCheckout(GooglePayExpressPayment.fromJson(PAYMENT), "order-123") }
            assertEquals(2, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun apiBaseSupportsRootAndCustomPrefixesWithOrWithoutTrailingSlash() {
        assertEquals("http://localhost:3009/api/", CheckoutConfig().backendUrl)
        val server = MockWebServer()
        server.start()
        try {
            listOf("/", "/api/", "/api", "/payments/v1/", "/payments/v1").forEach { basePath ->
                val backend = CheckoutBackend(config.copy(backendUrl = server.url(basePath).toString()))
                server.enqueue(MockResponse().setBody("""{"merchantId":"test-merchant"}"""))
                assertEquals("test-merchant", backend.merchantId())
                assertEquals("/ui/config", server.takeRequest().path)
                server.enqueue(MockResponse().setResponseCode(201).setBody(response("REJECTED", false).toString()))
                backend.createCheckout(GooglePayExpressPayment.fromJson(PAYMENT), "order-123")
                assertEquals(basePath.trimEnd('/') + "/commerce-cases", server.takeRequest().path)
            }
        } finally { server.shutdown() }
    }

    companion object {
        const val CASE_ID = "11111111-1111-4111-8111-111111111111"
        const val CHECKOUT_ID = "22222222-2222-4222-8222-222222222222"
        const val PAYMENT = """{"email":"buyer@example.test","shippingAddress":{"name":"Ada Lovelace","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Teststrasse 1"},"shippingOptionData":{"id":"express"},"paymentMethodData":{"type":"CARD","info":{"billingAddress":{"name":"Ada Lovelace","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Teststrasse 1"}},"tokenizationData":{"type":"PAYMENT_GATEWAY","token":"opaque-token"}}}"""
        fun response(status: String, authorized: Boolean = true) = JsonParser.parseString(
            """{"commerceCaseId":"$CASE_ID","checkout":{"checkoutId":"$CHECKOUT_ID","paymentResponse":{"payment":{"id":"payment-123","status":"$status","statusOutput":{"isAuthorized":$authorized}}}}}""",
        ).asJsonObject
    }
}
