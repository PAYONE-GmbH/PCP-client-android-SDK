# PCPClient SDK Android

https://img.shields.io/maven-central/v/io.github.payone-gmbh/pcp-client-android-sdk

[![Maven Central Repository](https://img.shields.io/maven-central/v/io.github.payone-gmbh/pcp-client-android-sdk)](https://img.shields.io/maven-central/v/io.github.payone-gmbh/pcp-client-android-sdk)
[![GitHub License](https://img.shields.io/github/license/PAYONE-GmbH/PCP-client-android-SDK)](https://github.com/PAYONE-GmbH/PCP-client-android-SDK/blob/main/LICENSE)

Welcome to the PAYONE Commerce Platform Client Android SDK for the PAYONE Commerce Platform. This SDK provides everything a client needs to easily complete payments using Credit or Debit Card, PAYONE Buy Now Pay Later (BNPL).

## Table of Contents

- [Supported Android API Version](#supported-android-api-version)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
    - [Creditcard Tokenizer](#creditcard-tokenizer)
        - [1. Add the HTML page](#1-add-the-html-page)
        - [2. Imports for CreditCardTokenizer](#2-imports-for-creditcardtokenizer)
        - [3. Configure the Tokenizer](#3-configure-the-tokenizer)
        - [4. Fetch the JWT Token from your Backend](#4-fetch-the-jwt-token-from-your-backend)
        - [5. Host and load the HTML page](#5-host-and-load-the-html-page)
        - [6. Initialize the Tokenizer](#6-initialize-the-tokenizer)
        - [7. Customization and Callbacks](#7-customization-and-callbacks)
        - [8. PCI DSS & Security](#8-pci-dss--security)
        - [9. Migration Note](#9-migration-note)
    - [Fingerprint Tokenizer](#fingerprint-tokenizer)
        - [1. Import for Fingerprint Tokenizer](#1-import-for-fingerprint-tokenizer)
        - [2. Create a new Fingerprint tokenizer instance](#2-create-a-new-fingerprint-tokenizer-instance)
        - [3. Get the snippet token](#3-get-the-snippet-token)
    - [Google Pay Integration](#google-pay-integration)
        - [Setup Google Pay Integration](#setup-google-pay-integration-compose-example)
            - [1. Install Dependencies](#1-install-dependencies)
            - [2. Create the Button and handle the payment](#2-create-the-button-and-handle-the-payment)
        - [Button Configuration Options](#button-configuration-options)
    - [PayPal Integration](#paypal-integration)
        - [1. Add Dependencies](#1-add-dependencies)
        - [2. Imports for PayPal Integration](#2-imports-for-paypal-integration)
        - [3. Configure and Launch PayPal Checkout](#3-configure-and-launch-paypal-checkout)
        - [4. Handle Authentication Completion](#4-handle-authentication-completion)
        - [5. UI Feedback](#5-ui-feedback)
- [Demonstration Projects](#demonstration-projects)
- [Contributing](#contributing)
- [Releasing the library](#releasing-the-library)
- [License](#license)

## Supported Android API Version

In order to use the SDK your minimum SDK Version needs to be at least API 34.

## Features

- **Creditcard Tokenizer**: Securely tokenize credit and debit card information.
- **Fingerprint Tokenizer**: Generate unique tokens for device fingerprinting.

## Installation

```kotlin
dependencies {
    implementation("io.github.payone-gmbh:pcp-client-android-sdk:1.3.0")
}
```

**[back to top](#table-of-contents)**

## Usage

### PayPal Integration

The SDK provides a simple integration for PayPal web payments using the official PayPal Android SDK. The example app demonstrates how to launch a PayPal checkout flow, handle authentication challenges, and display results in the UI.

#### 1. Add Dependencies

Add the following dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.paypal.android:payment-buttons:<latest-version>")
    implementation("com.paypal.android:paypal-web-payments:<latest-version>")
}
```

#### 2. Imports for PayPal Integration

```kotlin
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paymentbuttons.PayPalButton
import com.paypal.android.paypalwebpayments.*
```

#### 3. Configure and Launch PayPal Checkout

```kotlin
val config = CoreConfig(
    clientId,
    environment = Environment.SANDBOX // Use Environment.LIVE for production
)
val payPalWebCheckoutClient = PayPalWebCheckoutClient(this, config, "yourapp://return_url")

val payPalButton = findViewById<PayPalButton>(R.id.paypal_button)
payPalButton.setOnClickListener {
    val orderId = getPaypalExecutionIdFromServer()
    launchPayPalCheckout(orderId)
}

private fun launchPayPalCheckout(orderId: String) {
    val request = PayPalWebCheckoutRequest(
        orderId,
        fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
    )
    when (val result = payPalWebCheckoutClient.start(this, request)) {
        is PayPalPresentAuthChallengeResult.Success -> {
            // Show challenge to user, save authState
            authState = result.authState
        }
        is PayPalPresentAuthChallengeResult.Failure -> {
            // Show error in UI
        }
    }
}
```

#### 4. Handle Authentication Completion

```kotlin
override fun onResume() {
    super.onResume()
    checkForPayPalAuthCompletion(intent)
}

override fun onNewIntent(newIntent: Intent) {
    super.onNewIntent(newIntent)
    checkForPayPalAuthCompletion(newIntent)
}

fun checkForPayPalAuthCompletion(intent: Intent) = authState?.let { state ->
    when (val result = payPalWebCheckoutClient.finishStart(intent, state)) {
        is PayPalWebCheckoutFinishStartResult.Success -> {
            // Capture or authorize order on your server
            completeOrderOnServer()
            authState = null
        }
        is PayPalWebCheckoutFinishStartResult.Failure -> {
            // Show error in UI
            authState = null
        }
        is PayPalWebCheckoutFinishStartResult.Canceled -> {
            // Notify user checkout was canceled
            authState = null
        }
        PayPalWebCheckoutFinishStartResult.NoResult -> {
            // No result to process
            authState = null
        }
    }
}
```

#### 5. UI Feedback

- The example app displays all status and result messages in a `TextView` for clear user feedback.
- Replace placeholder values (`clientId`, `ORDER_ID`, etc.) with your own credentials and order IDs.


### Creditcard Tokenizer

The Credit Card Tokenizer now uses the new PAYONE Hosted Tokenization SDK. It securely collects and processes credit or debit card information in a PCI DSS-compliant way, returning a token for use in your server-side payment process.

To integrate the Credit Card Tokenizer feature into your Android application, follow these steps:

#### 1. Add the HTML page

Host your HTML page locally (for development) or on a server. The page must contain the payment IFrame and submit button:

```html
<div id="payment-IFrame"></div> <button id="submit">Submit</button>
```

For a more sophisticated example, see [creditcard-tokenizer-example.html](./app/src/main/assets/creditcard-tokenizer-example.html).

#### 2. Imports for CreditCardTokenizer

```kotlin
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
import com.payone.pcp_client_android_sdk.cctokenizer.IframeConfig
import com.payone.pcp_client_android_sdk.cctokenizer.UIConfig
import com.payone.pcp_client_android_sdk.cctokenizer.SubmitButtonConfig
```

#### 3. Configure the Tokenizer

```kotlin
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
    environment = "test", // Use "live" for production
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
```

#### 4. Fetch the JWT Token from your Backend

You must fetch the JWT from your backend before initializing the SDK.

```kotlin
val jwtToken = fetchJwtTokenFromBackend() // Implement this in your backend
```

#### 5. Host and load the HTML page

For local development, host your HTML file using a local server (e.g. `http-server` or similar). Use your computer's IP address or emulator's special address:

- Emulator: `http://10.0.2.2:8080/creditcard-tokenizer-example.html`
- Physical device: `http://<your-computer-ip>:8080/creditcard-tokenizer-example.html`

##### ⚠️ Local Development: Allowing HTTP traffic with network_security_config.xml

If you want to use the example app and host the HTML file locally over HTTP, you need to allow cleartext (HTTP) traffic in your Android app. This is only recommended for development and testing.

1. **Create network_security_config.xml**
   - Place the following file in `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain> <!-- Emulator -->
        <domain includeSubdomains="true">LOCAL_IP</domain> <!-- Your computer's local IP -->
    </domain-config>
</network-security-config>
```

2. **Reference it in your AndroidManifest.xml**
   - Add the attribute:

```xml
android:networkSecurityConfig="@xml/network_security_config"
```

- Example:

```xml
<application
    ...
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
    ...
</application>
```

3. **Host your HTML file locally**

   - Use a local server (e.g. `npx http-server`, `python3 -m http.server`, etc.)
   - For emulator: access via `http://10.0.2.2:8080/creditcard-tokenizer-example.html`
   - For physical device: use your computer's local IP, e.g. `http://192.168.1.100:8080/creditcard-tokenizer-example.html`

4. **Important: Remove network_security_config.xml for production**
   - Only use this config for development. In production, always use HTTPS and remove the `network_security_config.xml` reference from your manifest.

#### 6. Initialize the Tokenizer

```kotlin
val fragment = CreditcardTokenizerFragment.newInstance(
    config,
    jwtToken,
    "https://<your-server-address>/creditcard-tokenizer-example.html" // Use your actual server address
)
supportFragmentManager.beginTransaction().replace(R.id.fragment_container, fragment)
    .commit()
```

#### 7. Customization and Callbacks

- `iframe`: Configure the container and size for the payment iframe.
- `uiConfig`: Customize the look and feel of the form fields.
- `locale`: Set the language/locale for the form.
- `submitButton`: Provide a selector or element for the submit button.
- `tokenizationSuccessCallback`: Handle the token and card details on success.
- `tokenizationFailureCallback`: Handle errors on failure.
- `environment`: Choose "test" or "live" for the SDK environment.

#### 8. PCI DSS & Security

- The SDK uses a JWT from your backend for secure initialization.
- All card data is handled inside the iframe and never touches your application code.

#### 9. Migration Note

If you previously used the classic PAYONE Hosted IFrames, update your integration to use the new Hosted Tokenization SDK as shown above. The old `fields`, `defaultStyle`, and related config are no longer used.

**For more details, see the [demo project](./app) folder.**

**[back to top](#table-of-contents)**

### Fingerprint Tokenizer

To detect and prevent fraud at an early stage for the secured payment methods, the Fingerprint Tokenizer is an essential component for handling PAYONE Buy Now, Pay Later (BNPL) payment methods on the PAYONE Commerce Platform. During the checkout process, it securely collects three different IDs to generate a snippetToken in the format `<partner_id>_<merchant_id>_<session_id>`. This token must be sent from your server via the API parameter `paymentMethodSpecificInput.customerDevice.deviceToken`. Without this token, the server cannot perform the transaction. The tokenizer sends these IDs via a code snippet to Payla for later server-to-Payla authorization, ensuring the necessary device information is captured to facilitate secure and accurate payment processing.

To integrate the Fingerprint Tokenizer feature into your application, follow these steps:

#### 1. Import for Fingerprint Tokenizer

**Kotlin**

```kotlin
import com.payone.pcp_client_android_sdk.fingerprinttokenizer.FingerprintTokenizer
```

#### 2. Create a new Fingerprint Tokenizer instance

Create an instance with the payla partner ID, partner merchant ID and the environment (test or production).

<details>
  <summary>Example:</summary>

```kotlin
val fingerprintTokenizer = FingerprintTokenizer(
  context = this,
  paylaPartnerId = "YOUR_PARTNER_ID",
  partnerMerchantId = "YOUR_MERCHANT_ID",
  environment = PCPEnvironment.Test
)
```

</details>

#### 3. Get the snippet token

In order to retrieve the snippet token you call the following method:

<details>
  <summary>Example:</summary>

```kotlin
fingerprintTokenizer.getSnippetToken {
  if (it.isSuccess) {
    val token = it.getOrNull() ?: ""
    Log.d("Token: ", success)
  } else {
    val exception = it.exceptionOrNull()
    Log.d("Get Snippet token error: ", exception?.message ?: "")
  }
}
```

</details>

This snippet token is automatically generated when the `FingerprintTokenizer` instance is created and is also stored by Payla for payment verification. You need to send this snippet token to your server so that it can be included in the payment request. Add the token to the property `paymentMethodSpecificInput.customerDevice.deviceToken`.

For further information see: https://docs.payone.com/pcp/commerce-platform-payment-methods/payone-bnpl/payone-secured-invoice

### Google Pay Integration

The PAYONE Commerce Platform Client Android SDK provides a demonstration project for Google Pay integration. The integration uses the official Google Pay Button libraries which are available for various frameworks:

- Compose ([@google-pay/compose-pay-button](https://github.com/google-pay/compose-pay-button))

Our demo implementation uses this library.

#### Setup Google Pay Integration (Compose Example)

##### 1. **Install Dependencies**

Add the following dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-wallet:19.2.1")
    implementation("com.google.pay.button:compose-pay-button:1.2.0")
    // ...other dependencies
}
```

##### 2. **Create the Button and handle the payment**

In your Activity, set up the Google Pay button and handle the payment as shown below:

```kotlin
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson
import androidx.compose.ui.platform.ComposeView

class MainActivity : AppCompatActivity() {
    private lateinit var paymentsClient: PaymentsClient
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                val paymentData = data?.let { com.google.android.gms.wallet.PaymentData.getFromIntent(it) }
                if (paymentData != null) {
                    // TODO: process paymentData
                    Log.d("GooglePay", "Payment processed: \\${paymentData.toJson()}")
                } else {
                    Log.d("GooglePay", "Payment processed but paymentData is null")
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("GooglePay", "Payment canceled by user")
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = AutoResolveHelper.getStatusFromIntent(data)
                Log.e("GooglePay", "Payment error: \\${status?.statusMessage}")
            }
        }
    }
}
```

And the Compose Google Pay button:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.pay.button.ButtonTheme
import com.google.pay.button.PayButton
import com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson

@Composable
fun GooglePayButton(onClick: () -> Unit) {
    val allowedPaymentMethods = GooglePayRequestJson.allowedPaymentMethods
    Box(modifier = Modifier.fillMaxWidth()) {
        PayButton(
            onClick = onClick,
            allowedPaymentMethods = allowedPaymentMethods,
            theme = ButtonTheme.Dark
        )
    }
}
```

> **Note:** Replace `your-merchant-id` and `Your Merchant Name` with your actual merchant details. Make sure your layout contains a `ComposeView` with the ID `compose_google_pay`.

**[back to top](#table-of-contents)**

## Demonstration Projects

You can find a demonstration project for each language including all features in the corresponding directories:

- **Android**: Check out the [PCPClientAndroidDemo](./app) folder.

> [!IMPORTANT]
> Be aware that you will need to provide your own properties, for example AID, MID, PortalKey at all places which are prefixed with "YOUR_".

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md)

**[back to top](#table-of-contents)**

## Releasing the library

- Checkout develop branch.
- Do the required changes.
- Use the version script to update to new version, e.g:
  ```sh
  # from the root folder
  sh version.sh 1.2.3
  ```
- Create a pull-request into main branch.
- After merging the develop branch create a Git tag with the version.

**[back to top](#table-of-contents)**

## License

This project is licensed under the MIT License. For more details, see the [LICENSE](./LICENSE) file.

**[back to top](#table-of-contents)**
