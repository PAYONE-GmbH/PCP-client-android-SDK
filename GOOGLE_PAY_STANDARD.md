# Google Pay Standard Checkout (native Android)

This guide documents the native **Standard Checkout** integration in the SDK and demo. The shopper selects delivery on the shop page, confirms the Google Pay sheet and then the app submits the returned payment data to the backend. A PCP checkout is created only with validated Google Pay data, using PAYONE payment product **320**.

For delivery selection and dynamic price updates inside the sheet, see [Google Pay Express Checkout](GOOGLE_PAY_EXPRESS.md).

## 1. Standard Checkout flow

1. Open **Google Pay Standard Checkout** in the demo (`GooglePayActivity`).
2. The app reads the PAYONE merchant ID from the configured backend and checks Google Pay readiness.
3. Select delivery on the page. The default gross basket is €10.00; Standard delivery adds €0.00 and Express delivery adds €4.99.
4. Tap the official Google Pay button. Persist a fresh reference, selected delivery and configuration before launching the sheet.
5. Google Pay displays a **FINAL** total of €10.00 or €14.99 and collects billing/shipping address, phone and email.
6. On a successful Google Pay result, validate the payment data and combine it with the delivery option selected on the shop page.
7. Submit once to `POST /api/commerce-cases`, including the Google Pay token and product 320.
8. Display the payment result and returned commerce-case/checkout IDs.

Opening the page, checking readiness, changing delivery or cancelling the sheet before submission does not create a checkout. Standard does **not** use Express shipping/authorization callbacks or select delivery inside the sheet.

The demo defaults to `executePaymentOnCreation=false`. It supplies the Google Pay payment input to PCP while deferring immediate payment execution.

## 2. Prerequisites and dependencies

- Android minSdk 24, Google Play services and a Google Pay-capable device/account. Render the button only after readiness succeeds.
- PAYONE Google Pay/product 320 and the intended card networks must be configured for the merchant to execute payments.
- Gateway: `payonegmbh`. `gatewayMerchantId` is the PAYONE merchant ID, not a Google web merchant ID.
- The default API base URL is `http://localhost:3009/api/`. `GET /ui/config` at the same origin supplies the merchant ID (currently `P1_18352_2013244`); server credentials stay on the server. Creation uses the relative endpoint `commerce-cases`, resulting in `/api/commerce-cases` for this default. `GET /api/` may return 404 even when those routes work.
- Google `ENVIRONMENT_TEST` and the PAYONE backend environment are independent. Production additionally requires Google production access for the Android package/signing certificate and the appropriate PAYONE setup.

The SDK exposes Wallet 20 as an API dependency; the demo also uses the official Compose button:

```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-wallet:20.0.0")
    implementation("com.google.pay.button:compose-pay-button:1.1.0")
}
```

The app manifest needs Internet access and Wallet enablement (already configured in the demo):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<application>
    <meta-data
        android:name="com.google.android.gms.wallet.api.enabled"
        android:value="true" />
