package com.payone.pcpclientandroiddemo.gpay

object GooglePayRequestJson {
    val allowedPaymentMethods: String = """
        [
          {
            "type": "CARD",
            "parameters": {
              "allowedAuthMethods": ["PAN_ONLY", "CRYPTOGRAM_3DS"],
              "allowedCardNetworks": ["MASTERCARD", "VISA"],
              "billingAddressParameters": {
                "format": "FULL"
              }
            },
            "tokenizationSpecification": {
              "type": "PAYMENT_GATEWAY",
              "parameters": {
                "gateway": "payonegmbh",
                "gatewayMerchantId": "exampleGatewayMerchantId"
              }
            }
          }
        ]
    """.trimIndent()

    fun getRequestJson(
        merchantId: String,
        merchantName: String,
        totalPrice: String,
        currencyCode: String = "EUR",
        countryCode: String = "DE"
    ): String = """
        {
          "apiVersion": 2,
          "apiVersionMinor": 0,
          "allowedPaymentMethods": $allowedPaymentMethods,
          "merchantInfo": {
            "merchantId": "$merchantId",
            "merchantName": "$merchantName"
          },
          "transactionInfo": {
            "totalPriceStatus": "FINAL",
            "totalPriceLabel": "Total",
            "totalPrice": "$totalPrice",
            "currencyCode": "$currencyCode",
            "countryCode": "$countryCode"
          },
          "shippingAddressRequired": true,
          "shippingOptionRequired": true,
          "shippingOptionParameters": {
            "shippingOptions": [
              {
                "id": "standard",
                "label": "Standard Shipping",
                "description": "Arrives in 5-7 days"
              },
              {
                "id": "express",
                "label": "Express Shipping",
                "description": "Arrives in 2-3 days"
              }
            ],
            "defaultSelectedOptionId": "standard"
          }
        }
    """.trimIndent()
}
