package com.payone.pcpclientandroiddemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.payone.pcpclientandroiddemo.paypal.PayPalHandler
import com.paypal.android.paymentbuttons.PayPalButton

class PayPalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paypal)
        val payPalHandler = PayPalHandler(this)
        payPalHandler.initialize("YOUR_CLIENT_ID", "myapp://")
        val payPalButton = findViewById<PayPalButton>(R.id.paypal_button)
        payPalButton.setOnClickListener {
            payPalHandler.startPayment(orderId = "id")
        }
    }
}
