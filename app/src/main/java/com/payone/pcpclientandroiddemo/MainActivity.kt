package com.payone.pcpclientandroiddemo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.payone.pcp_client_android_sdk.cctokenizer.CCTokenizerActivity
import com.payone.pcp_client_android_sdk.fingerprint_tokenizer.FingerprintTokenizer
import com.payone.pcp_client_android_sdk.fingerprint_tokenizer.PCPEnvironment

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fingerprintBtn: Button = findViewById(R.id.fingerprint_button)
        val tvSnippetToken: TextView = findViewById(R.id.tvSnippetToken)
        val ccTokenizerBtn: Button = findViewById(R.id.btnCCTokenizer)

        val fingerprintTokenizer = FingerprintTokenizer(
            context = this,
            paylaPartnerId = "e7yeryF2of8X",
            partnerMerchantId = "test-1",
            environment = PCPEnvironment.test
        )

        fingerprintBtn.setOnClickListener {
            fingerprintTokenizer.getSnippetToken {
                if (it.isSuccess) {
                    val success = it.getOrNull() ?: ""
                    Log.d("Success Token", success)
                    tvSnippetToken.text = success
                } else {
                    val exception = it.exceptionOrNull()
                    Log.d("Failure Token", exception?.message ?: "unknown")
                    tvSnippetToken.text = exception?.message
                }
            }
        }

        ccTokenizerBtn.setOnClickListener {
            val intent = Intent(this, CCTokenizerActivity::class.java)
            startActivity(intent)
            finish()
        }


    }
}