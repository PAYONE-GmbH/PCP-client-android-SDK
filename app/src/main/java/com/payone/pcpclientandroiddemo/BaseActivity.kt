package com.payone.pcpclientandroiddemo

import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

open class BaseActivity : AppCompatActivity() {
    var headerTitle: String? = null
    private var checkoutAction: (() -> Unit)? = null
    private val networkPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = checkoutAction
        checkoutAction = null
        if (granted) {
            action?.invoke()
        } else {
            Toast.makeText(this, "Local network access is required for your development backend. Allow Nearby devices in app permissions, then retry.", Toast.LENGTH_LONG).show()
        }
    }

    /** Android 17 gates LAN sockets separately from INTERNET, including native Wallet services. */
    protected fun withCheckoutNetworkAccess(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") != PackageManager.PERMISSION_GRANTED) {
            checkoutAction = action
            networkPermission.launch("android.permission.ACCESS_LOCAL_NETWORK")
        } else {
            action()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headerTitle?.let {
            supportActionBar?.title = it
        }
        if (!isTaskRoot) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onContentChanged() {
        super.onContentChanged()
        // AppCompat fits the action-bar decor; reserve the remaining system/IME
        // insets on the content container without changing individual screens.
        val content = findViewById<View>(android.R.id.content) ?: return
        val left = content.paddingLeft
        val top = content.paddingTop
        val right = content.paddingRight
        val bottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
