package com.payone.pcpclientandroiddemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.gson.GsonBuilder
import com.payone.pcp_client_android_sdk.cctokenizer.CardNumberErrors
import com.payone.pcp_client_android_sdk.cctokenizer.CardholderNameErrors
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.ExpiryDateErrors
import com.payone.pcp_client_android_sdk.cctokenizer.IframeConfig
import com.payone.pcp_client_android_sdk.cctokenizer.LocaleTextAriaLabels
import com.payone.pcp_client_android_sdk.cctokenizer.LocaleTextConfig
import com.payone.pcp_client_android_sdk.cctokenizer.LocaleTextErrors
import com.payone.pcp_client_android_sdk.cctokenizer.LocaleTextLabels
import com.payone.pcp_client_android_sdk.cctokenizer.LocaleTextPlaceholders
import com.payone.pcp_client_android_sdk.cctokenizer.SecurityCodeErrors
import com.payone.pcp_client_android_sdk.cctokenizer.SubmitButtonConfig
import com.payone.pcp_client_android_sdk.cctokenizer.UIConfig
import kotlin.math.ceil

class CCTokenizerActivity : BaseActivity() {
    companion object {
        const val EXTRA_MERCHANT_TOKEN = "com.payone.pcpclientandroiddemo.MERCHANT_TOKEN"

        fun createIntent(context: Context, merchantToken: String? = null): Intent =
            Intent(context, CCTokenizerActivity::class.java).apply {
                merchantToken?.let { putExtra(EXTRA_MERCHANT_TOKEN, it) }
            }
    }

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

    private val token = ""

    private fun showOutput(message: String) {
        val output = findViewById<TextView>(R.id.tokenizerOutput)
        output.append("\n\n$message")
        val scroll = findViewById<NestedScrollView>(R.id.tokenizerOutputScroll)
        scroll.post {
            // fullScroll(FOCUS_DOWN) also moves input focus, interrupting the
            // WebView's numeric keyboard. Move only the output's viewport.
            scroll.scrollTo(0, output.bottom)
        }
    }

    private fun bindStatus(fragment: CreditcardTokenizerFragment) {
        fragment.onStatusChanged = { showOutput(it) }
    }

    private fun fitTokenizerContent(view: View) {
        val webView = view.findViewById<WebView>(com.payone.pcp_client_android_sdk.R.id.webView)
        val frame = findViewById<View>(R.id.fragment_container)
        webView.viewTreeObserver.addOnPreDrawListener {
            // The HTML includes its header, the hosted iframe AND the submit
            // button. Reserve space for the whole document before drawing logs.
            @Suppress("DEPRECATION")
            val documentHeight = ceil(webView.contentHeight * webView.scale.toDouble()).toInt()
            if (documentHeight > frame.height + resources.displayMetrics.density * 2) {
                frame.layoutParams = frame.layoutParams.apply { height = documentHeight }
                false
            } else {
                true
            }
        }
    }

    // Example: Custom text configuration with multiple locales
    private val customTextConfig = mapOf(
        "en" to LocaleTextConfig(
            labels = LocaleTextLabels(
                cardNumber = "Card Number",
                cardholderName = "Cardholder Name",
                expiryDate = "Expiry Date",
                securityCode = "Security Code"
            ),
            placeholders = LocaleTextPlaceholders(
                cardNumber = "1234 5678 9012 3456",
                cardholderName = "John Doe",
                expiryDate = "MM/YY",
                securityCode = "CVV"
            ),
            arialabels = LocaleTextAriaLabels(
                cardNumber = "Enter your card number",
                cardholderName = "Enter the name on the card",
                expiryDate = "Enter the expiration month and year of your card",
                securityCode = "Enter the card verification code"
            ),
            errors = LocaleTextErrors(
                cardNumber = CardNumberErrors(
                    isRequired = "Card number is required",
                    isInvalid = "Invalid card number",
                    isTooShort = "Card number is too short",
                    notSupported = "Card type not supported"
                ),
                cardholderName = CardholderNameErrors(
                    isRequired = "Cardholder name is required",
                    isInvalid = "Invalid cardholder name"
                ),
                expiryDate = ExpiryDateErrors(
                    isRequired = "Expiry date is required",
                    isInvalid = "Invalid expiry date"
                ),
                securityCode = SecurityCodeErrors(
                    isRequired = "Security code is required",
                    amexCardSecurityCodeError = "Invalid Amex security code",
                    generalSecurityCodeError = "Invalid security code"
                )
            )
        ),
        "de" to LocaleTextConfig(
            labels = LocaleTextLabels(
                cardNumber = "Kartennummer",
                cardholderName = "Karteninhaber",
                expiryDate = "Ablaufdatum",
                securityCode = "Sicherheitscode"
            ),
            placeholders = LocaleTextPlaceholders(
                cardNumber = "1234 5678 9012 3456",
                cardholderName = "Max Mustermann",
                expiryDate = "MM/JJ",
                securityCode = "CVV"
            ),
            arialabels = LocaleTextAriaLabels(
                cardNumber = "Geben Sie Ihre Kartennummer ein",
                cardholderName = "Geben Sie den Namen auf der Karte ein",
                expiryDate = "Geben Sie den Ablaufmonat und das Jahr Ihrer Karte ein",
                securityCode = "Geben Sie den Kartenprüfcode ein"
            ),
            errors = LocaleTextErrors(
                cardNumber = CardNumberErrors(
                    isRequired = "Kartennummer ist erforderlich",
                    isInvalid = "Ungültige Kartennummer",
                    isTooShort = "Kartennummer ist zu kurz",
                    notSupported = "Kartentyp nicht unterstützt"
                ),
                cardholderName = CardholderNameErrors(
                    isRequired = "Karteninhaber ist erforderlich",
                    isInvalid = "Ungültiger Karteninhaber"
                ),
                expiryDate = ExpiryDateErrors(
                    isRequired = "Ablaufdatum ist erforderlich",
                    isInvalid = "Ungültiges Ablaufdatum"
                ),
                securityCode = SecurityCodeErrors(
                    isRequired = "Sicherheitscode ist erforderlich",
                    amexCardSecurityCodeError = "Ungültiger Amex-Sicherheitscode",
                    generalSecurityCodeError = "Ungültiger Sicherheitscode"
                )
            )
        ),
        "fr" to LocaleTextConfig(
            labels = LocaleTextLabels(
                cardNumber = "Numéro de carte",
                cardholderName = "Nom du titulaire",
                expiryDate = "Date d'expiration",
                securityCode = "Code de sécurité"
            ),
            placeholders = LocaleTextPlaceholders(
                cardNumber = "1234 5678 9012 3456",
                cardholderName = "Jean Dupont",
                expiryDate = "MM/AA",
                securityCode = "CVV"
            )
        )
    )

