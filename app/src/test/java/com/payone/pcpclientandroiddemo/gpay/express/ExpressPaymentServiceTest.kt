package com.payone.pcpclientandroiddemo.gpay.express

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.gson.JsonParser
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import com.payone.pcpclientandroiddemo.gpay.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/** Exercise real native service handlers: quote only, correlated final payload, one POST. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ExpressPaymentServiceTest {
    private fun <T> await(task: Task<T>): T {
        val future = CompletableFuture<T>()
        task.addOnCompleteListener(Executor { it.run() }) {
            if (it.isSuccessful) { future.complete(it.result) } else { future.completeExceptionally(requireNotNull(it.exception)) }
        }
        return future.get(5, TimeUnit.SECONDS)
    }
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun shippingQuoteAndRepeatedAuthorizationCreateOneMatchingOrder() {
        val server = MockWebServer()
        server.start()
        val controller = Robolectric.buildService(ExpressPaymentService::class.java).create()
        try {
            val service = controller.get()
            val store = PaymentAttemptStore(context)
            store.start("ref", "EXPRESS", ConfigService(server.url("/api/").toString(), "merchant"), "standard")
            val update = await(service.onShippingChanged(IntermediatePaymentData.fromJson(
                """{"callbackTrigger":"SHIPPING_OPTION","shippingAddress":{"countryCode":"AT"},"shippingOptionData":{"id":"express"}}""")))
            val info = JsonParser.parseString(update.toJson()).asJsonObject.getAsJsonObject("newTransactionInfo")
            assertEquals("29.80", info.get("totalPrice").asString)
            assertEquals("Total", info.get("totalPriceLabel").asString)
            assertEquals(0, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"commerceCaseId":"case","checkout":{"checkoutId":"checkout","paymentResponse":{"payment":{"id":"payment","status":"CAPTURED","statusOutput":{"isAuthorized":true}}}}}"""))
            val payment = GooglePayExpressPayment.fromJson("""{"email":"buyer@example.com","shippingAddress":{"name":"Ada Lovelace","countryCode":"DE","postalCode":"10115","locality":"Berlin","address1":"Street 1"},"shippingOptionData":{"id":"express"},"paymentMethodData":{"type":"CARD","info":{"billingAddress":{"name":"Ada Lovelace","countryCode":"US","postalCode":"10001","locality":"New York","address1":"Street 1"}},"tokenizationData":{"type":"PAYMENT_GATEWAY","token":"opaque-token"}}}""")
            assertTrue(await(service.onAuthorize(payment, update.updatedSavedState)))
            assertTrue(await(service.onAuthorize(payment, update.updatedSavedState)))
            assertEquals(1, server.requestCount)
            val body = JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
            val checkout = body.getAsJsonObject("checkout")
            assertEquals("US", body.getAsJsonObject("customer").getAsJsonObject("billingAddress").get("countryCode").asString)
            assertEquals("DE", checkout.getAsJsonObject("shipping").getAsJsonObject("address").get("countryCode").asString)
            assertEquals(2980, checkout.getAsJsonObject("amountOfMoney").get("amount").asInt)
            val items = checkout.getAsJsonObject("shoppingCart").getAsJsonArray("items")
            assertEquals(990, items[1].asJsonObject.getAsJsonObject("orderLineDetails").get("productPrice").asInt)
            val mobile = checkout.getAsJsonObject("orderRequest").getAsJsonObject("paymentMethodSpecificInput").getAsJsonObject("mobilePaymentMethodSpecificInput")
            assertEquals("opaque-token", kotlin.io.encoding.Base64.decode(mobile.get("encryptedPaymentData").asString).toString(Charsets.UTF_8))
            assertEquals("SALE", mobile.get("authorizationMode").asString)
            assertFalse(context.getSharedPreferences("google-pay-express", Context.MODE_PRIVATE).all.toString().contains("opaque-token"))
        } finally { controller.destroy(); server.shutdown() }
    }
}
