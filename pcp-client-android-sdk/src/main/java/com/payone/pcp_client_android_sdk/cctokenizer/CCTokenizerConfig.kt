package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import java.io.Serializable

// UI customization options
data class UIConfig(
    val formBgColor: String? = null,
    val fieldBgColor: String? = null,
    val fieldBorder: String? = null,
    val fieldOutline: String? = null,
    val fieldLabelColor: String? = null,
    val fieldPlaceholderColor: String? = null,
    val fieldTextColor: String? = null,
    val fieldErrorCodeColor: String? = null
) : Serializable

// Iframe config for the payment form
data class IframeConfig(
    val iframeWrapperId: String,
    val height: Int? = null,
    val width: Int? = null
) : Serializable

// Submit button config
data class SubmitButtonConfig(
    val selector: String? = null,
    val element: Any? = null // For Android, could be a View reference
) : Serializable

// The configuration object to set up the credit card tokenizer.
data class CreditcardTokenizerConfig(
    val iframe: IframeConfig? = null,
    val uiConfig: UIConfig? = null,
    val locale: String? = null,
    val submitButton: SubmitButtonConfig? = null,
    val tokenizationSuccessCallback: ((Int, String, Map<String, Any?>) -> Unit)? = null,
    val tokenizationFailureCallback: ((Int, Map<String, Any?>) -> Unit)? = null,
    val environment: String, // "test" or "live"
) : Serializable