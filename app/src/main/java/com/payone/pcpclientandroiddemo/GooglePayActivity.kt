package com.payone.pcpclientandroiddemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants

class GooglePayActivity : AppCompatActivity() {
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    private lateinit var paymentsClient: PaymentsClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_pay)

        paymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )

        val composeView = findViewById<ComposeView>(R.id.compose_google_pay)

        composeView.setContent {
            val startPayment = remember { mutableStateOf(false) }
            if (startPayment.value) {
                startPayment.value = false
                launchGooglePay()
            }
            GooglePayButton(onClick = { startPayment.value = true })
        }
    }

    private fun launchGooglePay() {
        val paymentDataRequestJson = com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson.getRequestJson(
            merchantId = "your-merchant-id",
            merchantName = "Your Merchant Name",
            totalPrice = "100.00"
        )
        val request = PaymentDataRequest.fromJson(paymentDataRequestJson)
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            this,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    private fun handleGooglePayResult(resultCode: Int, data: android.content.Intent?) {
        when (resultCode) {
            android.app.Activity.RESULT_OK -> {
                val paymentData = data?.let { PaymentData.getFromIntent(it) }
                if (paymentData != null) {
                    android.util.Log.d("GooglePay", "Payment processed: ${paymentData.toJson()}")
                } else {
                    android.util.Log.d("GooglePay", "Payment processed but paymentData is null")
                }
            }
            android.app.Activity.RESULT_CANCELED -> {
                android.util.Log.d("GooglePay", "Payment canceled by user")
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                android.util.Log.e("GooglePay", "Payment error: ${status?.statusMessage}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            handleGooglePayResult(resultCode, data)
        }
    }
}
