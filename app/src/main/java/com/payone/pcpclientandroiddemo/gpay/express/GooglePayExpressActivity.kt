package com.payone.pcpclientandroiddemo.gpay.express

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.google.gson.Gson
import com.google.pay.button.PayButton
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpress
import com.payone.pcpclientandroiddemo.BaseActivity
import com.payone.pcpclientandroiddemo.gpay.*
import java.util.UUID

/** Starts the native Express sheet. Only ExpressPaymentService submits payment data. */
class GooglePayExpressActivity : BaseActivity() {
    private lateinit var status: TextView
    private lateinit var store: PaymentAttemptStore
    private lateinit var config: ConfigService
    private val busy = mutableStateOf(false)
    private var reference: String? = null
    private val progress = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> showProgress() }
    private val client by lazy {
        Wallet.getPaymentsClient(this, Wallet.WalletOptions.Builder().setEnvironment(config.walletEnvironment).build())
    }
    private val paymentLauncher = registerForActivityResult(GetPaymentDataResult()) { result ->
        reference?.let { store.cancel(it) }
        showProgress()
        if (!store.blocked()) {
            status.text = if (result.status.isSuccess) { "Wallet finished without a submitted payment." } else { "Google Pay cancelled or failed." }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "Google Pay Express Checkout"
        super.onCreate(savedInstanceState)
        store = PaymentAttemptStore(this)
        reference = savedInstanceState?.getString("reference")
        status = TextView(this)
        val wallet = ComposeView(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(status)
            addView(wallet)
            addView(Button(this@GooglePayExpressActivity).apply {
                text = "Check backend status"
                setOnClickListener {
                    withCheckoutNetworkAccess {
                    CheckoutTasks.run { store.reconcile() }.addOnCompleteListener(this@GooglePayExpressActivity) { task ->
                        status.text = if (task.isSuccessful) { describe(task.result) } else { task.exception?.message }
                    }
                    }
                }
            })
            addView(Button(this@GooglePayExpressActivity).apply {
                text = "Start new order"
                setOnClickListener {
                    try { store.newOrder(); recreate() } catch (error: Exception) { status.text = error.message }
                }
            })
            addView(Button(this@GooglePayExpressActivity).apply {
                text = "Close abandoned wallet (before submission)"
                setOnClickListener {
                    store.current()?.let { store.cancel(it.reference) }
                    if (!store.blocked()) { recreate() } else { status.text = "Payment submitted. Check backend status." }
                }
            })
        })
        showProgress()
        try {
            config = ConfigService(this)
            val express = GooglePayExpress(config.walletConfig())
            client.isReadyToPay(express.readinessRequest()).addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result == true) {
                    wallet.setContent { PayButton(onClick = { withCheckoutNetworkAccess { launchPayment() } }, enabled = !busy.value, allowedPaymentMethods = express.allowedPaymentMethodsJson()) }
                } else {
                    status.text = "Google Pay unavailable or readiness check failed."
                }
            }
        } catch (error: Exception) {
            status.text = error.message
        }
    }

    private fun launchPayment() {
        if (busy.value) { return }
        try {
            val ref = UUID.randomUUID().toString().replace("-", "").take(20)
            val request = Gson().toJson(mapOf(
                "apiVersion" to 2, "apiVersionMinor" to 0,
                "merchantInfo" to mapOf("merchantName" to config.merchantName),
                "allowedPaymentMethods" to listOf(mapOf("type" to "CARD", "parameters" to mapOf(
                    "allowedAuthMethods" to listOf("PAN_ONLY", "CRYPTOGRAM_3DS"), "allowedCardNetworks" to listOf("VISA", "MASTERCARD"),
                    "billingAddressRequired" to true, "billingAddressParameters" to mapOf("format" to "FULL", "phoneNumberRequired" to true)),
                    "tokenizationSpecification" to mapOf("type" to "PAYMENT_GATEWAY", "parameters" to mapOf("gateway" to "payonegmbh", "gatewayMerchantId" to config.gatewayMerchantId)))),
                "transactionInfo" to mapOf("totalPriceStatus" to "ESTIMATED", "totalPrice" to "24.80", "totalPriceLabel" to "Total",
                    "currencyCode" to "EUR", "countryCode" to "DE", "transactionId" to ref, "checkoutOption" to "DEFAULT"),
                "emailRequired" to true, "shippingAddressRequired" to true,
                "shippingAddressParameters" to mapOf("phoneNumberRequired" to true),
                "shippingOptionRequired" to true,
                "callbackIntents" to listOf("PAYMENT_AUTHORIZATION", "SHIPPING_ADDRESS", "SHIPPING_OPTION"),
            ))
            val paymentRequest = PaymentDataRequest.fromJson(request)
            store.start(ref, "EXPRESS", config, "standard")
            reference = ref
            busy.value = true
            client.loadPaymentData(paymentRequest).addOnCompleteListener(paymentLauncher::launch)
        } catch (error: Exception) {
            reference?.let { store.cancel(it) }
            showProgress()
            status.text = error.message
        }
    }

    private fun showProgress() {
        busy.value = store.blocked()
        val attempt = store.current()
        status.text = attempt?.outcome?.let { describe(it) }
            ?: if (busy.value) { "Previous wallet/payment attempt: ${attempt?.phase ?: "legacy"}. Reference: ${attempt?.reference ?: "legacy"}. Check status or close an abandoned wallet before submission." }
            else { "Demo item: 19.90 EUR. Select delivery inside Google Pay (4.90 / 9.90 EUR)." }
    }

    override fun onResume() {
        super.onResume()
        store.observe(progress)
        showProgress()
    }

    override fun onPause() {
        store.stopObserving(progress)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("reference", reference)
        super.onSaveInstanceState(outState)
    }
}
