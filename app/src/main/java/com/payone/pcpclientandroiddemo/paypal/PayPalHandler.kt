package com.payone.pcpclientandroiddemo.paypal

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest


class PayPalHandler(private val context: AppCompatActivity) {
    lateinit var payPalWebCheckoutClient: PayPalWebCheckoutClient

    fun initialize(clientId: String, returnUrl: String) {
        val config = CoreConfig(clientId, environment = Environment.SANDBOX)
        payPalWebCheckoutClient = PayPalWebCheckoutClient(context, config, returnUrl)

    }

    fun startPayment(
        orderId: String,

        ) {
        val payPalWebCheckoutRequest = PayPalWebCheckoutRequest(
            "ORDER_ID",
            fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        )
        payPalWebCheckoutClient.start(this.context, payPalWebCheckoutRequest)


    }


}
