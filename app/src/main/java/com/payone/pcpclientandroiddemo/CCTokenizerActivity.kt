package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.Button
import com.payone.pcp_client_android_sdk.cctokenizer.CardNumberErrors
import com.payone.pcp_client_android_sdk.cctokenizer.CardholderNameErrors
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.CustomIconsConfig
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
        customIconsConfig = CustomIconsConfig(
            useCustomValidationIcons = true,
            showCardBrandIcons = false,
            successIcon = "/validIcon.svg",
            errorIcon = "/invalidIcon.svg"
        ),
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