    private val config = CreditcardTokenizerConfig(
        iframe = IframeConfig(
            iframeWrapperId = "payment-IFrame",
            height = 400,
            width = 400
        ),
        uiConfig = uiConfig,
        // switch to en_US or fr_FR to see other locales
        locale = "de_DE",
        submitButton = SubmitButtonConfig(
            selector = "#submit",
        ),
        mode = "test",
        token = token,
        customTextConfig = customTextConfig,
        tokenizationSuccessCallback = { statusCode, token, cardDetails, inputMode ->
            showOutput("Tokenization succeeded\n" + GsonBuilder().setPrettyPrinting().create().toJson(
                mapOf("statusCode" to statusCode, "token" to token,
                    "cardDetails" to cardDetails, "inputMode" to inputMode),
            ))
        },
        tokenizationFailureCallback = { statusCode, errorResponse ->
            showOutput("Tokenization failed ($statusCode)\n" +
                GsonBuilder().setPrettyPrinting().create().toJson(errorResponse))
        }
    )


    // Use your merchant-controlled HTTPS origin; the HTML itself is bundled in the app.
    private val localHTMLBaseURL = "https://your-merchant.example/checkout/"
    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "CC Tokenizer"
        super.onCreate(savedInstanceState)
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager, fragment: Fragment, view: View, savedInstanceState: Bundle?,
                ) {
                    if (fragment is CreditcardTokenizerFragment) fitTokenizerContent(view)
                }
            }, false,
        )
        setContentView(R.layout.activity_cc_tokenizer)
        (supportFragmentManager.findFragmentById(R.id.fragment_container) as? CreditcardTokenizerFragment)
            ?.let {
                it.setTokenizationCallbacks(config.tokenizationSuccessCallback, config.tokenizationFailureCallback)
                bindStatus(it)
            }
        // Tokens are runtime input, not Activity saved state.
        findViewById<EditText>(R.id.tokenizerMerchantToken).isSaveEnabled = false

        if (supportFragmentManager.backStackEntryCount > 0 || !isTaskRoot) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val btnStart = findViewById<Button>(R.id.btnStartCCTokenizer)
        btnStart.setOnClickListener {
            val merchantToken = findViewById<EditText>(R.id.tokenizerMerchantToken).text.toString().trim()
            if (merchantToken.isEmpty()) {
                showOutput("Enter a current merchant token from your backend first.")
                return@setOnClickListener
            }
            findViewById<TextView>(R.id.tokenizerOutput).text = "Loading bundled HTML…"
            val fragment = CreditcardTokenizerFragment.newInstanceWithAsset(
                config.copy(token = merchantToken),
                "creditcard-tokenizer-example.html",
                localHTMLBaseURL,
            )
            bindStatus(fragment)
            supportFragmentManager.beginTransaction().replace(
                com.payone.pcpclientandroiddemo.R.id.fragment_container,
                fragment
            ).commit()
        }

        val initialToken = intent.getStringExtra(EXTRA_MERCHANT_TOKEN)?.trim().orEmpty()
        if (savedInstanceState == null && initialToken.isNotEmpty()) {
            findViewById<EditText>(R.id.tokenizerMerchantToken).setText(initialToken)
            btnStart.performClick()
        }
    }

}
