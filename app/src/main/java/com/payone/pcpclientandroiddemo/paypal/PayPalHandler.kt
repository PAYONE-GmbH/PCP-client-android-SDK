package com.payone.pcpclientandroiddemo.paypal

import android.app.Application
import android.content.Context
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.Address
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment




class PayPalHandler(private val context: Context) {
	lateinit var cardClient: CardClient

	fun initialize(clientId: String, returnUrl: String) {
		val config = CoreConfig(clientId, environment = Environment.SANDBOX)
		cardClient = CardClient(context,config)
        
	}

	fun startPayment(
		orderId: String,
	
	) {
val card = Card(
  number = "4005519200000004",
  expirationMonth = "01",
  expirationYear = "2025",
  securityCode = "123",
  billingAddress = Address(
    streetAddress = "123 Main St.",
    extendedAddress = "Apt. 1A",
    locality = "Anytown",
    region = "CA",
    postalCode = "12345",
    countryCode = "US"
  )
)

		val cardRequest  = CardRequest(
  orderId = orderId,
  card = card,
  returnUrl = "myapp://return_url", // custom URL scheme needs to be configured in AndroidManifest.xml
  sca = SCA.SCA_ALWAYS // default value is SCA.SCA_WHEN_REQUIRED
)

 cardClient.approveOrder( cardRequest, { approval ->
  // Handle successful approval
 
  // You can now capture the order on your server
})
	}


}