</application>
```

Standard Checkout does not require the Express callback service. The demo registers that service for its separate Express page.

## 3. Implementation modules

| Module | Responsibility |
| --- | --- |
| [GooglePayActivity](app/src/main/java/com/payone/pcpclientandroiddemo/GooglePayActivity.kt) | Thin concrete page selecting `CheckoutMode.STANDARD` |
| [GooglePayCheckoutActivity](app/src/main/java/com/payone/pcpclientandroiddemo/gpay/checkout/GooglePayCheckoutActivity.kt) | Shared UI, delivery selector, readiness, Activity Result handling and lifecycle |
| [CheckoutConfig](app/src/main/java/com/payone/pcpclientandroiddemo/gpay/checkout/CheckoutConfig.kt) | Configurable environment, delivery options, pricing and validation |
| [CheckoutBackend](app/src/main/java/com/payone/pcpclientandroiddemo/gpay/checkout/CheckoutBackend.kt) | HTTP contract, contact/address mapping, required references and authorization interpretation |
| [CheckoutSession](app/src/main/java/com/payone/pcpclientandroiddemo/gpay/checkout/CheckoutSession.kt) | Persisted session/outcome, duplicate guard and background worker |
| [GooglePayStandard](pcp-client-android-sdk/src/main/java/com/payone/pcp_client_android_sdk/googlepay/GooglePayStandard.kt) | Reusable SDK readiness/method/request factory with a final price |
| [GooglePayExpressPayment](pcp-client-android-sdk/src/main/java/com/payone/pcp_client_android_sdk/googlepay/GooglePayExpress.kt) | Shared validated payment model, including `fromStandardJson` and PAYONE mobile-input mapping |

Despite its shared model name, Standard uses `GooglePayExpressPayment.fromStandardJson`, not the Express parser requiring sheet-selected delivery.

## 4. Configuration

`CheckoutConfig` is a demo integration model. Amounts are integer minor currency units; formatting uses the currency's fraction digits.

| Parameter | Default | Meaning |
| --- | --- | --- |
| `backendUrl` | `http://localhost:3009/api/` | API base URL including any deployment prefix, supplied before starting the checkout |
| `merchantName` | `PAYONE Google Pay Demo` | Merchant display name |
| `basketAmount` | `1000` | Gross basket including tax |
| `currencyCode` | `EUR` | Uppercase ISO currency |
| `countryCode` | `DE` | Merchant/delivery country; the demo accepts one delivery country |
| `deliveryOptions` | `standard`/0, `express`/499 | Unique option IDs, labels and gross delivery fees |
| `cardNetworks` | `VISA`, `MASTERCARD` | Accepted Google Pay card networks |
| `authMethods` | `PAN_ONLY`, `CRYPTOGRAM_3DS` | Accepted Google Pay authentication methods |
| `authorizationMode` | `PRE_AUTHORIZATION` | Also supports `SALE`; preauthorization does not capture funds |
| `executePaymentOnCreation` | `false` | Maps to PCP `autoExecuteOrder`; enable explicitly for combined creation/payment tests |
| `walletEnvironment` | `ENVIRONMENT_TEST` | Google environment, independent of the backend's PAYONE environment |

Customize the concrete Standard page, keeping shared flow logic in its base class:

```kotlin
class GooglePayActivity : GooglePayCheckoutActivity() {
    override val mode = CheckoutMode.STANDARD
    override val checkoutConfig = CheckoutConfig(
        backendUrl = "https://test-backend.example.com/v1/",
        merchantName = "My shop",
        basketAmount = 2500,
        deliveryOptions = listOf(
            DeliveryOption("pickup", "Collection", 0),
            DeliveryOption("nextday", "Next-day delivery", 700),
        ),
        executePaymentOnCreation = false,
    )
}
```

This produces a final total of €25.00 or €32.00 according to the page selection. Config validation rejects empty/duplicate option IDs, negative fees, overflowing totals and invalid SDK currency/country/network settings. Production pricing, taxes, inventory and accepted delivery options must be validated by the merchant backend; app parameters are not authoritative.

### Set your backend URL before starting the page

Pass your API base URL through the existing `CheckoutConfig(backendUrl = "...")` constructor parameter when defining the concrete page's `checkoutConfig`, as shown above. This is configuration in application code, set before the checkout starts; there is no URL input field or Intent-based URL override. Rebuild/install the demo after changing its configuration. Apply the same configuration to the separate Express page if you use both flows (see [Express backend configuration](GOOGLE_PAY_EXPRESS.md#backend-url-before-starting-the-checkout)).

`http://localhost:3009/api/` is only the default used by this repository's local test setup. Neither that host, that port nor `/api/` is required. For example:

| Backend setup | `backendUrl` parameter | Device routing |
| --- | --- | --- |
| Hosted backend under `/v1/` | `https://test-backend.example.com/v1/` | Device must be able to reach the host |
| Developer machine, API at root, port 8080 | `http://localhost:8080/` | Run `adb reverse tcp:8080 tcp:8080` for the connected device |
| Android emulator to host API under `/api/` | `http://10.0.2.2:8080/api/` | Android emulator host alias |
| LAN test server, API at root | `https://backend.internal.example:8443/` | Device DNS/network and TLS trust must permit access |

Supply the **API base URL including scheme, optional port and deployment prefix**. The adapter appends only `commerce-cases`, preserving the configured prefix. Both `/v1` and `/v1/` work; a base ending in `/` posts directly to `/commerce-cases`. No `/api/` prefix is hardcoded in requests. Separately, demo merchant discovery uses `/ui/config` at the same origin (outside the API prefix). A different server must expose that merchant-discovery route and the same checkout contract, or you must adapt `CheckoutBackend` to its API. Do not put credentials in the URL.

