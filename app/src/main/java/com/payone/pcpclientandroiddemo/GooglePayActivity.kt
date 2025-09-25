package com.payone.pcpclientandroiddemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.payone.pcpclientandroiddemo.gpay.GooglePayHandler

class GooglePayActivity : AppCompatActivity() {
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    private lateinit var googlePayHandler: GooglePayHandler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_pay)

        val composeView = findViewById<ComposeView>(R.id.compose_google_pay)
        googlePayHandler = GooglePayHandler(this, LOAD_PAYMENT_DATA_REQUEST_CODE)

        composeView.setContent {
            val startPayment = remember { mutableStateOf(false) }
            if (startPayment.value) {
                startPayment.value = false
                googlePayHandler.launchGooglePay()
            }
            GooglePayButton(onClick = { startPayment.value = true })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            googlePayHandler.handleGooglePayResult(resultCode, data)
        }
    }
}
