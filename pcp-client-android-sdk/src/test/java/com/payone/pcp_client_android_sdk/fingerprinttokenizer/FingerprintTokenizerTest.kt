package com.payone.pcp_client_android_sdk.fingerprinttokenizer

import android.content.Context
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class FingerprintTokenizerTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var fingerprintTokenizer: FingerprintTokenizer

    private val testPaylaPartnerId = "test_partner_id"
    private val testPartnerMerchantId = "test_merchant_id"
    private val testSessionId = "test_session_123"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test FingerprintTokenizer initialization with custom session ID`() {
        // Given
        fingerprintTokenizer = FingerprintTokenizer(
            context = mockContext,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Test,
            sessionId = testSessionId
        )

        // When - Create a reflection test to access private snippetToken
        // (Note: In real scenarios, you might want to expose this via a public getter)
        val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
        snippetTokenField.isAccessible = true
        val snippetToken = snippetTokenField.get(fingerprintTokenizer) as String

        // Then
        val expectedToken = "${testPaylaPartnerId}_${testPartnerMerchantId}_$testSessionId"
        assertEquals(expectedToken, snippetToken)
    }

    @Test
    fun `test FingerprintTokenizer initialization without session ID generates UUID`() {
        // Given
        fingerprintTokenizer = FingerprintTokenizer(
            context = mockContext,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Production,
            sessionId = null
        )

        // When
        val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
        snippetTokenField.isAccessible = true
        val snippetToken = snippetTokenField.get(fingerprintTokenizer) as String

        // Then
        assertTrue(snippetToken.startsWith("${testPaylaPartnerId}_${testPartnerMerchantId}_"))
        
        // Extract UUID part
        val parts = snippetToken.split("_")
        assertTrue(parts.size >= 3)
        assertNotNull(parts.last())
    }

    @Test
    fun `test snippet token format is correct`() {
        // Given
        val customSessionId = "custom_uuid_12345"
        fingerprintTokenizer = FingerprintTokenizer(
            context = mockContext,
            paylaPartnerId = "partner123",
            partnerMerchantId = "merchant456",
            environment = PCPEnvironment.Test,
            sessionId = customSessionId
        )

        // When
        val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
        snippetTokenField.isAccessible = true
        val snippetToken = snippetTokenField.get(fingerprintTokenizer) as String

        // Then
        assertEquals("partner123_merchant456_custom_uuid_12345", snippetToken)
        assertTrue(snippetToken.contains("partner123"))
        assertTrue(snippetToken.contains("merchant456"))
        assertTrue(snippetToken.contains(customSessionId))
    }

    @Test
    fun `test different environments are accepted`() {
        // Test with Test environment
        val testEnvTokenizer = FingerprintTokenizer(
            context = mockContext,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Test,
            sessionId = testSessionId
        )
        assertNotNull(testEnvTokenizer)

        // Test with Production environment
        val prodEnvTokenizer = FingerprintTokenizer(
            context = mockContext,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Production,
            sessionId = testSessionId
        )
        assertNotNull(prodEnvTokenizer)
    }
}
