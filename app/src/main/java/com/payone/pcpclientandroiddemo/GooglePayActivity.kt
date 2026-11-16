package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.contract.TaskResultContracts.GetPaymentDataResult
import com.google.gson.Gson
import com.google.pay.button.PayButton
import com.payone.pcp_client_android_sdk.googlepay.GooglePayStandard
import com.payone.pcpclientandroiddemo.gpay.*
import java.util.UUID

/** Standard checkout: freeze merchant contacts/delivery; Google Pay supplies only the token. */
class GooglePayActivity : BaseActivity() {
    private lateinit var status: TextView
    private lateinit var store: PaymentAttemptStore
    private lateinit var config: ConfigService
    private lateinit var delivery: Spinner
    private val fields = linkedMapOf<String, EditText>()
    private val busy = mutableStateOf(false)
    private var reference: String? = null
    private var snapshot: Map<String, String>? = null
    private val client by lazy {
        Wallet.getPaymentsClient(this, Wallet.WalletOptions.Builder().setEnvironment(config.walletEnvironment).build())
    }

    private val paymentLauncher = registerForActivityResult(GetPaymentDataResult()) { result ->
        val ref = reference
        if (ref != null && result.status.isSuccess) {
            try {
                val token = googleToken(requireNotNull(result.result?.toJson()))
                val contact = requireNotNull(snapshot) { "Merchant contact snapshot unavailable" }
                val attempt = requireNotNull(store.current())
                check(attempt.reference == ref && attempt.mode == "STANDARD")
                val shipping = if (attempt.shippingOption == "express") { 990L } else { 490L }
                val address = mapOf(
                    "countryCode" to contact.getValue("countryCode"), "zip" to contact.getValue("zip"), "city" to contact.getValue("city"),
                    "street" to contact.getValue("street"),
                    "name" to mapOf("firstName" to contact.getValue("firstName"), "surname" to contact.getValue("surname")),
                )
                val body = Gson().toJsonTree(mapOf(
                    "merchantReference" to ref,
                    "customer" to mapOf("billingAddress" to address.filterKeys { it != "name" }, "personalInformation" to mapOf("name" to address.getValue("name")), "contactDetails" to mapOf(
                        "emailAddress" to contact.getValue("email"), "phoneNumber" to contact.getValue("phone"),
                    )),
                    "checkout" to mapOf(
                        "amountOfMoney" to mapOf("amount" to 1990L + shipping, "currencyCode" to "EUR"),
                        "references" to mapOf("merchantReference" to ref), "shipping" to mapOf("address" to address),
                        "shoppingCart" to mapOf("items" to listOf(
                            mapOf("orderLineDetails" to mapOf("productCode" to "DEMO", "productPrice" to 1990, "quantity" to 1, "productType" to "GOODS", "productName" to "Demo item")),
                            mapOf("orderLineDetails" to mapOf("productCode" to "SHIPPING", "productPrice" to shipping, "quantity" to 1, "productType" to "SHIPMENT", "productName" to "Delivery")),
                        )),
                        "autoExecuteOrder" to true,
                        "orderRequest" to mapOf("orderType" to "FULL", "orderReferences" to mapOf("merchantReference" to ref),
                            "paymentMethodSpecificInput" to mapOf("mobilePaymentMethodSpecificInput" to mapOf(
                                "paymentProductId" to 320, "authorizationMode" to "SALE", "encryptedPaymentData" to token,
                            ))),
                    ),
                )).asJsonObject
                status.text = "Submitting payment…"
                CheckoutTasks.run {
                    check(store.beginSubmission(ref)) { "Payment already submitted. Check its backend status." }
                    val outcome = createdOutcome(CommerceApiService(attempt.backendUrl).createCommerceCase(body))
                    store.created(ref, outcome)
                    outcome
                }.addOnCompleteListener(this) { task ->
                    busy.value = store.blocked()
                    status.text = if (task.isSuccessful) { describe(task.result) } else { "${task.exception?.message}\nReference: $ref. Check backend status before retrying." }
                }
            } catch (error: Exception) {
                store.cancel(ref)
                busy.value = store.blocked()
                enableForm(!busy.value)
                status.text = error.message
            }
        } else {
            if (ref != null) {
                store.cancel(ref)
            }
            busy.value = store.blocked()
            enableForm(!busy.value)
            status.text = "Google Pay cancelled or failed. No new payment submitted."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "Google Pay Standard Checkout"
        super.onCreate(savedInstanceState)
        store = PaymentAttemptStore(this)
        reference = savedInstanceState?.getString("reference")
        snapshot = savedInstanceState?.getBundle("contact")?.let { bundle -> bundle.keySet().associateWith { bundle.getString(it).orEmpty() } }
        status = TextView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            addView(status)
        }
        listOf("firstName", "surname", "email", "phone", "street", "zip", "city", "countryCode").forEach { key ->
            fields[key] = EditText(this).apply {
                hint = if (key == "countryCode") { "Country code (e.g. DE, US, AT)" } else { key }
                setSingleLine(true)
                setText(snapshot?.get(key) ?: savedInstanceState?.getString("field:$key"))
                content.addView(this)
            }
        }
        delivery = Spinner(this).apply {
            adapter = ArrayAdapter(this@GooglePayActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Standard: 4.90 EUR", "Express: 9.90 EUR"))
            setSelection(savedInstanceState?.getInt("delivery") ?: 0)
        }
        content.addView(delivery)
        val wallet = ComposeView(this)
        content.addView(wallet)
        content.addView(Button(this).apply {
            text = "Check backend status"
            setOnClickListener {
                withCheckoutNetworkAccess {
                CheckoutTasks.run { store.reconcile() }.addOnCompleteListener(this@GooglePayActivity) { task ->
                    status.text = if (task.isSuccessful) { describe(task.result) } else { task.exception?.message }
                }
                }
            }
        })
        content.addView(Button(this).apply {
            text = "Start new order"
            setOnClickListener {
                try { store.newOrder(); recreate() } catch (error: Exception) { status.text = error.message }
            }
        })
        content.addView(Button(this).apply {
            text = "Close abandoned wallet (before submission)"
            setOnClickListener {
                store.current()?.let { store.cancel(it.reference) }
                if (!store.blocked()) { recreate() } else { status.text = "Payment submitted. Check backend status." }
            }
        })
        setContentView(ScrollView(this).apply { addView(content) })
        busy.value = store.blocked()
        enableForm(!busy.value)
        try {
            config = ConfigService(this)
            val standard = GooglePayStandard(config.walletConfig())
            if (busy.value) {
                status.text = store.current()?.outcome?.let { describe(it) }
                    ?: "Previous wallet/payment attempt. Reference: ${store.current()?.reference ?: "legacy"}. Check status or close an abandoned wallet before submission."
            }
            client.isReadyToPay(standard.readinessRequest()).addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result == true) {
                    if (!busy.value) { status.text = "Fill in merchant contacts. Total: 24.80 / 29.80 EUR." }
                    wallet.setContent { PayButton(onClick = { withCheckoutNetworkAccess { launchPayment() } }, enabled = !busy.value, allowedPaymentMethods = standard.allowedPaymentMethodsJson()) }
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
            val contact = fields.mapValues { (key, field) ->
                val value = field.text.toString().trim()
                if (key == "countryCode") { value.uppercase(java.util.Locale.ROOT) } else { value }
            }
            require(contact.values.all { it.isNotBlank() }) { "Complete all merchant contact fields." }
            require(contact.getValue("countryCode") in java.util.Locale.getISOCountries()) { "Enter a valid two-letter country code." }
            val ref = UUID.randomUUID().toString().replace("-", "").take(20)
            val option = if (delivery.selectedItemPosition == 1) { "express" } else { "standard" }
            val shipping = if (option == "express") { 990L } else { 490L }
            val request = Gson().toJson(mapOf(
                "apiVersion" to 2, "apiVersionMinor" to 0,
                "merchantInfo" to mapOf("merchantName" to config.merchantName),
                "allowedPaymentMethods" to listOf(mapOf("type" to "CARD", "parameters" to mapOf(
                    "allowedAuthMethods" to listOf("PAN_ONLY", "CRYPTOGRAM_3DS"), "allowedCardNetworks" to listOf("VISA", "MASTERCARD")),
                    "tokenizationSpecification" to mapOf("type" to "PAYMENT_GATEWAY", "parameters" to mapOf("gateway" to "payonegmbh", "gatewayMerchantId" to config.gatewayMerchantId)))),
                "transactionInfo" to mapOf("totalPriceStatus" to "FINAL", "totalPrice" to money(1990 + shipping),
                    "totalPriceLabel" to "Total", "currencyCode" to "EUR", "countryCode" to "DE", "transactionId" to ref,
                    "displayItems" to listOf(mapOf("label" to "Demo item", "type" to "SUBTOTAL", "price" to "19.90"), mapOf("label" to "Delivery", "type" to "SHIPPING_OPTION", "price" to money(shipping)))),
            ))
            val paymentRequest = PaymentDataRequest.fromJson(request)
            store.start(ref, "STANDARD", config, option)
            reference = ref
            snapshot = contact
            busy.value = true
            enableForm(false)
            client.loadPaymentData(paymentRequest).addOnCompleteListener(paymentLauncher::launch)
        } catch (error: Exception) {
            reference?.let { store.cancel(it) }
            busy.value = store.blocked()
            enableForm(!busy.value)
            status.text = error.message
        }
    }

    private fun enableForm(enabled: Boolean) {
        fields.values.forEach { it.isEnabled = enabled }
        delivery.isEnabled = enabled
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("reference", reference)
        outState.putInt("delivery", delivery.selectedItemPosition)
        fields.forEach { (key, field) -> outState.putString("field:$key", field.text.toString()) }
        snapshot?.let { contact -> outState.putBundle("contact", Bundle().apply { contact.forEach { (key, value) -> putString(key, value) } }) }
        super.onSaveInstanceState(outState)
    }
}
