package com.payone.pcpclientandroiddemo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.pay.button.ButtonTheme
import com.google.pay.button.PayButton
import com.payone.pcpclientandroiddemo.gpay.GooglePayRequestJson

@Composable
fun GooglePayButton(onClick: () -> Unit) {
    val allowedPaymentMethods = GooglePayRequestJson.allowedPaymentMethods
    Box(modifier = Modifier.fillMaxWidth()) {
        PayButton(
            onClick = onClick,
            allowedPaymentMethods = allowedPaymentMethods,
            theme = ButtonTheme.Dark
        )
    }
}
