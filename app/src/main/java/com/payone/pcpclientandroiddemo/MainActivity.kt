package com.payone.pcpclientandroiddemo


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "PCP Client Android SDK Demo"

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
