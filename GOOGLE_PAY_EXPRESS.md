# Google Pay Express Checkout — Android

## Start here

Follow the shared [setup and backend instructions](GOOGLE_PAY_STANDARD.md#setup), then open **Google Pay Express Checkout**. The item costs 19.90 EUR; the sheet selects shipping and collects email, phone and full shipping/billing addresses. Initial total: ESTIMATED 24.80 EUR. Shipping updates return FINAL **24.80 / 29.80 EUR**.

## Code map

| File | Purpose |
| --- | --- |
| `GooglePayActivity.kt` | Direct Standard flow with merchant form |
| `gpay/express/GooglePayExpressActivity.kt` | Express readiness, full request, wallet launch and result display |
| `gpay/express/ExpressPaymentService.kt` | Shipping updates and backend authorization |
| `gpay/ConfigService.kt` | Mandatory backend URL/merchant ID and public wallet settings |
| `gpay/CommerceApiService.kt` | Typed POST creation and GET status transport |
| `gpay/PaymentAttemptStore.kt` | Persisted identity, shipping selection, submission guard and recovery |
| `gpay/GooglePayUtils.kt` | Stateless token/contact adapters and result mapping |

Activities contain their full Google requests. The service contains its full CommerceCase request. The reusable SDK keeps `GooglePayExpress`, `GooglePayStandard` and `GooglePayExpressService` APIs for integrations.

## Native callbacks

The demo manifest registers `ExpressPaymentService` with Google's signature-protected service permission:

```xml
<service android:name=".gpay.express.ExpressPaymentService"
    android:exported="true"
    android:permission="com.google.android.gms.permission.BIND_PAYMENTS_CALLBACK_SERVICE">
    <intent-filter>
        <action android:name="com.google.android.gms.wallet.callback.PAYMENT_DATA_CALLBACKS" />
    </intent-filter>
</service>
```

Wallet 20 callbacks are asynchronous:

```kotlin
override fun onShippingChanged(request: IntermediatePaymentData): Task<PaymentDataRequestUpdate>
override fun onAuthorize(payment: GooglePayExpressPayment, savedState: Bundle?): Task<Boolean>
```

Shipping updates use redacted addresses only. They accept international delivery, calculate flat sample prices, return the full shipping option list, and save the selected option and reference. No payment is submitted here. Missing/sentinel option IDs retain the current selection; unknown options fail. Real shops use server-authoritative quotes.

On Android 17, the demo requests `ACCESS_LOCAL_NETWORK` before checkout or status lookup. Allow it to reach a development backend on your Wi-Fi network. EUR and the DE merchant transaction country remain unchanged; neither restricts the address country.

Authorization validates the saved reference, Express mode, final full contacts and quoted shipping option. It persists `SUBMITTED` before its single POST. Repeated callbacks return the stored authorization result without submitting again. Activity results never POST: the service already handled the payment.

The SDK completes each listener once and returns an error after 25 seconds, within Google's 30-second deadline. A timed-out callback does not cancel an already running merchant request. Activity/service recreation reads persisted identity instead of Activity fields.

## Result and recovery

The sample uses FULL order, itemized cart, product **320**, SALE and `autoExecuteOrder=true`. The SDK retains the original Google token in `encryptedPaymentData`; `mobilePaymentMethodSpecificInputJson()` encodes it once as UTF-8/Base64 without line breaks for PAYONE's payload. The backend must not encode it again. Nested PAYONE error codes and error IDs are displayed without server messages or token data.

Only confirmed authorization/capture returns `true` to Google. A created checkout or HTTP 2xx alone does not establish payment success. UI distinguishes failed, pending, authorized, succeeded and requires-action. Native redirect/3DS continuation must be supplied by your merchant integration.

Both modes share the same attempt guard. Unknown POST outcomes block another order. **Check backend status** retrieves existing IDs with GET; **Start new order** requires a definitive succeeded/failed status. Without IDs, reconcile the reference on the server. An abandoned OPEN wallet can be released before submission; first close its old sheet. Legacy unresolved attempts remain blocked.

Only references, public settings, shipping IDs and safe result IDs/status are persisted. Tokens and customer contacts are never stored in preferences or logged. HTTP errors expose only status and validated error codes.

## Verify

Use the [build/test commands](GOOGLE_PAY_STANDARD.md#verify). Tests mirror Android's `src/main` packages under `src/test` and cover API routing/errors, token/contact mapping, persisted guards and status recovery. SDK tests cover requests and native callback deadlines.

On a physical device, verify shipping changes create no checkout, final shipping matches the quote, cancellation before authorization sends no POST, repeated authorization sends one POST, and rotation/service recreation retains the same reference. Wallet readiness is checked before displaying the official button. Launch `GetPaymentDataResult` only after the Wallet Task has completed.
