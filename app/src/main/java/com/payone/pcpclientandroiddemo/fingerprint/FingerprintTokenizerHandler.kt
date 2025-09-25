package com.payone.pcpclientandroiddemo.fingerprint

import android.content.Context
import android.util.Log
import android.widget.TextView
import com.payone.pcp_client_android_sdk.fingerprinttokenizer.FingerprintTokenizer
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment

class FingerprintTokenizerHandler(
    private val context: Context,
    private val tvSnippetToken: TextView
) {
    private val fingerprintTokenizer = FingerprintTokenizer(
        context = context,
        paylaPartnerId = "e7yeryF2of8X",
        partnerMerchantId = "test-1",
        environment = PCPEnvironment.Test
    )

    fun startTokenization() {
        fingerprintTokenizer.getSnippetToken {
            if (it.isSuccess) {
                val token = it.getOrNull() ?: ""
                Log.d("Success Token", token)
                tvSnippetToken.text = token
            } else {
                val exception = it.exceptionOrNull()
                Log.d("Failure Token", exception?.message ?: "unknown")
                tvSnippetToken.text = exception?.message
            }
        }
    }
}
