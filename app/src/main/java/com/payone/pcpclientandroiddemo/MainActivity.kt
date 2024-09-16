package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.payone.pcp_client_android_sdk.cctokenizer.CCTokenizerRequest
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.Field
import com.payone.pcp_client_android_sdk.cctokenizer.PayoneLanguage
import com.payone.pcp_client_android_sdk.cctokenizer.SupportedCardType
import com.payone.pcp_client_android_sdk.fingerprinttokenizer.FingerprintTokenizer
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment

class MainActivity : AppCompatActivity() {

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
                tokenizerUrl = "https://djirlic.com/paytestapp.html",
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


    }
}