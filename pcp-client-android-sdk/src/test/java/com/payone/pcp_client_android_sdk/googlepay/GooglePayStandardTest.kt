package com.payone.pcp_client_android_sdk.googlepay

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class GooglePayStandardTest {
    @Test fun standardRequestIsFinalAndHasNoExpressCallbacksOrShippingOptionSelection() {
        val standard = GooglePayStandard(GooglePayExpressConfig("merchant", "Shop"))
        val request = JsonParser.parseString(standard.requestJson("14.99", "order-123")).asJsonObject
        assertEquals("FINAL", request.getAsJsonObject("transactionInfo").get("totalPriceStatus").asString)
        assertEquals("14.99", request.getAsJsonObject("transactionInfo").get("totalPrice").asString)
        assertFalse(request.has("callbackIntents"))
        assertFalse(request.has("shippingOptionRequired"))
        assertTrue(request.get("shippingAddressRequired").asBoolean)
        val method = request.getAsJsonArray("allowedPaymentMethods")[0].asJsonObject
        assertEquals("payonegmbh", method.getAsJsonObject("tokenizationSpecification").getAsJsonObject("parameters").get("gateway").asString)
        assertNotNull(standard.paymentDataRequest("14.99", "order-123"))
        assertNotNull(standard.readinessRequest())
    }

    @Test fun standardPaymentUsesShopShippingOptionAndPreservesOpaqueToken() {
        val raw = """{"email":"buyer@example.test","shippingAddress":{"name":"Test Buyer","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Teststrasse 1"},"paymentMethodData":{"type":"CARD","info":{"billingAddress":{"name":"Test Buyer","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Teststrasse 1"}},"tokenizationData":{"type":"PAYMENT_GATEWAY","token":" opaque-token "}}}"""
        val payment = GooglePayExpressPayment.fromStandardJson(raw, "shop-express")
        assertEquals("shop-express", payment.shippingOptionId)
        assertEquals(" opaque-token ", payment.encryptedPaymentData)
        assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromJson(raw) }
        assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromStandardJson(raw, "") }
        assertThrows(IllegalArgumentException::class.java) { GooglePayExpressPayment.fromStandardJson("{}", "standard") }
    }
}
