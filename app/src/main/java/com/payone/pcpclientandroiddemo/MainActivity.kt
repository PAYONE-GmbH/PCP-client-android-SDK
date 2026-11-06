package com.payone.pcpclientandroiddemo


import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.payone.pcpclientandroiddemo.gpay.ConfigService


class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        headerTitle = "PCP Client Android SDK Demo"
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnBackendSettings).setOnClickListener { showBackendSettings() }


        val fingerprintBtn = findViewById<Button>(R.id.fingerprint_button)
        val ccTokenizerBtn = findViewById<Button>(R.id.btnCCTokenizer)
        val googlePayBtn = findViewById<Button>(R.id.btnGooglePay)
        findViewById<Button>(R.id.btnGooglePayExpress).setOnClickListener {
            startActivity(Intent(this, com.payone.pcpclientandroiddemo.gpay.express.GooglePayExpressActivity::class.java))
        }
        val payPalBtn = findViewById<Button>(R.id.btnPayPal)

        fingerprintBtn.setOnClickListener {
            startActivity(Intent(this, FingerprintActivity::class.java))
        }
        ccTokenizerBtn.setOnClickListener {
            startActivity(
                CCTokenizerActivity.createIntent(
                    context = this,
                    merchantToken = "", // Replace with your current merchant token.
                )
            )
        }
        googlePayBtn.setOnClickListener {
            startActivity(Intent(this, GooglePayActivity::class.java))
        }
        payPalBtn.setOnClickListener {
            startActivity(Intent(this, PayPalActivity::class.java))
        }
    }

    private fun showBackendSettings() {
        val prefs = getSharedPreferences("demo-backend", MODE_PRIVATE)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            hint = getString(R.string.backend_url_hint)
            setText(prefs.getString("url", BuildConfig.BACKEND_BASE_URL))
            setSelectAllOnFocus(true)
        }
        val merchant = EditText(this).apply {
            hint = "PAYONE merchant ID (public gatewayMerchantId)"
            setSingleLine(true)
            setText(prefs.getString("merchantId", BuildConfig.GATEWAY_MERCHANT_ID))
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(this@MainActivity).apply { setText(R.string.backend_settings_help) })
            addView(input)
            addView(merchant)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Checkout settings")
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(R.string.backend_save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Clear settings") { _, _ -> prefs.edit().clear().apply() }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    val config = ConfigService(input.text.toString(), merchant.text.toString().trim())
                    check(prefs.edit().putString("url", config.backendUrl).putString("merchantId", config.gatewayMerchantId).commit())
                    Toast.makeText(this, R.string.backend_saved, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (error: IllegalArgumentException) {
                    input.error = error.message
                }
            }
        }
        dialog.show()
    }
}
