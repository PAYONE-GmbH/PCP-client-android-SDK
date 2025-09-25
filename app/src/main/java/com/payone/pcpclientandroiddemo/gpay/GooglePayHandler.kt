package com.payone.pcpclientandroiddemo.gpay


import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.wallet.*

class GooglePayHandler(
    private val activity: AppCompatActivity,
    private val loadPaymentDataRequestCode: Int
) {
    private val paymentsClient: PaymentsClient = Wallet.getPaymentsClient(
        activity,
        Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
            .build()
    )

    fun launchGooglePay() {
        val paymentDataRequestJson = GooglePayRequestJson.getRequestJson(
            merchantId = "your-merchant-id",
            merchantName = "Your Merchant Name",
            totalPrice = "100.00"
        )
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            activity,
            loadPaymentDataRequestCode
        )
    }

    fun handleGooglePayResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                val paymentData = data?.let { PaymentData.getFromIntent(it) }
                if (paymentData != null) {
                    Log.d("GooglePay", "Payment processed: ${paymentData.toJson()}")
                } else {
                    Log.d("GooglePay", "Payment processed but paymentData is null")
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("GooglePay", "Payment canceled by user")
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                Log.e("GooglePay", "Payment error: ${status?.statusMessage}")
            }
        }
    }
}
