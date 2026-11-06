package com.payone.pcpclientandroiddemo

import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerFragment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CCTokenizerActivityTest {
    @Test
    fun `status updates keep focus on the card form`() {
        val intent = CCTokenizerActivity.createIntent(RuntimeEnvironment.getApplication(), "merchant-token")
        val controller = Robolectric.buildActivity(CCTokenizerActivity::class.java, intent).setup().visible()
        try {
            val activity = controller.get()
            activity.supportFragmentManager.executePendingTransactions()
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as
                CreditcardTokenizerFragment
            // Use a real numeric editor in the form container: Robolectric does not
            // implement WebView's Chromium input connection.
            val cardNumber = EditText(activity).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
            }
            (fragment.requireView() as ViewGroup).addView(cardNumber,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100))
            val root = activity.window.decorView
            fun layout() {
                root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1800, View.MeasureSpec.EXACTLY))
                root.layout(0, 0, 1080, 1800)
            }
            layout()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(cardNumber.requestFocus())
            assertTrue(cardNumber.hasFocus())
            repeat(20) {
                fragment.onStatusChanged!!.invoke("Payment form status $it")
                layout()
                shadowOf(Looper.getMainLooper()).idle()
                assertTrue("Status output must not change the active input", cardNumber.hasFocus())
            }
            assertTrue(activity.findViewById<TextView>(R.id.tokenizerOutput).text.contains("Payment form status 19"))
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `token parameter starts form once and recreation retains it`() {
        val intent = CCTokenizerActivity.createIntent(RuntimeEnvironment.getApplication(), "  merchant-token  ")
        val controller = Robolectric.buildActivity(CCTokenizerActivity::class.java, intent).setup()
        try {
            val activity = controller.get()
            activity.supportFragmentManager.executePendingTransactions()
            assertEquals("merchant-token", activity.findViewById<EditText>(R.id.tokenizerMerchantToken).text.toString())
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!
            @Suppress("DEPRECATION")
            val config = fragment.requireArguments().getSerializable("config") as
                com.payone.pcp_client_android_sdk.cctokenizer.CreditcardTokenizerConfig
            assertEquals("merchant-token", config.token)
            controller.recreate()
            controller.get().supportFragmentManager.executePendingTransactions()
            assertEquals(1, controller.get().supportFragmentManager.fragments.size)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun `missing or blank parameter leaves manual token entry available`() {
        listOf(null, "  ").forEach { token ->
            val intent = CCTokenizerActivity.createIntent(RuntimeEnvironment.getApplication(), token)
            val controller = Robolectric.buildActivity(CCTokenizerActivity::class.java, intent).setup()
            try {
                val activity = controller.get()
                activity.supportFragmentManager.executePendingTransactions()
                assertNull(activity.supportFragmentManager.findFragmentById(R.id.fragment_container))
                activity.findViewById<EditText>(R.id.tokenizerMerchantToken).setText("replacement-token")
                activity.findViewById<Button>(R.id.btnStartCCTokenizer).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                assertNotNull(activity.supportFragmentManager.findFragmentById(R.id.fragment_container))
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }
}
