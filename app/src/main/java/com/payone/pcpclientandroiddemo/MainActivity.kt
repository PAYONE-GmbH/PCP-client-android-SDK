package com.payone.pcpclientandroiddemo


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "PCP Client Android SDK Demo"
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
