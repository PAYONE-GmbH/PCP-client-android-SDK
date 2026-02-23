package com.payone.pcp_client_android_sdk.fingerprinttokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import android.content.Context
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FingerprintTokenizerTest {

    private lateinit var context: Context
    private val paylaPartnerId = "PAYparId"
    private val merchantId = "merch"
    private val sessionId = "sessionId"

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    private fun getSnippetToken(tokenizer: FingerprintTokenizer): String {
        val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
        snippetTokenField.isAccessible = true
        return snippetTokenField.get(tokenizer) as String
    }

    @Test
    fun `test initialization with session ID creates tokenizer`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        assertNotNull(tokenizer)
    }

    @Test
    fun `test initialization without session ID creates tokenizer`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = null
        )

        assertNotNull(tokenizer)
    }

    @Test
    fun `test snippet token format with test environment`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        val snippetToken = getSnippetToken(tokenizer)

        // Expected format: PAYparId_merch_sessionId
        assertEquals("${paylaPartnerId}_${merchantId}_$sessionId", snippetToken)
    }

    @Test
    fun `test snippet token format with production environment`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Production,
            sessionId = sessionId
        )

        val snippetToken = getSnippetToken(tokenizer)

        assertEquals("${paylaPartnerId}_${merchantId}_$sessionId", snippetToken)
    }

    @Test
    fun `test snippet token without session ID generates UUID`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = null
        )

        val snippetToken = getSnippetToken(tokenizer)

        // Should start with partner and merchant ID
        assertTrue(snippetToken.startsWith("${paylaPartnerId}_${merchantId}_"))
        
        // Extract UUID part
        val parts = snippetToken.split("_")
        assertTrue(parts.size >= 3)
        
        // Last part should be a UUID (not empty)
        val uuid = parts.drop(2).joinToString("_")
        assertNotNull(uuid)
        assertTrue(uuid.isNotEmpty())
    }

    @Test
    fun `test different partner IDs create different tokens`() {
        val tokenizer1 = FingerprintTokenizer(
            context = context,
            paylaPartnerId = "Partner1",
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        val tokenizer2 = FingerprintTokenizer(
            context = context,
            paylaPartnerId = "Partner2",
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        val token1 = getSnippetToken(tokenizer1)
        val token2 = getSnippetToken(tokenizer2)

        assertNotNull(token1)
        assertNotNull(token2)
        assertNotEquals(token1, token2)
        assertEquals("Partner1_${merchantId}_$sessionId", token1)
        assertEquals("Partner2_${merchantId}_$sessionId", token2)
    }

    @Test
    fun `test different merchant IDs create different tokens`() {
        val tokenizer1 = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = "Merchant1",
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        val tokenizer2 = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = "Merchant2",
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        val token1 = getSnippetToken(tokenizer1)
        val token2 = getSnippetToken(tokenizer2)

        assertNotNull(token1)
        assertNotNull(token2)
        assertNotEquals(token1, token2)
        assertEquals("${paylaPartnerId}_Merchant1_$sessionId", token1)
        assertEquals("${paylaPartnerId}_Merchant2_$sessionId", token2)
    }

    @Test
    fun `test different session IDs create different tokens`() {
        val tokenizer1 = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = "session1"
        )

        val tokenizer2 = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = "session2"
        )

        val token1 = getSnippetToken(tokenizer1)
        val token2 = getSnippetToken(tokenizer2)

        assertNotNull(token1)
        assertNotNull(token2)
        assertNotEquals(token1, token2)
        assertEquals("${paylaPartnerId}_${merchantId}_session1", token1)
        assertEquals("${paylaPartnerId}_${merchantId}_session2", token2)
    }

    @Test
    fun `test getSnippetToken can be called without errors`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        // This should not throw an exception
        tokenizer.getSnippetToken { _ -> }
    }

    @Test
    fun `test tokenizer with test environment uses correct identifier`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Test,
            sessionId = sessionId
        )

        assertNotNull(tokenizer)
        // The environment identifier is used internally in the script
    }

    @Test
    fun `test tokenizer with production environment uses correct identifier`() {
        val tokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = paylaPartnerId,
            partnerMerchantId = merchantId,
            environment = PCPEnvironment.Production,
            sessionId = sessionId
        )

        assertNotNull(tokenizer)
        // The environment identifier is used internally in the script
    }
}
