package com.payone.pcpclientandroiddemo.gpay

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import kotlin.io.encoding.Base64

/** Payment state and adapters are verified independently from Android UI. */
class GooglePayUtilsTest {
    @Test fun standardTokenNeedsNoWalletContactsAndIsEncodedExactlyOnce() {
        val token = "{\"signedMessage\":\"ä +/==\"}"
        val json = Gson().toJson(mapOf("paymentMethodData" to mapOf("type" to "CARD", "tokenizationData" to mapOf("type" to "PAYMENT_GATEWAY", "token" to token))))
        val encoded = googleToken(json)
        assertEquals(token, Base64.decode(encoded).toString(Charsets.UTF_8))
        assertFalse(encoded.contains('\n'))
        assertThrows(IllegalArgumentException::class.java) { googleToken("{}") }
    }

    @Test fun nestedPaymentErrorsAreDisplayedWithoutServerMessages() {
        val outcome = createdOutcome(ApiResponse(Gson().fromJson(
            """{"commerceCaseId":"case","checkout":{"checkoutId":"checkout","errorResponse":{"errorId":"error-123","errors":[{"errorCode":"1234","message":"secret-token"},{"errorCode":"unsafe code\nsecret"}]}}}""", CreateResponse::class.java), 200))
        assertEquals("failed", outcome.state)
        assertEquals(listOf("1234"), outcome.errorCodes)
        assertTrue(describe(outcome).contains("error-123"))
        assertFalse(describe(outcome).contains("secret"))
    }

    @Test fun createdPendingAuthorizedAndCapturedAreDistinct() {
        fun outcome(status: String, authorized: Boolean, http: Int = 201) = createdOutcome(ApiResponse(Gson().fromJson(
            """{"commerceCaseId":"case","checkout":{"checkoutId":"checkout","paymentResponse":{"payment":{"id":"payment","status":"$status","statusOutput":{"isAuthorized":$authorized}}}}}""", CreateResponse::class.java), http))
        assertEquals("pending", outcome("CREATED", false).state)
        assertFalse(outcome("PENDING_CAPTURE", false).authorized)
        assertTrue(outcome("PENDING_CAPTURE", true).authorized)
        assertEquals("succeeded", outcome("CAPTURED", true).state)
        assertEquals("pending", outcome("CAPTURED", true, 202).state)
        assertEquals("failed", outcome("REJECTED", false).state)
        assertEquals("pending", checkoutState(Gson().fromJson("""{"paymentExecutions":[{"events":[{"paymentStatus":"CAPTURED"}]}]}""", CheckoutResponse::class.java)))
    }

    @Test fun fullAddressKeepsAdditionalLinesAndAcceptsInternationalCountries() {
        val address = JsonParser.parseString("""{"countryCode":"DE","name":"Ada Byron Lovelace","postalCode":"10115","locality":"Berlin","address1":"Street 1","address2":"Floor 2"}""").asJsonObject
        val mapped = googleAddress(address)
        assertEquals("Floor 2", mapped.get("additionalInfo").asString)
        assertEquals("Ada Byron", mapped.getAsJsonObject("name").get("firstName").asString)
        address.addProperty("countryCode", "US")
        assertEquals("US", googleAddress(address).get("countryCode").asString)
        address.addProperty("countryCode", "")
        assertThrows(IllegalArgumentException::class.java) { googleAddress(address) }
    }
}
