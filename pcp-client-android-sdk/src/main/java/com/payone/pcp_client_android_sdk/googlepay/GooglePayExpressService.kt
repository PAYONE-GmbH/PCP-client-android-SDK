package com.payone.pcp_client_android_sdk.googlepay

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacksService
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentAuthorizationResult
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Register a concrete subclass using Google's signature-protected callback service intent.
 * Handlers must start work asynchronously, use backend-authoritative totals and be idempotent.
 * Google may recreate this service between callbacks: use saved state/backend state, never Activity state.
 */
abstract class GooglePayExpressService : BasePaymentDataCallbacksService() {
    abstract fun onShippingChanged(request: IntermediatePaymentData): Task<PaymentDataRequestUpdate>

    /** Return true ONLY after the backend confirms authorization/capture. */
    abstract fun onAuthorize(payment: GooglePayExpressPayment, savedState: Bundle?): Task<Boolean>

    final override fun createPaymentDataCallbacks(): BasePaymentDataCallbacks = object : BasePaymentDataCallbacks() {
        override fun onPaymentDataChanged(request: IntermediatePaymentData?, listener: OnCompleteListener<PaymentDataRequestUpdate>) {
            completeWithinDeadline(listener, { shippingError() }) {
                onShippingChanged(requireNotNull(request))
            }
        }

        override fun onPaymentAuthorized(request: PaymentData?, listener: OnCompleteListener<PaymentAuthorizationResult>) {
            completeWithinDeadline(listener, { authorizationResult(false) }) {
                val data = requireNotNull(request)
                onAuthorize(GooglePayExpressPayment.fromJson(requireNotNull(data.toJson())), data.lastSavedState)
                    .continueWith(Executor { it.run() }) { task ->
                        authorizationResult(task.isSuccessful && task.result == true)
                    }
            }
        }
    }

    private fun <T : Any> completeWithinDeadline(listener: OnCompleteListener<T>, failure: () -> T, start: () -> Task<T>) {
        val handler = Handler(Looper.getMainLooper())
        val completed = AtomicBoolean()
        fun complete(value: T) {
            if (completed.compareAndSet(false, true)) listener.complete(value)
        }
        val timeout = Runnable { complete(failure()) }
        handler.postDelayed(timeout, 25_000)
        try {
            start().addOnCompleteListener(Executor { it.run() }) { task ->
                handler.removeCallbacks(timeout)
                complete(if (task.isSuccessful) task.result else failure())
            }
        } catch (_: Exception) {
            handler.removeCallbacks(timeout)
            complete(failure())
        }
    }

    companion object {
        fun authorizationResult(authorized: Boolean): PaymentAuthorizationResult = PaymentAuthorizationResult.fromJson(
            if (authorized) """{"transactionState":"SUCCESS"}"""
            else """{"transactionState":"ERROR","error":{"intent":"PAYMENT_AUTHORIZATION","reason":"OTHER_ERROR","message":"Payment could not be confirmed. Please check your order before retrying."}}""",
        )

        private fun shippingError(): PaymentDataRequestUpdate = PaymentDataRequestUpdate.fromJson(
            """{"error":{"intent":"SHIPPING_ADDRESS","reason":"OTHER_ERROR","message":"Shipping could not be updated. Please try again."}}""",
        )
    }
}
