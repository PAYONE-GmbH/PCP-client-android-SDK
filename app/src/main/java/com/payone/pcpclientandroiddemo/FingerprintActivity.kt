package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.payone.pcp_client_android_sdk.fingerprinttokenizer.FingerprintTokenizer
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment

class FingerprintActivity : AppCompatActivity() {
    private lateinit var fingerprintTokenizer: FingerprintTokenizer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint)
        val tvSnippetToken = findViewById<TextView>(R.id.tvSnippetToken)
        val btnStart = findViewById<Button>(R.id.btnStartFingerprint)
        fingerprintTokenizer = FingerprintTokenizer(
            context = this,
            paylaPartnerId = "e7yeryF2of8X",
            partnerMerchantId = "test-1",
            environment = PCPEnvironment.Test
        )
        btnStart.setOnClickListener {
            fingerprintTokenizer.getSnippetToken {
                if (it.isSuccess) {
                    val token = it.getOrNull() ?: ""
                    android.util.Log.d("Success Token", token)
                    tvSnippetToken.text = token
                } else {
                    val exception = it.exceptionOrNull()
                    android.util.Log.d("Failure Token", exception?.message ?: "unknown")
                    tvSnippetToken.text = exception?.message
                }
            }
        }
    }
}
