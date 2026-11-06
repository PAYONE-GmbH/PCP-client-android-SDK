package com.payone.pcpclientandroiddemo.gpay.express

import com.payone.pcpclientandroiddemo.gpay.checkout.CheckoutMode
import com.payone.pcpclientandroiddemo.gpay.checkout.GooglePayCheckoutActivity

/** Express: delivery/pricing and authorization use native Wallet callbacks. */
class GooglePayExpressActivity : GooglePayCheckoutActivity() {
    override val mode = CheckoutMode.EXPRESS
}
