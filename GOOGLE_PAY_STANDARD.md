# Google Pay Standard Checkout — Android

The complete demo flow is in [`GooglePayActivity.kt`](app/src/main/java/com/payone/pcpclientandroiddemo/GooglePayActivity.kt).

## Setup

1. Build/install the demo on a Google Pay-capable device with Google Play services.
2. Set `checkout.backendBaseUrl` and `checkout.gatewayMerchantId` in `app/local.properties` and rebuild (see the README). Alternatively, set both in **Checkout settings** in the main menu. Saved settings override build defaults; **Clear settings** restores them. Both values are mandatory.
3. Open **Google Pay Standard Checkout** and enter customer name, email, phone and address, including its two-letter country code. This demo uses the same address for billing and shipping. On Android 17, allow the local network permission before checkout when using your development backend.
4. Select Standard delivery (4.90 EUR) or Express delivery (9.90 EUR), then tap Google Pay.

The sample item costs 19.90 EUR, including tax. Final totals are **24.80 / 29.80 EUR**. Contacts and delivery are frozen before opening the wallet. Google supplies only the token; contacts come from the merchant form.

The demo uses Google TEST, VISA/MASTERCARD and PAN_ONLY/CRYPTOGRAM_3DS. Public wallet settings are in `ConfigService.kt`; card request parameters are visible in the Activity. Google TEST and the server's PAYONE environment are independent. Product **320** must be configured for the merchant.

## Backend

`CommerceApiService` takes an explicit base URL and calls:

- `POST commerce-cases`
- `GET commerce-cases/{commerceCaseId}/checkouts/{checkoutId}`

Examples: emulator `http://10.0.2.2:3000/api/`, phone on Wi-Fi `http://YOUR-COMPUTER-IP:3000/api/`, or USB `http://localhost:3000/api/` with `adb reverse tcp:3000 tcp:3000`. Use a reachable server; localhost on a phone refers to the phone. The API prefix is preserved. There is no merchant-discovery endpoint requirement. API credentials stay on the server.

The Activity shows the complete CommerceCase payload: customer contacts, shipping, itemized cart, 20-character order reference, `autoExecuteOrder=true`, FULL order, product 320 and SALE. `googleToken()` encodes the complete original `tokenizationData.token` once as UTF-8/Base64 without line breaks for PAYONE's `encryptedPaymentData`. The backend must not encode it again. Your merchant backend validates prices and enforces idempotency. Nested PAYONE error codes and error IDs are displayed without server messages or token data.

## Results and recovery

Cancellation before submission releases the attempt. Before POST, the app persists a `SUBMITTED` marker; after the response it saves checkout IDs and the mapped result. Tokens and contact payloads are not saved in preferences. Form state is retained only through Android's Activity saved state.

Created, pending, authorized and captured are distinct results. A failed or lost response does not automatically start another payment. Use **Check backend status**. Only a definitive succeeded/failed GET result enables **Start new order**.

If no IDs were received, reconcile the displayed merchant reference on the server before resetting demo data. Legacy attempts remain blocked across upgrades. **Close abandoned wallet (before submission)** releases only an OPEN attempt; close the old sheet first. Submitted attempts cannot be released by this control.

Additional actions/3DS are displayed as `requires-action`; native continuation is not implemented by this demo. Continue through your merchant integration and recheck status.

## Verify

```bash
./gradlew :app:testDebugUnitTest :pcp-client-android-sdk:testDebugUnitTest :app:assembleDebug :app:lintDebug :pcp-client-android-sdk:lintDebug --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Gradle daemon requires Oracle JDK 27. Native wallet tests additionally require a compatible physical device/account.

Manual checks: both delivery totals; cancellation sends no POST; a confirmed payment sends one POST; rotation retains frozen contacts; an uncertain POST blocks another order; status lookup uses GET only.

See [Express Checkout](GOOGLE_PAY_EXPRESS.md) for native wallet shipping/authorization callbacks.
