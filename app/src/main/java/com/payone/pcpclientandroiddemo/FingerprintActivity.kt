package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.payone.pcpclientandroiddemo.fingerprint.FingerprintTokenizerHandler

class FingerprintActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)
        val tvSnippetToken = findViewById<TextView>(R.id.tvSnippetToken)
        val fingerprintHandler = FingerprintTokenizerHandler(this, tvSnippetToken)
        val btnStart = findViewById<Button>(R.id.btnStartFingerprint)
        btnStart.setOnClickListener {
            fingerprintHandler.startTokenization()
        }
    }
}
