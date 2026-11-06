# Google Pay Standard and Express Checkout (native Android)

For a dedicated, step-by-step Standard integration guide, see [Google Pay Standard Checkout](GOOGLE_PAY_STANDARD.md). This guide details the shared integration and native Express callbacks.

Two separate pages share configurable checkout components. Both integrate Google Pay payments with PCP using PAYONE product **320**.

## Flows

| Step | Standard Checkout | Express Checkout |
| --- | --- | --- |
| Page | `GooglePayActivity` | `GooglePayExpressActivity` |
| Delivery selection | Spinner on the shop page | Inside the native Google Pay sheet |
| Initial total | `FINAL`, basket + selected delivery | `ESTIMATED`, basket; native callbacks provide `FINAL` totals |
| Contact/address | Google Pay collects full billing/shipping address, phone and email | Same; intermediate shipping callbacks expose only redacted addresses |
| Backend submission | After a successful Google Pay result, with the returned token | Inside the native `PAYMENT_AUTHORIZATION` callback |
| Confirmation | Page displays payment result and checkout IDs | Authorization callback completes the sheet; page displays IDs/status |
| Cancellation before submission | No checkout request; release local session | Same |

Opening either page, checking readiness, changing delivery on the Standard page or dismissing the sheet does not create a checkout.

The demo defaults to `executePaymentOnCreation=false` for deferred payment execution. Set it to `true` to request immediate execution.

## Prerequisites

- Android minSdk 24, Google Play services, a Google Pay-capable device/account. The button is shown only after readiness succeeds.
- Wallet **`com.google.android.gms:play-services-wallet:20.0.0`**, exposed by the SDK. Native Express callbacks require this API. The demo uses `com.google.pay.button:compose-pay-button:1.1.0`.
- PAYONE Google Pay/product **320** must be enabled for the merchant and selected card networks. Gateway is `payonegmbh`; `gatewayMerchantId` is the PAYONE merchant ID, not a Google web merchant ID.
- The existing test proxy is at `http://localhost:3009`; requests use `POST /api/commerce-cases`. `GET /ui/config` provides `merchantId` (currently `P1_18352_2013244`). Server credentials remain on the server. `GET /api/` can return 404 although the actual API routes work.
- Both demo pages explicitly use Google `ENVIRONMENT_TEST`. Google TEST does not select the PAYONE environment: the proxy must independently target the intended PAYONE test environment. Production requires Google production access for the package/signing certificate and a corresponding PAYONE production setup.

## Modules and responsibilities

SDK package `com.payone.pcp_client_android_sdk.googlepay`:

| Class | Responsibility |
| --- | --- |
| `GooglePayStandard` | Readiness, allowed methods and final-price Standard request; no Express callback intents |
| `GooglePayExpress` | Shared PAYONE/card configuration and estimated-price Express request with native callback intents |
| `GooglePayExpressConfig` | Merchant, currency, country, shipping countries, networks and authentication methods |
| `GooglePayExpressPayment` | Validated token/contact model; Standard and Express parsers; PAYONE mobile-input mapping |
| `GooglePayExpressService` | Async native callback bridge, safe errors and completion deadline |
| `GooglePayExpressResult` | Google Pay success/cancel/error status mapping |

Demo package `app/src/main/java/com/payone/pcpclientandroiddemo/gpay/checkout`:

| File | Responsibility |
| --- | --- |
| `CheckoutConfig.kt` | Shared parameters, delivery options, amount formatting and validation |
| `GooglePayCheckoutActivity.kt` | Shared UI/readiness/result/lifecycle logic; concrete pages select only mode/config |
| `CheckoutPricing.kt` | Parameter-driven Express delivery updates and final totals |
| `CheckoutBackend.kt` | Proxy contract, address/token mapping and payment-response handling |
| `CheckoutSession.kt` | Persisted identity/configuration/outcome, duplicate guard and serial async worker |

