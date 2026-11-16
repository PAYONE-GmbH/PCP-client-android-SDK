package com.payone.pcp_client_android_sdk.googlepay

import com.google.android.gms.wallet.PaymentDataRequest

/** Fixed-price checkout: delivery is selected by the shop; payment data returns to the Activity. */
class GooglePayStandard(config: GooglePayExpressConfig) {
    private val requests = GooglePayExpress(config)

    fun allowedPaymentMethodsJson(): String = requests.allowedPaymentMethodsJson()
    fun readinessRequest() = requests.readinessRequest()
    fun requestJson(finalTotal: String, transactionId: String): String =
        requests.buildRequest(finalTotal, transactionId, express = false)
    fun paymentDataRequest(finalTotal: String, transactionId: String): PaymentDataRequest =
        PaymentDataRequest.fromJson(requestJson(finalTotal, transactionId))
}