For local HTTP, Android must permit cleartext traffic to the selected host. The demo's [network security configuration](app/src/main/res/xml/network_security_config.xml) permits its listed development hosts; add your own development host there if necessary. Port changes do not require a new host entry. With `localhost`, the address refers to the Android device itself unless `adb reverse` forwards the selected port. Use HTTPS for hosted environments.

The chosen URL is stored with the session configuration before submission, so the backend call uses that session's configured endpoint.

## 5. SDK request and Google Pay result

Use the shared merchant/card configuration with the Standard request factory:

```kotlin
val googlePay = GooglePayStandard(
    GooglePayExpressConfig(
        gatewayMerchantId = payoneMerchantId,
        merchantName = "My shop",
        currencyCode = "EUR",
        countryCode = "DE",
        shippingCountries = setOf("DE"),
    ),
)
val client = Wallet.getPaymentsClient(
    activity,
    Wallet.WalletOptions.Builder()
        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
        .build(),
)
val readiness = googlePay.readinessRequest()
val allowedMethods = googlePay.allowedPaymentMethodsJson()
val request = googlePay.paymentDataRequest("14.99", reference)
```

Use `client.isReadyToPay(readiness)` to gate the official button and pass `allowedMethods` to its `allowedPaymentMethods`. The request uses `FINAL`, collects email/full shipping and billing details, and omits Express `callbackIntents` and shipping-option selection. Do not pass a basket-only total if delivery is charged separately.

Register `TaskResultContracts.GetPaymentDataResult()` during Activity initialization, before STARTED. At launch, use:

```kotlin
client.loadPaymentData(request).addOnCompleteListener(paymentLauncher::launch)
```

**Wallet 20 requires the Task to be completed before launching this contract.** Launching a pending Task throws `IllegalArgumentException`. The old `AutoResolveHelper.resolveTask` API is removed. The runnable registration/result handler is in `GooglePayCheckoutActivity`.

Handle the result as follows:

- **Success:** require a matching OPEN Standard session and non-null `result.result.toJson()`. Parse using `GooglePayExpressPayment.fromStandardJson(json, selectedShopShippingOptionId)`, then submit on the background worker.
- **Cancellation:** release an OPEN session; do not call the backend.
- **Google error:** display the status code; do not call the backend.
- **Invalid data:** missing token/email/full addresses or shop delivery selection fails validation before submission.

The selected shop delivery ID is required because the Standard sheet does not supply it. Persist it before launching and use that value when parsing the result. The model exposes the token, email, full billing/shipping JSON and delivery ID for backend integration; never log or persist the token or contact JSON. The opaque token is forwarded unchanged, without trimming, decrypting or reserializing its internal JSON.

## 6. PCP checkout creation

`CheckoutBackend.createCheckout(payment, reference)` requires validated Google Pay data. There is no tokenless overload or fallback. The relevant body for `POST /api/commerce-cases` is:

```json
{
  "merchantReference": "5da9a483036e4866b42f",
  "checkout": {
    "amountOfMoney": {"amount": 1000, "currencyCode": "EUR"},
    "references": {"merchantReference": "5da9a483036e4866b42f"},
    "autoExecuteOrder": false,
    "orderRequest": {
      "orderType": "FULL",
      "orderReferences": {"merchantReference": "5da9a483036e4866b42f"},
      "paymentMethodSpecificInput": {
        "mobilePaymentMethodSpecificInput": {
          "paymentProductId": 320,
          "authorizationMode": "PRE_AUTHORIZATION",
          "encryptedPaymentData": "<exact tokenizationData.token string>"
        }
      }
    }
  }
}
```

The real request also includes Google-derived customer billing/contact details and shipping address. The adapter maps `countryCode`, `postalCode`, `locality` and `address1` to PAYONE country, ZIP, city and street, joins extra address lines and splits the shipping name at the first space. Adapt house-number/name handling for the supported countries. The amount is recalculated from the final delivery country/option and session config.

**The Order reference is mandatory even when Commerce Case and Checkout references are set.** Its maximum length is **20 characters**, rather than the Checkout reference's 40. The demo generates a fresh 20-character reference and uses it on all three levels. Missing `orderReferences.merchantReference` caused HTTP 400/code `50308003`; a 36-character UUID caused code `50102001`. Both prevented checkout creation before payment execution.