The separate concrete pages are [Standard](app/src/main/java/com/payone/pcpclientandroiddemo/GooglePayActivity.kt) and [Express](app/src/main/java/com/payone/pcpclientandroiddemo/gpay/express/GooglePayExpressActivity.kt). [ExpressPaymentService](app/src/main/java/com/payone/pcpclientandroiddemo/gpay/express/ExpressPaymentService.kt) connects the SDK callback bridge to these shared components. No Activity instance or singleton holds checkout identity for callbacks.

## Parameters

`CheckoutConfig` is a demo integration model; it is independent of the reusable SDK request API. Amounts are integer minor currency units, formatted using the configured currency's fraction digits.

| Parameter | Default | Meaning |
| --- | --- | --- |
| `backendUrl` | `http://localhost:3009/api/` | API base URL including any deployment prefix, supplied before checkout start |
| `merchantName` | `PAYONE Google Pay Demo` | Display name in the sheet |
| `basketAmount` | `1000` | Gross basket, including tax |
| `currencyCode` | `EUR` | Uppercase ISO currency |
| `countryCode` | `DE` | Merchant/delivery country; this demo allows one delivery country |
| `deliveryOptions` | `standard`/0, `express`/499 | Nonempty unique IDs, labels and nonnegative gross fees; first option is Express's initial default |
| `cardNetworks` | `VISA`, `MASTERCARD` | Google-supported networks accepted by the merchant |
| `authMethods` | `PAN_ONLY`, `CRYPTOGRAM_3DS` | Google-supported methods accepted by the merchant |
| `authorizationMode` | `PRE_AUTHORIZATION` | Also accepts `SALE`; PRE_AUTHORIZATION does not capture funds |
| `executePaymentOnCreation` | `false` | Maps to `autoExecuteOrder`; saves the Google Pay payment input without requesting immediate execution. Set `true` only when testing the combined creation/payment flow |
| `walletEnvironment` | `ENVIRONMENT_TEST` | Google Wallet environment, independent of the proxy/PAYONE environment |

Configure either concrete page by overriding `checkoutConfig`:

```kotlin
class GooglePayActivity : GooglePayCheckoutActivity() {
    override val mode = CheckoutMode.STANDARD // EXPRESS for the Express page
    override val checkoutConfig = CheckoutConfig(
        backendUrl = "https://test-backend.example.com/v1/",
        merchantName = "My shop",
        basketAmount = 2500,
        deliveryOptions = listOf(
            DeliveryOption("pickup", "Collection", 0),
            DeliveryOption("nextday", "Next-day delivery", 700),
        ),
        authorizationMode = "PRE_AUTHORIZATION",
        executePaymentOnCreation = false,
    )
}
```

Standard then sends €25.00 or €32.00 according to the page selection; Express starts estimated at €25.00 and updates in the sheet. The chosen config is persisted when a session starts, so service recreation uses the same prices/options. Invalid/duplicate options, negative fees, overflowing totals and invalid SDK merchant/currency/country/network settings are rejected. Parameters do not replace backend-authoritative pricing in a production shop.

### Backend URL before starting the checkout

Open **Backend URL** in the demo main menu, enter your API base URL and tap **Save** before opening the checkout page. The saved setting is shared by Standard and Express and survives app restarts. **Use default** restores `http://localhost:3009/api/`. No rebuild is needed. A concrete page can still supply an explicit `checkoutConfig` override in application code; this takes precedence over the saved setting for that page:

```kotlin
class GooglePayExpressActivity : GooglePayCheckoutActivity() {
    override val mode = CheckoutMode.EXPRESS
    override val checkoutConfig = CheckoutConfig(
        backendUrl = "https://test-backend.example.com/v1/",
    )
}
```

These classes use the shared demo types from `com.payone.pcpclientandroiddemo.gpay.checkout`. The stock pages use the shared runtime setting; rebuilding is only needed for application-code changes.

