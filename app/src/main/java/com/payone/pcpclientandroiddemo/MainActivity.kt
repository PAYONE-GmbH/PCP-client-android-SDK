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
import com.payone.pcp_client_android_sdk.cctokenizer.CCTokenizerRequest
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.Field
import com.payone.pcp_client_android_sdk.cctokenizer.PayoneLanguage
import com.payone.pcp_client_android_sdk.cctokenizer.SupportedCardType
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
            val fragment = CreditcardTokenizerFragment.newInstance(
                tokenizerUrl = "YOUR_URL",
                request = CCTokenizerRequest.create(
                    "YOUR_MID",
                    "YOUR_AID",
                    "YOUR_PORTAL_ID",
                    PCPEnvironment.Test,
                    "YOUR_PMI_PORTAL_KEY"
                ),
                supportedCardTypes = listOf(
                    SupportedCardType.Visa.identifier,
                    SupportedCardType.Mastercard.identifier
                ),
                config = CreditcardTokenizerConfig(
                    cardPan = Field(
                        selector = "cardpan",
                        style = "font-size: 14px; border: 1px solid #000;",
                        type = "input",
                        size = null,
                        maxlength = null,
                        length = null,
                        iframe = null
                    ),
                    cardCvc2 = Field(
                        selector = "cardcvc2",
                        style = "font-size: 14px; border: 1px solid #000;",
                        type = "password",
                        size = "4",
                        maxlength = "4",
                        length = mapOf("V" to 3, "M" to 3),
                        iframe = null
                    ),
                    cardExpireMonth = Field(
                        selector = "cardexpiremonth",
                        style = "font-size: 14px; width: 30px; border: solid 1px #000; height: 22px;",
                        type = "text",
                        size = "2",
                        maxlength = "2",
                        length = null,
                        iframe = "width" to "40px"
                    ),
                    cardExpireYear = Field(
                        selector = "cardexpireyear",
                        style = null,
                        type = "text",
                        size = null,
                        maxlength = null,
                        length = null,
                        iframe = "width" to "50px"
                    ),
                    defaultStyles = mapOf(
                        "input" to "font-size: 1em; border: 1px solid #000; width: 175px;",
                        "select" to "font-size: 1em; border: 1px solid #000;",
                        "iframe" to "height: 22px; width: 180px"
                    ),
                    language = PayoneLanguage.German,
                    error = "error",
                    submitButtonId = "submit",
                    creditCardCheckCallback = { result ->
                        if (result.isSuccess) {
                            // get response as CCTokenizerResponse
                            val response = result.getOrNull()
                        } else if (result.isFailure) {
                            val error = result.exceptionOrNull()
                        }
                    }
                )
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
