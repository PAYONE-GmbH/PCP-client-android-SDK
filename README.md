# PCPClient SDK Android

Welcome to the PAYONE Commerce Platform Client Android SDK for the PAYONE Commerce Platform. This SDK provides everything a client needs to easily complete payments using Credit or Debit Card, PAYONE Buy Now Pay Later (BNPL).

## Table of Contents

- [Supported Android API Version](#supported-android-api-version)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
  - [Creditcard Tokenizer](#creditcard-tokenizer)
    - [1. Upload an HTML page](#1-upload-an-html-page)
    - [2. Imports for CreditCardTokenizer](#2-imports-for-creditcardtokenizer)
    - [3. Create a CCTokenizerFragment instance](#3-create-a-cctokenizerfragment-instance)
  - [Fingerprint Tokenizer](#fingerprint-tokenizer)
    - [1. Import for Fingerprint Tokenizer](#1-import-for-fingerprint-tokenizer)
    - [2. Create a new Fingerprint tokenizer instance](#2-create-a-new-fingerprint-tokenizer-instance)
    - [3. Get the snippet token](#3-get-the-snippet-token)
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

```

## Usage

### Creditcard Tokenizer

The Credit Card Tokenizer is an essential component for handling payments on the PAYONE Commerce Platform. It securely collects and processes credit or debit card information to generate a `paymentProcessingToken`, which is required for the Server-SDK to complete the payment process. Without this token, the server cannot perform the transaction. The tokenizer ensures that sensitive card details are handled securely and is PCI DSS (Payment Card Industry Data Security Standard) compliant.

To integrate the Creditcard Tokenizer feature into your application, follow these steps:

#### 1. Upload an HTML page

The Creditcard tokenizer injects code and PCI DSS conform input fields into a webpage. To assure this process works, you need to setup the correct containers and submit button.

```html
  <div id="cardpanInput"></div>
  <div id="cardcvc2Input"></div>
  <div id="cardExpireMonthInput"></div>
  <div id="cardExpireYearInput"></div>
  <button id="submit">Submit</button>
  ```

For a more sophisticated example, see this [creditcard-tokenizer-example.html](./creditcard-tokenizer-example.html).

#### 2. Imports for CreditCardTokenizer

**Kotlin**
```kotlin
import com.payone.pcp_client_android_sdk.cctokenizer.CCTokenizerFragment
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
```

#### 3. Create a CCTokenizerFragment instance

##### Kotlin

Use the `CCTokenizerFragment.newInstance()` method.

```kotlin
CCTokenizerFragment.newInstance(
  tokenizerUrl: String,
  request: CCTokenizerRequest,
  supportedCardTypes: List<String>,
  config: CreditcardTokenizerConfig
)
```

##### Tokenizer URL

This is the URL where your HTML code can be found and this should include a valid HTML with the later specified fields and submit button.

<details>
  <summary>Example:</summary>

```kotlin
tokenizerUrl = "https://github.com"
```
</details>

> [!CAUTION]  
> Do not use local HTML since this won't work. The scripts that are used need a valid origin to send updates to, Therefore, it's currently a limitation that the HTML must be hosted.

##### Request

The `CCTokenizerRequest` object includes several configuration keys and settings. These are your AID, MID, Portal ID, PMI Portal Key and lastly the environment to run your code against (test or production).

<details>
  <summary>Example:</summary>
  
```kotlin
CCTokenizerRequest(
  mid: "123",
  aid: "456",
  portalId: "789",
  environment: PCPEnvironment,
  pmiPortalKey: "a1b2"
)
```
</details>

##### Supported Card Types

A `List<String>` of supported card types. You should use the `SupportedCardType` enum and it's identifier property to receive valid values.

<details>
  <summary>Example:</summary>

```kotlin
listOf(SupportedCardType.Visa.identifier, SupportedCardType.Mastercard.identifier)
```
</details>

##### Config

The config including the different required [fields](#fields), the callback(s), certain CSS styles, submit button ID, and the used language.

<details>
  <summary>Example:</summary>
  
```kotlin
data class CreditcardTokenizerConfig(
    val cardPan: Field,
    val cardCvc2: Field,
    val cardExpireMonth: Field,
    val cardExpireYear: Field,
    val defaultStyles: Map<String, String>,
    val language: PayoneLanguage,
    val error: String,
    val submitButtonId: String,
    val creditCardCheckCallback: (Result<CCTokenizerResponse>) -> Unit
) : Serializable

data class Field(
    val selector: String,
    val style: String?,
    val type: String,
    val size: String?,
    val maxlength: String?,
    val length: Map<String, Int>,
    val iframe: Pair<String, String>
)

enum class PayoneLanguage(val configValue: String) {
    English("Payone.ClientApi.Language.en"),
    German("Payone.ClientApi.Language.de")
}
```
</details>

##### Fields

Defines the various input fields for credit card details.

| Property          | Type                             | Description                                        |
| ----------------- | -------------------------------- | -------------------------------------------------- |
| `cardpan`         | `Field`                    | Configuration for the card number field.           |
| `cardcvc2`        | `Field`                    | Configuration for the card CVC2 field.             |
| `cardexpiremonth` | `Field`                    | Configuration for the card expiration month field. |
| `cardexpireyear`  | `Field`                    | Configuration for the card expiration year field.  |

##### Field properties

- **selector**: `String`  
  The CSS selector for the input element.

- **element**: `String` (optional)  
  The actual DOM element if not using a selector.

- **size**: `String` (optional)  
  The size attribute for the input element.

- **maxlength**: `String` (optional)  
  The maximum length of input allowed.

- **length**: `Map<String, Int>` 
  Specifies the length for various card types (e.g., `mapOf(V to 3, M to 3, A to 4, J to 0 )`).

- **type**: `String`  
  The type attribute for the input element (e.g., `text`, `password`).

- **style**: `String` (optional)  
  CSS styles applied to the input element.

- **iframe**: `Pair<String: String>` 
  Dimensions for the iframe if used (pass only width and height properties).

##### Other configurations fields

| Property                           | Type                                                                                                                                                       | Description                                                                                                                                            |
| ---------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `language`                         | `PayoneLanguage`                                                                                                                                                   | The language for the SDK (`.German` or `.English` for now).                                                                                                               |
| `submitButtonId`                     | `String`                                                                                                                                       | HTML ID of the  submit button.                                                                                                                 
| `error`                            | `String`                                                                                                                                        | HTML ID of the div-container where error messages should be displayed.                                                                                |
| `creditCardCheckCallback`          | `Result<CCTokenizerResponse>) -> Unit`  | Callback function for credit card check responses                                                                                                       |
| `success`          | `(CCTokenizerResponse) -> Unit` | Callback function for credit card check success.                                                                                                     |
| `error`          | `(CCTokenizerError) -> Unit`  | Callback function for credit card check failure.                                                                                                     |

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

**[back to top](#table-of-contents)**

## Demonstration Projects

You can find a demonstration project for each language including all features in the corresponding directories:

- **Android**: Check out the [PCPClientAndroidDemo](./app) folder.

> [!IMPORTANT]
>Be aware that you will need to provide your own properties, for example AID, MID, PortalKey at all places which are prefixed with "YOUR_".

## Contributing

See [CONTRIBUTING.md](./CONTRIBUTING.md)

**[back to top](#table-of-contents)**

## Releasing the library

- Checkout develop branch.
- Do the required changes.
- Create a pull-request into main branch.
- After merging the develop branch create a Git tag with the version.

**[back to top](#table-of-contents)**

## License

This project is licensed under the MIT License. For more details, see the [LICENSE](./LICENSE) file.

**[back to top](#table-of-contents)**






