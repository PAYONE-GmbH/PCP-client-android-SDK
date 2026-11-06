package com.payone.pcpclientandroiddemo.gpay.express

import android.os.Bundle
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressService
import com.payone.pcpclientandroiddemo.gpay.checkout.CheckoutMode
import com.payone.pcpclientandroiddemo.gpay.checkout.CheckoutPricing
import com.payone.pcpclientandroiddemo.gpay.checkout.CheckoutSession
import com.payone.pcpclientandroiddemo.gpay.checkout.CheckoutTasks

class ExpressPaymentService : GooglePayExpressService() {
    override fun onShippingChanged(request: IntermediatePaymentData): Task<PaymentDataRequestUpdate> = CheckoutTasks.run {
        val session = CheckoutSession(this)
        check(session.mode() == CheckoutMode.EXPRESS && session.state() == "OPEN")
        PaymentDataRequestUpdate.fromJson(CheckoutPricing(session.config()).update(requireNotNull(request.toJson())))
            .withUpdatedSavedState(Bundle().apply { putString("reference", session.reference()) })
    }

    override fun onAuthorize(payment: GooglePayExpressPayment, savedState: Bundle?): Task<Boolean> = CheckoutTasks.run {
        val session = CheckoutSession(this)
        check(session.mode() == CheckoutMode.EXPRESS && savedState?.getString("reference") == session.reference())
        session.submit(payment)
    }
}
