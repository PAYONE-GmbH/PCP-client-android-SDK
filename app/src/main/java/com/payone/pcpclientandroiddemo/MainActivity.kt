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
import com.payone.pcpclientandroiddemo.gpay.checkout.BackendSettings


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
        val settings = BackendSettings(this)
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            hint = getString(R.string.backend_url_hint)
            setText(settings.configuration().backendUrl)
            setSelectAllOnFocus(true)
        }
        val padding = (20 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            addView(TextView(this@MainActivity).apply { setText(R.string.backend_settings_help) })
            addView(input)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.backend_settings_title)
            .setView(ScrollView(this).apply { addView(content) })
            .setPositiveButton(R.string.backend_save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.backend_reset) { _, _ -> settings.reset() }
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                try {
                    settings.save(input.text.toString())
                    Toast.makeText(this, R.string.backend_saved, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (_: IllegalArgumentException) {
                    input.error = getString(R.string.backend_url_invalid)
                }
            }
        }
        dialog.show()
    }
}
