package com.payone.pcpclientandroiddemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.PaymentDataRequest
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.IframeConfig
import com.payone.pcp_client_android_sdk.cctokenizer.SubmitButtonConfig
import com.payone.pcp_client_android_sdk.cctokenizer.UIConfig
import com.payone.pcp_client_android_sdk.fingerprinttokenizer.FingerprintTokenizer
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
import com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson

class MainActivity : AppCompatActivity() {
    private lateinit var paymentsClient: PaymentsClient
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fingerprintBtn: Button = findViewById(R.id.fingerprint_button)
        val tvSnippetToken: TextView = findViewById(R.id.tvSnippetToken)
        val ccTokenizerBtn: Button = findViewById(R.id.btnCCTokenizer)

        val fingerprintTokenizer = FingerprintTokenizer(
            context = this,
            paylaPartnerId = "e7yeryF2of8X",
            partnerMerchantId = "test-1",
            environment = PCPEnvironment.Test
        )

        fingerprintBtn.setOnClickListener {
            fingerprintTokenizer.getSnippetToken {
                if (it.isSuccess) {
                    val token = it.getOrNull() ?: ""
                    Log.d("Success Token", token)
                    tvSnippetToken.text = token
                } else {
                    val exception = it.exceptionOrNull()
                    Log.d("Failure Token", exception?.message ?: "unknown")
                    tvSnippetToken.text = exception?.message
                }
            }
        }

        ccTokenizerBtn.setOnClickListener {
            // Example UI config for the new SDK
            val uiConfig = UIConfig(
                formBgColor = "#64bbb7",
                fieldBgColor = "wheat",
                fieldBorder = "1px solid #b33cd8",
                fieldOutline = "#101010 solid 5px",
                fieldLabelColor = "#d3d83c",
                fieldPlaceholderColor = "blue",
                fieldTextColor = "crimson",
                fieldErrorCodeColor = "green"
            )
            val config = CreditcardTokenizerConfig(
                iframe = IframeConfig(
                    iframeWrapperId = "payment-IFrame",
                    height = 400,
                    width = 400
                ),
                uiConfig = uiConfig,
                locale = "de_DE",
                submitButton = SubmitButtonConfig(
                    selector = "#submit",
                    element = null
                ),
                environment = "test",
                tokenizationSuccessCallback = { statusCode, token, cardDetails ->
                    Log.d("CC Success", "Tokenized card successfully")
                    Log.d("CC Success", "Status: $statusCode")
                    Log.d("CC Success", "Token: $token")
                    Log.d("CC Success", "Card Details: $cardDetails")
                },
                tokenizationFailureCallback = { statusCode, errorResponse ->
                    Log.e("CC Failure", "Tokenization of card failed")
                    Log.e("CC Failure", "Status: $statusCode")
                    Log.e("CC Failure", "Error: ${errorResponse["error"]}")
                }
            )
            val jwtToken = "<Token to be retrieved from the CommercePlatform-API>" // Fetch this from your backend
            var tokenizerHtmlUrl = "https://path-to-your-server/creditcard-tokenizer.html" // Use your hosted HTML page URL
            val fragment = CreditcardTokenizerFragment.newInstance(
                config,
                jwtToken,
                tokenizerHtmlUrl
            )
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
                .commit()
        }

        // Set up Google Pay PaymentsClient
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
        val paymentDataRequestJson = GooglePayRequestJson.getRequestJson(
            merchantId = "your-merchant-id",
            merchantName = "Your Merchant Name",
            totalPrice = "100.00"
        )
        val request = com.google.android.gms.wallet.PaymentDataRequest.fromJson(paymentDataRequestJson)
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            this,
            LOAD_PAYMENT_DATA_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            handleGooglePayResult(resultCode, data)
        }
    }

    private fun handleGooglePayResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                // Handle successful payment here
                val paymentData = data?.let { com.google.android.gms.wallet.PaymentData.getFromIntent(it) }
                if (paymentData != null) {
                    // TODO: process paymentData
                    Log.d("GooglePay", "Payment processed: ${paymentData.toJson()}")
                } else {
                    Log.d("GooglePay", "Payment processed but paymentData is null")
                }
            }
            Activity.RESULT_CANCELED -> {
                // Payment canceled
                Log.d("GooglePay", "Payment canceled by user")
            }
            AutoResolveHelper.RESULT_ERROR -> {
                // Handle error
                val status = AutoResolveHelper.getStatusFromIntent(data)
                Log.e("GooglePay", "Payment error: ${status?.statusMessage}")
            }
        }
    }
}
