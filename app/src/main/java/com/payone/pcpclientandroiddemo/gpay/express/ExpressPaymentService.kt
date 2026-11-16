package com.payone.pcpclientandroiddemo.gpay.express

import android.os.Bundle
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.callback.IntermediatePaymentData
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressPayment
import com.payone.pcp_client_android_sdk.googlepay.GooglePayExpressService
import com.payone.pcpclientandroiddemo.gpay.*

/** Native shipping quotes and single-shot authorization; independent of any Activity instance. */
class ExpressPaymentService : GooglePayExpressService() {
    override fun onShippingChanged(request: IntermediatePaymentData): Task<PaymentDataRequestUpdate> = CheckoutTasks.run {
        val store = PaymentAttemptStore(this)
        val attempt = requireNotNull(store.current())
        check(attempt.mode == "EXPRESS" && attempt.phase == "OPEN")
        val data = JsonParser.parseString(requireNotNull(request.toJson())).asJsonObject
        val selected = data.getAsJsonObject("shippingOptionData")?.get("id")?.asString
            ?.takeUnless { it.isBlank() || it == "shipping_option_unselected" } ?: attempt.shippingOption
        store.selectShipping(attempt.reference, selected)
        val shipping = if (selected == "express") { 990L } else { 490L }
        PaymentDataRequestUpdate.fromJson(Gson().toJson(mapOf(
            "newTransactionInfo" to mapOf("totalPriceStatus" to "FINAL", "totalPrice" to money(1990 + shipping), "totalPriceLabel" to "Total",
                "currencyCode" to "EUR", "countryCode" to "DE", "displayItems" to listOf(
                    mapOf("label" to "Demo item", "type" to "SUBTOTAL", "price" to "19.90"),
                    mapOf("label" to "Delivery", "type" to "SHIPPING_OPTION", "price" to money(shipping)),
                )),
            "newShippingOptionParameters" to mapOf("defaultSelectedOptionId" to selected, "shippingOptions" to listOf(
                mapOf("id" to "standard", "label" to "4.90 EUR: Standard delivery"), mapOf("id" to "express", "label" to "9.90 EUR: Express delivery"),
            )),
        ))).withUpdatedSavedState(Bundle().apply { putString("reference", attempt.reference) })
    }

    override fun onAuthorize(payment: GooglePayExpressPayment, savedState: Bundle?): Task<Boolean> = CheckoutTasks.run {
        val store = PaymentAttemptStore(this)
        val attempt = requireNotNull(store.current())
        check(attempt.mode == "EXPRESS" && savedState?.getString("reference") == attempt.reference)
        if (attempt.phase != "OPEN") {
            return@run attempt.outcome?.authorized == true
        }
        check(payment.shippingOptionId == attempt.shippingOption) { "Shipping option changed since the last quote." }
        val shippingAddress = googleAddress(JsonParser.parseString(payment.shippingAddressJson).asJsonObject)
        val billing = JsonParser.parseString(payment.billingAddressJson).asJsonObject
        val billingAddress = googleAddress(billing)
        val customerName = billingAddress.remove("name")
        val shipping = if (payment.shippingOptionId == "express") { 990L } else { 490L }
        val body = Gson().toJsonTree(mapOf(
            "merchantReference" to attempt.reference,
            "customer" to mapOf("billingAddress" to billingAddress, "personalInformation" to mapOf("name" to customerName), "contactDetails" to mapOf(
                "emailAddress" to payment.email, "phoneNumber" to billing.get("phoneNumber")?.asString.orEmpty(),
            )),
            "checkout" to mapOf(
                "amountOfMoney" to mapOf("amount" to 1990L + shipping, "currencyCode" to "EUR"),
                "references" to mapOf("merchantReference" to attempt.reference), "shipping" to mapOf("address" to shippingAddress),
                "shoppingCart" to mapOf("items" to listOf(
                    mapOf("orderLineDetails" to mapOf("productCode" to "DEMO", "productPrice" to 1990, "quantity" to 1, "productType" to "GOODS", "productName" to "Demo item")),
                    mapOf("orderLineDetails" to mapOf("productCode" to "SHIPPING", "productPrice" to shipping, "quantity" to 1, "productType" to "SHIPMENT", "productName" to "Delivery")),
                )),
                "autoExecuteOrder" to true,
                "orderRequest" to mapOf("orderType" to "FULL", "orderReferences" to mapOf("merchantReference" to attempt.reference),
                    "paymentMethodSpecificInput" to mapOf("mobilePaymentMethodSpecificInput" to JsonParser.parseString(payment.mobilePaymentMethodSpecificInputJson("SALE")))),
            ),
        )).asJsonObject
        check(store.beginSubmission(attempt.reference))
        val response = CommerceApiService(attempt.backendUrl).createCommerceCase(body)
        val outcome = createdOutcome(response)
        store.created(attempt.reference, outcome)
        outcome.authorized
    }
}
