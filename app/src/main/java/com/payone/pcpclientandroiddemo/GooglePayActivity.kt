package com.payone.pcpclientandroiddemo

import com.payone.pcpclientandroiddemo.gpay.checkout.CheckoutMode
import com.payone.pcpclientandroiddemo.gpay.checkout.GooglePayCheckoutActivity

/** Standard: shop-selected delivery; payment data is handled after the sheet. */
class GooglePayActivity : GooglePayCheckoutActivity() {
    override val mode = CheckoutMode.STANDARD
}
