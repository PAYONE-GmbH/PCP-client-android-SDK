package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.payone.pcpclientandroiddemo.cctokenizer.CCTokenizerHandler

class CCTokenizerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cc_tokenizer)
        val ccTokenizerHandler = CCTokenizerHandler(supportFragmentManager)
        val btnStart = findViewById<Button>(R.id.btnStartCCTokenizer)
        btnStart.setOnClickListener {
            ccTokenizerHandler.startTokenization()
        }
    }
}
