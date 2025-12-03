package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.Button
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.IframeConfig
import com.payone.pcp_client_android_sdk.cctokenizer.SubmitButtonConfig
import com.payone.pcp_client_android_sdk.cctokenizer.UIConfig

class CCTokenizerActivity : BaseActivity() {
    private val uiConfig = UIConfig(
        formBgColor = "#446462ff",
        fieldBgColor = "wheat",
        fieldBorder = "1px solid #b33cd8",
        fieldOutline = "#101010 solid 5px",
        fieldLabelColor = "#d3d83c",
        fieldPlaceholderColor = "blue",
        fieldTextColor = "crimson",
        fieldErrorCodeColor = "green"
    )

    private val token = "<Token to be retrieved from the CommercePlatform-API>"

    private val config = CreditcardTokenizerConfig(
        iframe = IframeConfig(
            iframeWrapperId = "payment-IFrame",
            width = 400
        ),
        uiConfig = uiConfig,
        locale = "de_DE",
        submitButton = SubmitButtonConfig(
            selector = "#submit",
        ),
        mode = "test",
        token = token,
        tokenizationSuccessCallback = { statusCode, token, cardDetails, inputMode ->
            android.util.Log.d("CC Success", "Tokenized card successfully")
            android.util.Log.d("CC Success", "Status: $statusCode")
            android.util.Log.d("CC Success", "Token: $token")
            android.util.Log.d("CC Success", "Card Details: $cardDetails")
            android.util.Log.d("CC Success", "InputMode: $inputMode")

        },
        tokenizationFailureCallback = { statusCode, errorResponse ->
            android.util.Log.e("CC Failure", "Tokenization of card failed")
            android.util.Log.e("CC Failure", "Status: $statusCode")
            android.util.Log.e("CC Failure", "Error: ${errorResponse["error"]}")
        }
    )


    private val tokenizerHtmlUrl = "https://path-to-your-server/creditcard-tokenizer.html"
    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "CC Tokenizer"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cc_tokenizer)

        if (supportFragmentManager.backStackEntryCount > 0 || !isTaskRoot) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val btnStart = findViewById<Button>(R.id.btnStartCCTokenizer)
        btnStart.setOnClickListener {
            val fragment = CreditcardTokenizerFragment.newInstance(
                config,
                tokenizerHtmlUrl
            )
            supportFragmentManager.beginTransaction().replace(
                com.payone.pcpclientandroiddemo.R.id.fragment_container,
                fragment
            ).commit()
        }
    }

}
