package com.payone.pcpclientandroiddemo.cctokenizer

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.payone.pcp_client_android_sdk.cctokenizer.*
import com.payone.pcpclientandroiddemo.R

class CCTokenizerHandler(
    private val fragmentManager: FragmentManager
) {
    fun startTokenization() {
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
        val jwtToken = "<Token to be retrieved from the CommercePlatform-API>"
        val tokenizerHtmlUrl = "https://path-to-your-server/creditcard-tokenizer.html"
        val fragment = CreditcardTokenizerFragment.newInstance(
            config,
            jwtToken,
            tokenizerHtmlUrl
        )
        fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
    }
}
