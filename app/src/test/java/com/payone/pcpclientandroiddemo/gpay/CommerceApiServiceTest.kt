package com.payone.pcpclientandroiddemo.gpay

import com.google.gson.JsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

/** Real HTTP tests: preserve deployment paths, distinguish failures, never retry writes. */
class CommerceApiServiceTest {
    @Test fun postAndStatusGetPreservePrefixAndEncodeIdentifiers() {
        val server = MockWebServer()
        server.start()
        try {
            val api = CommerceApiService(server.url("/api/v1").toString())
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"commerceCaseId":"case","checkout":{"checkoutId":"checkout"}}"""))
            assertEquals("case", api.createCommerceCase(JsonObject()).body.commerceCaseId)
            assertEquals("/api/v1/commerce-cases", server.takeRequest().path)
            server.enqueue(MockResponse().setBody("""{"statusOutput":{"paymentStatus":"PAYMENT_COMPLETED"}}"""))
            assertEquals("succeeded", checkoutState(api.getCheckout("case/1", "checkout 2").body))
            assertEquals("/api/v1/commerce-cases/case%2F1/checkouts/checkout%202", server.takeRequest().path)
        } finally { server.shutdown() }
    }

    @Test fun httpAndMalformedResponseDoNotRetryOrExposeSensitiveMessages() {
        val server = MockWebServer()
        server.start()
        try {
            val api = CommerceApiService(server.url("/").toString())
            server.enqueue(MockResponse().setResponseCode(422).setBody("""{"errors":[{"errorCode":"1201","message":"secret-token buyer@example.com"}]}"""))
            val error = assertThrows(CommerceApiError::class.java) { api.createCommerceCase(JsonObject()) }
            assertEquals("http", error.kind)
            assertEquals(422, error.status)
            assertEquals(listOf("1201"), error.codes)
            assertFalse(error.message!!.contains("secret-token"))
            server.enqueue(MockResponse().setBody("not json"))
            assertEquals("invalid-response", assertThrows(CommerceApiError::class.java) { api.createCommerceCase(JsonObject()) }.kind)
            assertEquals(2, server.requestCount)
        } finally { server.shutdown() }
    }
}