`http://localhost:3009/api/` is only the repository's local default. On a phone connected to the same Wi-Fi as your computer, enter a URL such as `http://192.168.178.42:4000/api/`, replacing the IP and port with yours. Bind the backend to `0.0.0.0` or its LAN address and allow its port through the firewall. No USB forwarding is needed. On an Android Studio emulator, use `http://10.0.2.2:4000/api/`. A hosted backend or HTTPS tunnel can use its own reachable hostname, port and prefix such as `/v1/`.

If you prefer localhost over USB, enter `http://localhost:8080/` and run `adb reverse tcp:8080 tcp:8080`. This posts to `/commerce-cases`. On a phone, localhost otherwise refers to the phone itself.

Provide the **API base URL with scheme, optional port and deployment prefix**. `CheckoutBackend` appends only the relative endpoint `commerce-cases`; `/api/` is not hardcoded. Trailing slashes are normalized, so `/v1` and `/v1/` both post to `/v1/commerce-cases`. Demo merchant discovery separately uses `/ui/config` at the same origin, outside the API prefix. Your server must implement that merchant-discovery route and checkout contract, or the adapter must be adjusted. The demo's [network security configuration](app/src/main/res/xml/network_security_config.xml) permits HTTP to arbitrary local development hosts without an allowlist edit. Use HTTPS for hosted environments. The URL editor rejects credentials, query strings, fragments and invalid hosts/ports.

The URL, including the API prefix, is persisted with the session configuration before launching Google Pay. The Express callback service reads that configuration, so callbacks use the session's backend after service recreation, even if the shared URL setting changes later. See also the [Standard URL/routing examples](GOOGLE_PAY_STANDARD.md#set-your-backend-url-before-starting-the-page).

## SDK requests and result handling

```kotlin
val config = GooglePayExpressConfig(
    gatewayMerchantId = payoneMerchantId,
    merchantName = "My shop",
    currencyCode = "EUR",
    countryCode = "DE",
    shippingCountries = setOf("DE"),
)
val standard = GooglePayStandard(config)
val express = GooglePayExpress(config)
val standardRequest = standard.paymentDataRequest("14.99", reference)
val expressRequest = express.paymentDataRequest("10.00", reference)
// Use readinessRequest() before rendering the official PayButton, and
// allowedPaymentMethodsJson() for its allowedPaymentMethods argument.
```

Register `TaskResultContracts.GetPaymentDataResult()` before the Activity reaches STARTED. After a user click, persist a unique merchant reference and the selected config, then:

```kotlin
client.loadPaymentData(request).addOnCompleteListener(paymentLauncher::launch)
```

**Wallet 20 requires a completed Task** when launching this contract. Launching the pending task throws `IllegalArgumentException`. `AutoResolveHelper.resolveTask` has been removed.

On a successful **Standard** result, parse `result.result.toJson()` with `GooglePayExpressPayment.fromStandardJson(json, selectedShopShippingOptionId)`, then submit it once to your backend. The shop option is mandatory because the Standard sheet does not select delivery. On cancellation/error, do not submit.

For **Express**, the SDK service parses final payment data with `GooglePayExpressPayment.fromJson(json)` using the sheet's shipping option. The Activity must **not** submit the token again: backend submission already ran in the callback.

The validated model exposes `encryptedPaymentData`, `email`, `shippingAddressJson`, `billingAddressJson`, `shippingOptionId`. Missing token/full contact data fails before backend submission. Parser errors exclude the payment payload. Never log/persist tokens, full payment JSON or customer address data.

## Express callback service

Register the concrete service (already present in the demo):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<application>
    <meta-data android:name="com.google.android.gms.wallet.api.enabled" android:value="true" />
    <service
        android:name=".gpay.express.ExpressPaymentService"
        android:exported="true"
        android:permission="com.google.android.gms.permission.BIND_PAYMENTS_CALLBACK_SERVICE">
        <intent-filter>
            <action android:name="com.google.android.gms.wallet.callback.PAYMENT_DATA_CALLBACKS" />
        </intent-filter>
    </service>
