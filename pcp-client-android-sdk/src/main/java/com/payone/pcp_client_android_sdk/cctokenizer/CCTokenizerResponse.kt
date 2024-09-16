package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The credit card tokenizer response that consists of a status and either data or error entries.
@Serializable
data class CCTokenizerResponse(
    @SerialName("cardexpiredate") val cardExpireDate: String?,
    @SerialName("cardtype") val cardType: String?,
    @SerialName("errorcode") val errorCode: String?,
    @SerialName("errormessage") val errorMessage: String?,
    @SerialName("pseudocardpan") val pseudoCardpan: String?,
    @SerialName("status") val status: String,
    @SerialName("truncatedcardpan") val truncatedCardpan: String?
)
