package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import android.util.Base64
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
import java.io.Serializable
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// The credit card tokenizer request with the needed parameters for the SDK setup of the tokenizer.
data class CCTokenizerRequest(
    val mid: String,
    val aid: String,
    val portalId: String,
    val environment: PCPEnvironment,
    val generatedHash: String
) : Serializable {
    companion object {
        // Initializes the request and generates the hash.
        fun create(
            mid: String,
            aid: String,
            portalId: String,
            environment: PCPEnvironment,
            pmiPortalKey: String
        ): CCTokenizerRequest {
            val generatedHash = makeHash(
                environment,
                mid,
                aid,
                portalId,
                pmiPortalKey
            )
            return CCTokenizerRequest(mid, aid, portalId, environment, generatedHash)
        }

        // Function to generate the hash using the input values.
        private fun makeHash(
            environment: PCPEnvironment,
            mid: String,
            aid: String,
            portalId: String,
            pmiPortalKey: String
        ): String {
            val requestValues = listOf(
                aid,
                "3.11",
                "UTF-8",
                mid,
                environment.ccTokenizerIdentifier,
                portalId,
                "creditcardcheck",
                "JSON",
                "yes"
            )
            return createHash(requestValues.joinToString(""), pmiPortalKey)
        }

        // Creates an HMAC-SHA512 hash and encodes it in Base64.
        private fun createHash(string: String, secret: String): String {
            val stringToSign = "$string$secret"
            val secretKeySpec =
                SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")
            val mac = Mac.getInstance("HmacSHA512")
            mac.init(secretKeySpec)
            val hashBytes = mac.doFinal(stringToSign.toByteArray(StandardCharsets.UTF_8))
            return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        }
    }
}
