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
import com.paypal.android.paymentbuttons.PayPalButton
import com.payone.pcpclientandroiddemo.gpay.GooglePayHandler
import com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson
import com.payone.pcpclientandroiddemo.paypal.PayPalHandler

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fingerprintBtn = findViewById<Button>(R.id.fingerprint_button)
        val ccTokenizerBtn = findViewById<Button>(R.id.btnCCTokenizer)
        val googlePayBtn = findViewById<Button>(R.id.btnGooglePay)
        val payPalBtn = findViewById<Button>(R.id.btnPayPal)

        fingerprintBtn.setOnClickListener {
            startActivity(Intent(this, FingerprintActivity::class.java))
        }
        ccTokenizerBtn.setOnClickListener {
            startActivity(Intent(this, CCTokenizerActivity::class.java))
        }
        googlePayBtn.setOnClickListener {
            startActivity(Intent(this, GooglePayActivity::class.java))
        }
        payPalBtn.setOnClickListener {
            startActivity(Intent(this, PayPalActivity::class.java))
        }
    }
}
