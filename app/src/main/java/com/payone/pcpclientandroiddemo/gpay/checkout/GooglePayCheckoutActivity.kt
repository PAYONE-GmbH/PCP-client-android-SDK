package com.payone.pcpclientandroiddemo.gpay.checkout

import android.os.Bundle
import android.content.SharedPreferences
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.google.pay.button.PayButton
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpress
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import com.payone.pcp_client_android_sdk.googlepay.GooglePayStandard
import com.payone.pcpclientandroiddemo.BaseActivity
import java.util.UUID

/** Shared UI/lifecycle. Concrete pages supply checkout mode and configuration. */
abstract class GooglePayCheckoutActivity : BaseActivity() {
    protected abstract val mode: CheckoutMode
    protected open val checkoutConfig: CheckoutConfig by lazy { BackendSettings(this).configuration() }
    private lateinit var status: TextView
    private lateinit var delivery: Spinner
    private lateinit var session: CheckoutSession
    private var express: GooglePayExpress? = null
    private var standard: GooglePayStandard? = null
    private val busy = mutableStateOf(false)
    private val sessionListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "state" || key == "completion") restoreBackendProgress()
    }
    private val client by lazy {
        Wallet.getPaymentsClient(this, Wallet.WalletOptions.Builder().setEnvironment(checkoutConfig.walletEnvironment).build())
    }

    private val paymentLauncher = registerForActivityResult(GetPaymentDataResult()) { result ->
        if (mode == CheckoutMode.STANDARD && result.status.isSuccess) {
            try {
                check(session.mode() == mode && session.state() == "OPEN")
                val payment = GooglePayExpressPayment.fromStandardJson(
                    requireNotNull(result.result?.toJson()), session.shippingOption(),
                )
                status.text = "Google Pay data received. Creating PCP checkout with Google Pay…"
                CheckoutTasks.run { session.submit(payment) }.addOnCompleteListener(this) {
                    busy.value = false
                    showOutcome("Backend request finished. Check backend status before retrying if unconfirmed.")
                }
            } catch (_: Exception) {
                busy.value = false
                session.cancelBeforeSubmission()
                showOutcome("Invalid Google Pay result. No new checkout was submitted.")
            }
        } else {
            busy.value = false
            session.cancelBeforeSubmission()
            val message = when {
                result.status.isSuccess -> "Google Pay finished."
                result.status.statusCode == CommonStatusCodes.CANCELED -> "Google Pay cancelled."
                else -> "Google Pay failed (code ${result.status.statusCode})."
            }
            showOutcome(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = if (mode == CheckoutMode.STANDARD) "Google Pay Standard Checkout" else "Google Pay Express Checkout"
        super.onCreate(savedInstanceState)
        session = CheckoutSession(this)
        busy.value = savedInstanceState?.getBoolean("busy") ?: false
        status = TextView(this).apply { text = "Connecting to test backend…" }
        delivery = Spinner(this).apply {
            adapter = ArrayAdapter(this@GooglePayCheckoutActivity, android.R.layout.simple_spinner_dropdown_item,
                checkoutConfig.deliveryOptions.map { "${it.label}: ${checkoutConfig.money(it.amount)} ${checkoutConfig.currencyCode}" })
            setSelection(savedInstanceState?.getInt("delivery") ?: 0)
            visibility = if (mode == CheckoutMode.STANDARD) View.VISIBLE else View.GONE
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!busy.value && standard != null) showBasket()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        val payButton = ComposeView(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(status)
            addView(delivery)
            addView(payButton)
        })
        CheckoutTasks.run { CheckoutBackend(checkoutConfig).merchantId() }.addOnSuccessListener(this) { merchantId ->
            val config = checkoutConfig.googlePayConfig(merchantId)
            express = GooglePayExpress(config)
            standard = GooglePayStandard(config)
            client.isReadyToPay(requireNotNull(standard).readinessRequest()).addOnSuccessListener(this) { ready ->
                if (ready) {
                    if (!busy.value) showBasket()
                    payButton.setContent {
                        PayButton(onClick = { launchPayment() }, enabled = !busy.value,
                            allowedPaymentMethods = requireNotNull(standard).allowedPaymentMethodsJson())
                    }
                } else status.text = "Google Pay is not available on this device."
            }.addOnFailureListener(this) { status.text = "Google Pay readiness check failed." }
        }.addOnFailureListener(this) { error ->
            status.text = BackendSettings(this).connectionError(checkoutConfig.backendUrl, error)
        }
    }

    private fun selectedOption() = checkoutConfig.deliveryOptions[delivery.selectedItemPosition.coerceAtLeast(0)].id

    private fun showBasket() {
        if (session.state() in setOf("SUBMITTED", "CREATED", "CONFIRMED")) {
            showOutcome("Previous checkout result. Google Pay starts a new checkout.")
            return
        }
        val total = checkoutConfig.total(checkoutConfig.countryCode, selectedOption())
        status.text = if (mode == CheckoutMode.STANDARD)
            "Standard checkout: select delivery here. Total: ${checkoutConfig.money(total)} ${checkoutConfig.currencyCode}.\n" +
                "A PCP checkout is created only after Google Pay returns payment data."
        else "Express checkout: delivery is selected in Google Pay. Basket: ${checkoutConfig.money(checkoutConfig.basketAmount)} ${checkoutConfig.currencyCode}.\n" +
                "A PCP checkout is created only in the Google Pay authorization callback."
    }

    private fun launchPayment() {
        if (busy.value) return
        var started = false
        try {
            val reference = UUID.randomUUID().toString().replace("-", "").take(20)
            val option = selectedOption()
            session.start(reference, mode, checkoutConfig, option)
            started = true
            busy.value = true
            delivery.isEnabled = false
            val request = if (mode == CheckoutMode.STANDARD)
                requireNotNull(standard).paymentDataRequest(checkoutConfig.money(checkoutConfig.total(checkoutConfig.countryCode, option)), reference)
            else requireNotNull(express).paymentDataRequest(checkoutConfig.money(checkoutConfig.basketAmount), reference)
            client.loadPaymentData(request).addOnCompleteListener(paymentLauncher::launch)
        } catch (_: Exception) {
            busy.value = false
            if (started) session.cancelBeforeSubmission()
            showOutcome("Checkout could not start. Another checkout may still be running.")
        }
    }

    private fun showOutcome(message: String) {
        delivery.isEnabled = !busy.value
        val outcome = session.outcome()
        status.text = if (outcome != null) {
            "PCP checkout created with Google Pay. " +
                (if (outcome.authorized) "Payment authorized."
                else if (!session.config().executePaymentOnCreation) "Payment execution deferred; no authorization requested."
                else "Payment NOT confirmed; check backend before retrying.") +
                "\nCommerce case: ${outcome.commerceCaseId}\nCheckout: ${outcome.checkoutId}\nReference: ${outcome.reference}"
        } else "$message\n" + (session.failure()?.let { "$it\n" } ?: "") + "Session: ${session.state()}. " +
            if (session.state() == "SUBMITTED")
                "Previous outcome unknown; check this reference in the backend. " +
                    "The Google Pay button starts a new checkout, without replaying the previous payment.\nReference: ${session.reference()}" else ""
    }

    private fun restoreBackendProgress() {
        if (session.state() in setOf("SUBMITTED", "CREATED", "CONFIRMED")) {
            busy.value = session.isSubmitting()
            showOutcome("Google Pay data submitted. Reconcile backend status if the outcome remains unknown.")
        }
    }

    override fun onResume() {
        super.onResume()
        session.observe(sessionListener)
        restoreBackendProgress()
    }

    override fun onPause() {
        session.stopObserving(sessionListener)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("busy", busy.value)
        outState.putInt("delivery", delivery.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }
}
