package com.payone.pcpclientandroiddemo.gpay.checkout

import com.google.gson.Gson
import com.google.gson.JsonParser

/** Native Express shipping updates. Production prices must come from the merchant backend. */
class CheckoutPricing(private val config: CheckoutConfig) {
    fun update(json: String): String {
        val data = JsonParser.parseString(json).asJsonObject
        val country = data.getAsJsonObject("shippingAddress")?.get("countryCode")?.asString
        val selected = data.getAsJsonObject("shippingOptionData")?.get("id")?.asString
            ?.takeUnless { it.isBlank() || it == "shipping_option_unselected" } ?: config.deliveryOptions.first().id
        if (country != config.countryCode) return Gson().toJson(mapOf("error" to mapOf(
            "intent" to "SHIPPING_ADDRESS", "reason" to "SHIPPING_ADDRESS_UNSERVICEABLE",
            "message" to "Delivery is available only in ${config.countryCode}.",
        )))
        val total = config.total(country, selected)
        val delivery = config.deliveryOptions.single { it.id == selected }
        return Gson().toJson(mapOf(
            "newTransactionInfo" to mapOf(
                "currencyCode" to config.currencyCode, "countryCode" to config.countryCode, "totalPriceStatus" to "FINAL",
                "totalPrice" to config.money(total), "totalPriceLabel" to "Total (incl. tax)",
                "displayItems" to listOf(
                    mapOf("label" to "Basket (incl. tax)", "type" to "SUBTOTAL", "price" to config.money(config.basketAmount)),
                    mapOf("label" to "Delivery", "type" to "SHIPPING_OPTION", "price" to config.money(delivery.amount)),
                ),
            ),
            "newShippingOptionParameters" to mapOf(
                "defaultSelectedOptionId" to selected,
                "shippingOptions" to config.deliveryOptions.map {
                    mapOf("id" to it.id, "label" to "${config.money(it.amount)} ${config.currencyCode}: ${it.label}")
                },
            ),
        ))
    }
}
