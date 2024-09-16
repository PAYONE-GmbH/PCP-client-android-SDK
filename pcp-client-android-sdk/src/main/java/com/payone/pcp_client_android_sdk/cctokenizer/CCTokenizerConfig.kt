package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import java.io.Serializable

// A customizable field configuration for the required fields that will be injected into the website of the credit card tokenizer.
data class Field(
    val selector: String,
    val style: String?,
    val type: String,
    val size: String?,
    val maxlength: String?,
    val length: Map<String, Int>?,
    val iframe: Pair<String, String>?
) : Serializable

// The language of the credit card tokenizer. Currently, English or German is available.
enum class PayoneLanguage(val configValue: String) {
    English("Payone.ClientApi.Language.en"),
    German("Payone.ClientApi.Language.de")
}

// The configuration object to set up the credit card tokenizer.
data class CreditcardTokenizerConfig(
    val cardPan: Field,
    val cardCvc2: Field,
    val cardExpireMonth: Field,
    val cardExpireYear: Field,
    val defaultStyles: Map<String, String>,
    val language: PayoneLanguage,
    val error: String,
    val submitButtonId: String,
    val creditCardCheckCallback: (Result<CCTokenizerResponse>) -> Unit
) : Serializable