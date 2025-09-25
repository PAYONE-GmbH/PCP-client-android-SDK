package com.payone.pcpclientandroiddemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.payone.pcpclientandroiddemo.util.Header

class PayPalActivity : AppCompatActivity() {

    lateinit var payPalWebCheckoutClient: PayPalWebCheckoutClient
    var authState: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paypal)

        Header(this, "Paypal").setup()


        val config = CoreConfig(
            "AUn5n-4qxBUkdzQBv6f8yd8F4AWdEvV6nLzbAifDILhKGCjOS62qQLiKbUbpIKH_O2Z3OL8CvX7ucZfh",
            environment = Environment.SANDBOX
        )
        payPalWebCheckoutClient = PayPalWebCheckoutClient(this, config, "myapp://return_url")

        val payPalButton = findViewById<PayPalButton>(R.id.paypal_button)
        payPalButton.setOnClickListener {
            val id = "2YC501593T898163N"
            this.launchPayPalCheckout(orderId = id)
        }
    }

    override fun onResume() {
        super.onResume()
        // Manually attempt auth challenge completion (via host activity intent deep link)
        checkForPayPalAuthCompletion(intent)
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        // Manually attempt auth challenge completion (via new intent deep link)
        checkForPayPalAuthCompletion(newIntent)
    }

    private fun launchPayPalCheckout(orderId: String) {
        val payPalWebCheckoutRequest = PayPalWebCheckoutRequest(
            orderId,
            fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
        )

        when (val result = payPalWebCheckoutClient.start(this, payPalWebCheckoutRequest)) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // Capture auth state for balancing call to finishStart() when
                // the merchant application re-enters the foreground
                Log.d("PayPalActivity", "Present auth challenge to user.")
                authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                Log.d("PayPalActivity", "Failed to present auth challenge: ${result.error}")
            }
        }
    }

    fun checkForPayPalAuthCompletion(intent: Intent) = authState?.let { state ->
        Log.d("PayPalActivity", "Checking for PayPal auth completion...")
        // check for checkout completion
        when (val checkoutResult = payPalWebCheckoutClient.finishStart(intent, state)) {
            is PayPalWebCheckoutFinishStartResult.Success -> {
                Log.d("PayPalActivity", "Capture or authorize order on your server.")
                authState = null
            }

            is PayPalWebCheckoutFinishStartResult.Failure -> {
                Log.d("PayPalActivity", "Handle approve order failure.")
                authState = null
            }

            is PayPalWebCheckoutFinishStartResult.Canceled -> {
                Log.d("PayPalActivity", "Notify user PayPal checkout was canceled.")
                authState = null
            }

            PayPalWebCheckoutFinishStartResult.NoResult -> {
                // there isn't enough information to determine the state of the auth challenge for this payment method
                Log.d("PayPalActivity", "No PayPal checkout result to process.")
                authState = null
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = Header.onNavigateUp(this)
}
