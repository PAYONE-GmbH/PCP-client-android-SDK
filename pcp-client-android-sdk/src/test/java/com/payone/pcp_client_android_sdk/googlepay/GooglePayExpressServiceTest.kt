package com.payone.pcp_client_android_sdk.googlepay

import android.os.Bundle
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.PaymentAuthorizationResult
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 28, 35, 36, 37], manifest = Config.NONE)
class GooglePayExpressServiceTest {
    private class Service : GooglePayExpressService() {
        var authorization: () -> Task<Boolean> = { Tasks.forResult(true) }
        var shipping: () -> Task<PaymentDataRequestUpdate> = { Tasks.forResult(PaymentDataRequestUpdate.fromJson("{}")) }
        var calls = 0
        var saved: String? = null
        fun callbacks() = createPaymentDataCallbacks()
        override fun onShippingChanged(request: IntermediatePaymentData) = shipping()
        override fun onAuthorize(payment: GooglePayExpressPayment, savedState: Bundle?): Task<Boolean> {
            calls++
            saved = savedState?.getString("reference")
            return authorization()
        }
    }

    private fun state(result: PaymentAuthorizationResult) = JsonParser.parseString(result.toJson()).asJsonObject.get("transactionState").asString

    // Invoke Google's protected entry points exactly as its service transport does.
    private fun BasePaymentDataCallbacks.authorize(data: PaymentData?, listener: OnCompleteListener<PaymentAuthorizationResult>) {
        BasePaymentDataCallbacks::class.java.getDeclaredMethod("onPaymentAuthorized", PaymentData::class.java, OnCompleteListener::class.java)
            .apply { isAccessible = true }.invoke(this, data, listener)
    }
    private fun BasePaymentDataCallbacks.change(data: IntermediatePaymentData?, listener: OnCompleteListener<PaymentDataRequestUpdate>) {
        BasePaymentDataCallbacks::class.java.getDeclaredMethod("onPaymentDataChanged", IntermediatePaymentData::class.java, OnCompleteListener::class.java)
            .apply { isAccessible = true }.invoke(this, data, listener)
    }

    @Test fun confirmedBackendReturnsSuccessAndReceivesSavedState() {
        val service = Service()
        var response: PaymentAuthorizationResult? = null
        service.callbacks().authorize(
            PaymentData.fromJson(validPayment()).withLastSavedState(Bundle().apply { putString("reference", "order") }),
        ) { response = it }
        assertEquals("SUCCESS", state(requireNotNull(response)))
        assertEquals("order", service.saved)
        assertEquals(1, service.calls)
    }

    @Test fun declinedFailedCancelledAndThrowingBackendReturnSheetError() {
        val cancelled = TaskCompletionSource<Boolean>().apply { setException(java.util.concurrent.CancellationException()) }.task
        val cases: List<() -> Task<Boolean>> = listOf(
            { Tasks.forResult(false) }, { Tasks.forException(IllegalStateException("backend")) },
            { cancelled }, { throw IllegalStateException("synchronous") },
        )
        cases.forEach { backend ->
            val service = Service().apply { authorization = backend }
            var response: PaymentAuthorizationResult? = null
            service.callbacks().authorize(PaymentData.fromJson(validPayment())) { response = it }
            assertEquals("ERROR", state(requireNotNull(response)))
        }
    }

    @Test fun invalidPaymentNeverReachesBackend() {
        val service = Service()
        var response: PaymentAuthorizationResult? = null
        service.callbacks().authorize(PaymentData.fromJson("{}")) { response = it }
        assertEquals("ERROR", state(requireNotNull(response)))
        assertEquals(0, service.calls)
    }

    @Test fun deadlineCompletesOnceAndIgnoresLateBackendSuccess() {
        val pending = TaskCompletionSource<Boolean>()
        val service = Service().apply { authorization = { pending.task } }
        val responses = mutableListOf<PaymentAuthorizationResult>()
        service.callbacks().authorize(PaymentData.fromJson(validPayment())) { responses.add(it) }
        assertTrue(responses.isEmpty())
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(25))
        assertEquals("ERROR", state(responses.single()))
        pending.setResult(true)
        assertEquals(1, responses.size)
    }

    @Test fun successfulShippingPreservesUpdatedSavedState() {
        val service = Service().apply { shipping = {
            Tasks.forResult(PaymentDataRequestUpdate.fromJson("""{"newTransactionInfo":{"totalPrice":"14.99"}}""")
                .withUpdatedSavedState(Bundle().apply { putString("reference", "order") }))
        } }
        var response: PaymentDataRequestUpdate? = null
        service.callbacks().change(IntermediatePaymentData.fromJson("{}")) { response = it }
        assertEquals("order", response!!.updatedSavedState!!.getString("reference"))
        assertTrue(response!!.toJson()!!.contains("14.99"))
    }

    @Test fun shippingExceptionAndMissingRequestReturnSheetError() {
        val service = Service().apply { shipping = { throw IllegalArgumentException("shipping") } }
        listOf(IntermediatePaymentData.fromJson("{}"), null).forEach { input ->
            var response: PaymentDataRequestUpdate? = null
            service.callbacks().change(input) { response = it }
            assertTrue(JsonParser.parseString(response!!.toJson()).asJsonObject.has("error"))
        }
    }
}
