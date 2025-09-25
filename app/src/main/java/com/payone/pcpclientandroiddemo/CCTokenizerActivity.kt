package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.IframeConfig
import com.payone.pcp_client_android_sdk.cctokenizer.SubmitButtonConfig
import com.payone.pcp_client_android_sdk.cctokenizer.UIConfig
import com.payone.pcpclientandroiddemo.util.Header

class CCTokenizerActivity : AppCompatActivity() {
    private val uiConfig = UIConfig(
        formBgColor = "#64bbb7",
        fieldBgColor = "wheat",
        fieldBorder = "1px solid #b33cd8",
        fieldOutline = "#101010 solid 5px",
        fieldLabelColor = "#d3d83c",
        fieldPlaceholderColor = "blue",
        fieldTextColor = "crimson",
        fieldErrorCodeColor = "green"
    )

    private val config = CreditcardTokenizerConfig(
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
            android.util.Log.d("CC Success", "Tokenized card successfully")
            android.util.Log.d("CC Success", "Status: $statusCode")
            android.util.Log.d("CC Success", "Token: $token")
            android.util.Log.d("CC Success", "Card Details: $cardDetails")
        },
        tokenizationFailureCallback = { statusCode, errorResponse ->
            android.util.Log.e("CC Failure", "Tokenization of card failed")
            android.util.Log.e("CC Failure", "Status: $statusCode")
            android.util.Log.e("CC Failure", "Error: ${errorResponse["error"]}")
        }
    )

    private val jwtToken = "<Token to be retrieved from the CommercePlatform-API>"
    private val tokenizerHtmlUrl = "https://path-to-your-server/creditcard-tokenizer.html"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cc_tokenizer)

        Header(this, "CC Tokenizer").setup()


        if (supportFragmentManager.backStackEntryCount > 0 || !isTaskRoot) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val btnStart = findViewById<Button>(R.id.btnStartCCTokenizer)
        btnStart.setOnClickListener {
            val fragment = CreditcardTokenizerFragment.newInstance(
                config,
                jwtToken,
                tokenizerHtmlUrl
            )
            supportFragmentManager.beginTransaction().replace(
                com.payone.pcpclientandroiddemo.R.id.fragment_container,
                fragment
            ).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean = Header.onNavigateUp(this)
}
