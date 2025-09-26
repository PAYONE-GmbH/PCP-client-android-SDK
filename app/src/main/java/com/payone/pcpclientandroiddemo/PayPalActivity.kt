package com.payone.pcpclientandroiddemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import androidx.core.content.ContentProviderCompat.requireContext

class PayPalActivity : BaseActivity() {

    lateinit var payPalWebCheckoutClient: PayPalWebCheckoutClient
    var authState: String? = null
    var clientId: String =  "CLIENT_ID"

    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "Paypal"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paypal)

        statusTextView = findViewById(R.id.paypal_status)

        val config = CoreConfig(
            clientId,
            environment = Environment.SANDBOX
        )
        payPalWebCheckoutClient = PayPalWebCheckoutClient(this, config, "pcpdemo")

        val payPalButton = findViewById<PayPalButton>(R.id.paypal_button)
        payPalButton.setOnClickListener {
            val id = this.getPaypalExecutionIdFromServer()
            this.launchPayPalCheckout(orderId = id)
        }
    }

    override fun onResume() {
        super.onResume()
        checkForPayPalAuthCompletion(intent)
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        checkForPayPalAuthCompletion(newIntent)
    }

    private fun getPaypalExecutionIdFromServer(): String{
        // create an order and parse the payPalExecutionId and return it
        return "ORDER_ID"
    }

    private fun completeOrderOnServer(){
        // make api call on complete order endpoint
        return
    }

    private fun launchPayPalCheckout(orderId: String) {
        val payPalWebCheckoutRequest = PayPalWebCheckoutRequest(
            orderId,
            fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        )

        when (val result = payPalWebCheckoutClient.start(this, payPalWebCheckoutRequest)) {
            is PayPalPresentAuthChallengeResult.Success -> {
                Log.d("PayPayActivity", "PayPalPresentAuthChallengeResult.Success")
                statusTextView.text = "Present auth challenge to user."
                authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                  Log.d("PayPayActivity", "PayPalPresentAuthChallengeResult.Failure")
                statusTextView.text = "Failed to present auth challenge: ${result.error}"
            }
        }
    }

    fun checkForPayPalAuthCompletion(intent: Intent) = authState?.let { state ->
    Log.d("PayPalActivity", "Checking for PayPal auth completion...")
        // check for checkout completion
        when (val checkoutResult = payPalWebCheckoutClient.finishStart(intent, state)) {
            is PayPalWebCheckoutFinishStartResult.Success -> {
                statusTextView.text = "Capture or authorize order on your server."
                this.completeOrderOnServer()
                authState = null
            }
            is PayPalWebCheckoutFinishStartResult.Failure -> {
                statusTextView.text = "Handle approve order failure."
                authState = null
            }
            is PayPalWebCheckoutFinishStartResult.Canceled -> {
                statusTextView.text = "PayPal checkout was canceled."
                authState = null
            }
            PayPalWebCheckoutFinishStartResult.NoResult -> {
                statusTextView.text = "No PayPal checkout result to process."
                authState = null
            }
        }

      
    }

}
