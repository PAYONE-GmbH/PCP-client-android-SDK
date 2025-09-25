package com.payone.pcpclientandroiddemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.google.android.gms.wallet.PaymentDataRequest
import com.payone.pcpclientandroiddemo.fingerprint.FingerprintTokenizerHandler
import com.payone.pcpclientandroiddemo.cctokenizer.CCTokenizerHandler
import com.payone.pcpclientandroiddemo.gpay.GooglePayHandler
import com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson

class MainActivity : AppCompatActivity() {
    private val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    private lateinit var googlePayHandler: GooglePayHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fingerprintBtn: Button = findViewById(R.id.fingerprint_button)
        val tvSnippetToken: TextView = findViewById(R.id.tvSnippetToken)
        val ccTokenizerBtn: Button = findViewById(R.id.btnCCTokenizer)

        val fingerprintHandler = FingerprintTokenizerHandler(this, tvSnippetToken)
        fingerprintBtn.setOnClickListener {
            fingerprintHandler.startTokenization()
        }

        val ccTokenizerHandler = CCTokenizerHandler(supportFragmentManager)
        ccTokenizerBtn.setOnClickListener {
            ccTokenizerHandler.startTokenization()
        }

        // Set up Google Pay Handler (PaymentsClient is now created inside the handler)
        googlePayHandler = GooglePayHandler(this, LOAD_PAYMENT_DATA_REQUEST_CODE)

        val composeView = findViewById<ComposeView>(R.id.compose_google_pay)
        composeView.setContent {
            val startPayment = remember { mutableStateOf(false) }
            if (startPayment.value) {
                startPayment.value = false
                googlePayHandler.launchGooglePay()
            }
            GooglePayButton(onClick = { startPayment.value = true })
        }
    }

    // ...existing code...

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            googlePayHandler.handleGooglePayResult(resultCode, data)
        }
    }
}
