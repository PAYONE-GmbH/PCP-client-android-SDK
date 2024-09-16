package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

// The credit card tokenizer errors that can occur during tokenization of a credit card.
sealed class CCTokenizerError : Exception() {
    data object LoadingScriptFailed : CCTokenizerError()
    data object PopulatingHTMLFailed : CCTokenizerError()
    data object InvalidResponse : CCTokenizerError()
}