</application>
```

Subclass `GooglePayExpressService` and implement these asynchronous handlers:

```kotlin
override fun onShippingChanged(request: IntermediatePaymentData): Task<PaymentDataRequestUpdate>
override fun onAuthorize(payment: GooglePayExpressPayment, savedState: Bundle?): Task<Boolean>
```

Return Tasks promptly; never block the callback thread. Return authorization `true` only after confirmed backend authorization/capture. The SDK maps synchronous exceptions, failed Tasks and a **25-second deadline** to sheet errors and completes each listener once. Google requires completion within 30 seconds. Timeout does not cancel an already submitted payment; reconcile before retrying.

The service can be recreated between callbacks. Bind persisted checkout identity using `PaymentDataRequestUpdate.withUpdatedSavedState(Bundle)` and final `savedState`; the demo checks reference and Express mode. Callback intents are `PAYMENT_AUTHORIZATION`, `SHIPPING_ADDRESS`, `SHIPPING_OPTION`. The Web guide's `OFFER` callback is not included in this native API.

### Delivery updates

Handle `INITIALIZE`, `SHIPPING_ADDRESS` and `SHIPPING_OPTION` using the redacted `shippingAddress` and `shippingOptionData`. Missing/blank initial option or Google's sentinel `shipping_option_unselected` selects the first configured merchant option. Unknown merchant IDs fail; unsupported countries return `SHIPPING_ADDRESS_UNSERVICEABLE`.

Every successful update includes `newTransactionInfo` with `totalPriceStatus=FINAL`, currency/country, basket + selected delivery and display rows, even when price is unchanged. Also return `newShippingOptionParameters` containing the configured options/default. Gross amounts already include tax: do not add tax or subtotal rows twice. Initial Express requests use `ESTIMATED` and `checkoutOption=DEFAULT`; Google rejects estimated totals with `COMPLETE_IMMEDIATE_PURCHASE`.

## Backend contract and confirmation

`CheckoutBackend.createCheckout(payment, reference)` requires the validated payment model. Both modes use the same contract:

```json
{
  "merchantReference": "5da9a483036e4866b42f",
  "checkout": {
    "amountOfMoney": {"amount": 1499, "currencyCode": "EUR"},
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

The actual request also includes checkout references, Google-derived customer billing/contact information and shipping address. The token is forwarded **unchanged**, without trimming, decoding, decrypting or reserializing its internal JSON. Final country/option and configured total are validated again before submission. The existing PAYONE schema/endpoint is used; no Google-specific proxy endpoint was introduced. Without a shopping cart this creates a payment execution, not an itemized PAYONE Order.

**Required reference:** supplying commerce-case/checkout references alone is insufficient when `orderRequest` is present. PCP also requires `orderRequest.orderReferences.merchantReference`, with a maximum of **20 characters** (checkout references permit 40). The demo generates a fresh 20-character reference and uses it on all three levels. Missing order reference produced HTTP 400/code `50308003`; a 36-character UUID there produced HTTP 400/code `50102001`. Both errors previously prevented checkout creation before any payment could be attempted.

With `autoExecuteOrder=false`, the backend stores a `paymentExecutions` resource containing `mobilePaymentMethodSpecificInput.paymentProductId=320` and the Google Pay token, while the checkout remains `OPEN`. A later payment step follows the PCP payment-execution lifecycle; no automatic retry/replay is performed by the app.

Responses provide `commerceCaseId` and `checkout.checkoutId` for displaying and retrieving the checkout. The payment response includes `checkout.paymentResponse.payment.id`, status `PENDING_CAPTURE` or `CAPTURED` and `statusOutput.isAuthorized=true` for authorized payments. The Express callback maps the backend payment result to the sheet's success/error status.

HTTP errors display/store only the HTTP status, error codes and validated field names; response messages/bodies are excluded because a proxy can echo sensitive request data. A failed creation never falls back to a tokenless request.

Redirect/3DS continuation is not implemented in this demo; integrate that continuation and status refresh in a production application. The proxy connection has 5-second connect/15-second read timeouts and does not follow redirects. Use an authenticated HTTPS backend with server-owned pricing, inventory/tax validation and idempotency in production; do not trust app parameters as authoritative prices.

## Session and retry behavior

Both pages share one local guard (`google-pay-express` preferences, retained across upgrades). It stores only reference, mode, config, shop shipping option and safe outcome IDs/status. Payment tokens/customer addresses are not stored. Backend work runs on one serial worker.

| State | Meaning / next action |
| --- | --- |
| `IDLE` | No active session; a user may start Google Pay |
| `OPEN` | Sheet active; cancellation before submission returns to IDLE |
| `SUBMITTED` | Request started; unknown outcome on network/parse failure; check its reference in the backend. Only an actually running HTTP request disables the button |
| `CREATED` | Checkout created; IDs available; a fresh Google Pay checkout may be started |
| `CONFIRMED` | Authorized; repeated callback returns success without another POST; user may explicitly start a new checkout |

Activity recreation preserves selected delivery/busy state; persisted backend outcomes can be displayed after recreation. An active sheet or actually running backend request prevents overlapping Standard/Express sessions. A persisted `SUBMITTED` state alone does **not** permanently disable the Google Pay button. After the request finishes (including failure), an explicit button click starts a new Google Pay session with a new reference and fresh payment data. The previous reference/state/safe outcome is retained in the preferences' `history` set; no old token is replayed. Reconcile an uncertain previous payment separately before treating a new checkout as a replacement order.

An OPEN session orphaned by process death still requires recovery. This is a local duplicate guard, **not server-side idempotency or complete process-death recovery**. For that demo recovery, reconcile the reference in the backend first, then close the app and remove only the `google-pay-express` preferences using Android Studio Device Explorer. Never replay a previous token.

The address adapter maps Google's full address1 to PAYONE `street`, joins further lines into `additionalInfo` and splits names at the first space. Adapt house-number/name handling for your countries. Variable taxes, coupons and inventory are outside this configurable gross-basket example.

## Run and verify

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Set **Backend URL** in the main menu using the [routing options](#backend-url-before-starting-the-checkout), then open **Google Pay Standard Checkout** or **Google Pay Express Checkout**. Only localhost over USB needs `adb reverse tcp:3009 tcp:3009` (adjust the port to match your backend).

Standard: select Standard (€10.00) or Express (€14.99) delivery before opening Google Pay; verify the final sheet amount. Express: open the sheet and switch delivery there; verify the total updates from €10.00 to €14.99. Cancel before confirmation to verify no checkout submission. Use `executePaymentOnCreation=false` for deferred execution or `true` for immediate execution. Verify the checkout with `GET /api/commerce-cases/{commerceCaseId}/checkouts/{checkoutId}` and inspect only safe product/status fields, not the returned token.

### Automated checks

Tests cover request construction, token mapping, delivery prices, backend HTTP contracts, session persistence and duplicate-submission guards.

Run build/test/lint checks:

```bash
./gradlew :pcp-client-android-sdk:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :pcp-client-android-sdk:lintDebug :app:lintDebug --console=plain
```

New test classes: `GooglePayStandardTest`, `GooglePayExpressTest`, `GooglePayExpressServiceTest`, `CheckoutTest`, `CheckoutSessionTest`. Reports: each module's `build/reports/tests/testDebugUnitTest/index.html` and `build/reports/lint-results-debug.html`.

## Sources

- Local PAYONE Google Pay/Express docs under `docs/Apple Pay Express Checkout Documentation` and `docs/api-definition.yaml` (mobile input, checkout/order creation and response status).
- [Google native tutorial](https://developers.google.com/pay/api/android/guides/tutorial), [request objects](https://developers.google.com/pay/api/android/reference/request-objects), [callback API](https://developers.google.com/pay/api/android/reference/client).

The Web Express guide is adapted to Wallet 20 `BasePaymentDataCallbacksService`, `IntermediatePaymentData`, `PaymentDataRequestUpdate` and `PaymentAuthorizationResult`. No WebView or embedded Web JS callback is used.