With immediate execution disabled, the backend stores a payment-execution resource containing product 320 and the Google Pay input while leaving the checkout `OPEN`. Without a shopping cart the integration uses a Payment Execution, not an itemized PAYONE Order. Later execution follows the PCP payment-execution lifecycle; the app does not automatically replay saved tokens.

## 7. Payment result and error handling

- **Created:** the response contains `commerceCaseId` and `checkout.checkoutId` for displaying and retrieving the checkout.
- **Payment deferred:** `executePaymentOnCreation=false` schedules payment execution for a later step in the PCP lifecycle.
- **Authorized:** the payment response provides `checkout.paymentResponse.payment.id`, status `PENDING_CAPTURE` or `CAPTURED` and `statusOutput.isAuthorized=true`.
- **HTTP failure:** surface status/error codes/validated field names, without response messages or bodies that may echo sensitive input. There is no fallback creation request without Google Pay.
- **Network timeout/invalid response:** the result can be unknown; reconcile the reference in the backend before treating another checkout as a replacement.

The HTTP client uses 5-second connect/15-second read timeouts and does not follow redirects. Use an authenticated HTTPS backend with backend-owned prices and idempotency for production.

## 8. Session, retries and lifecycle

Both demo pages share `google-pay-express` preferences and a serial background worker. The preference name is historical. They store reference, mode, configuration, selected shop delivery and safe outcome IDs/status; no payment token or customer address is stored locally.

| State | Meaning |
| --- | --- |
| `IDLE` | Ready to start Google Pay |
| `OPEN` | Sheet active; cancellation before submission releases this session |
| `SUBMITTED` | Request started; unknown result after HTTP/network/parse failure |
| `CREATED` | Checkout created; IDs available |
| `CONFIRMED` | Backend confirmed authorization; repeated submission is not sent again |

An active sheet or actually running request blocks overlapping checkouts. A persisted `SUBMITTED` state alone does not permanently disable the button. After work finishes, an explicit click starts a fresh Google Pay session with a new reference and fresh payment data. Previous reference/state/safe outcome remains in `history`; no old token is replayed. Delivery selection/busy state survives Activity recreation, and preference listeners restore completed outcomes.

An orphaned OPEN session after process death still requires manual recovery: reconcile its backend reference, close the app, then remove only these demo preferences using Android Studio Device Explorer. This local guard is not server-side idempotency or complete process-death recovery.

## 9. Run and verify

For the connected physical device and local proxy:

```bash
adb reverse tcp:3009 tcp:3009
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Google Pay Standard Checkout**. Select delivery, confirm the sheet's final amount, then confirm Google TEST payment data. With execution deferred, the app should show a created checkout and deferred payment. Independently query `GET /api/commerce-cases/{commerceCaseId}/checkouts/{checkoutId}` for the default deployment (use your configured prefix elsewhere). Inspect status, amount and product ID without printing the returned token. To verify cancellation, dismiss the sheet before confirmation and ensure no new checkout request occurs. For emulators, `adb reverse` also works; alternatively configure `http://10.0.2.2:3009/api/`, allowed by the demo network security configuration.

### Automated checks

Coverage includes Standard FINAL/no-callback request construction, shop-selected delivery, token mapping, missing-data rejection, mandatory/20-character Order reference, execution flag, safe errors, HTTP failures, session persistence and duplicate guards. Backend tests cover root/custom prefixes with and without trailing slashes and independent `/ui/config` discovery.

```bash
./gradlew :pcp-client-android-sdk:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Reports are in each module's `build/reports/tests/testDebugUnitTest/index.html`.

## Sources and related documentation

- [Google Pay Express integration](GOOGLE_PAY_EXPRESS.md) for native shipping callbacks and Express-specific behavior.
- [Google Android tutorial](https://developers.google.com/pay/api/android/guides/tutorial), [request objects](https://developers.google.com/pay/api/android/reference/request-objects) and [client API](https://developers.google.com/pay/api/android/reference/client).
- Local PAYONE documentation under `docs/Apple Pay Express Checkout Documentation` and `docs/api-definition.yaml`, especially CreateCheckoutRequest, OrderRequest, mobile input and payment-execution schemas.
