package com.payone.pcp_client_android_sdk.fingerprinttokenizer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payone.pcp_client_android_sdk.utils.PCPEnvironment
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Robolectric tests for FingerprintTokenizer
 * These tests run on the JVM but simulate Android framework behavior
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // Target Android API level
class FingerprintTokenizerRobolectricTest {

    private lateinit var context: Context
    private lateinit var fingerprintTokenizer: FingerprintTokenizer

    private val testPaylaPartnerId = "test_partner_id"
    private val testPartnerMerchantId = "test_merchant_id"
    private val testSessionId = "test_session_123"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test FingerprintTokenizer can be instantiated with real context`() {
        // Given & When
        fingerprintTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Test,
            sessionId = testSessionId
        )

        // Then
        assertNotNull(fingerprintTokenizer)
    }

    @Test
    fun `test snippet token generation with custom session ID`() {
        // Given
        val customSessionId = "custom_session_abc123"
        fingerprintTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = "partner_abc",
            partnerMerchantId = "merchant_xyz",
            environment = PCPEnvironment.Production,
            sessionId = customSessionId
        )

        // When - Use reflection to access private field
        val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
        snippetTokenField.isAccessible = true
        val snippetToken = snippetTokenField.get(fingerprintTokenizer) as String

        // Then
        assertEquals("partner_abc_merchant_xyz_custom_session_abc123", snippetToken)
    }

    @Test
    fun `test snippet token generation without session ID creates UUID`() {
        // Given
        fingerprintTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Test,
            sessionId = null
        )

        // When
        val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
        snippetTokenField.isAccessible = true
        val snippetToken = snippetTokenField.get(fingerprintTokenizer) as String

        // Then
        assertTrue(snippetToken.startsWith("${testPaylaPartnerId}_${testPartnerMerchantId}_"))
        
        // Validate UUID-like format in the last part
        val uuidPart = snippetToken.substringAfterLast("_")
        assertTrue(uuidPart.isNotEmpty())
        assertTrue(uuidPart.length >= 32) // UUID without hyphens is 32 chars minimum
    }

    @Test
    fun `test makeHTML generates valid HTML structure`() {
        // Given
        fingerprintTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Test,
            sessionId = testSessionId
        )

        // When - Use reflection to call private method
        val makeHTMLMethod = FingerprintTokenizer::class.java.getDeclaredMethod("makeHTML")
        makeHTMLMethod.isAccessible = true
        val html = makeHTMLMethod.invoke(fingerprintTokenizer) as String

        // Then
        assertTrue(html.contains("<!doctype html>"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("<body>"))
        assertTrue(html.contains("</body>"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun `test makeScript generates correct JavaScript`() {
        // Given
        fingerprintTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = "partner_123",
            partnerMerchantId = "merchant_456",
            environment = PCPEnvironment.Test,
            sessionId = "session_789"
        )

        // When - Use reflection to call private method
        val makeScriptMethod = FingerprintTokenizer::class.java.getDeclaredMethod("makeScript")
        makeScriptMethod.isAccessible = true
        val script = makeScriptMethod.invoke(fingerprintTokenizer) as String

        // Then
        assertTrue(script.contains("window.paylaDcs"))
        assertTrue(script.contains("partner_123"))
        assertTrue(script.contains("merchant_456"))
        assertTrue(script.contains("d.payla.io/dcs"))
        assertTrue(script.contains("paylaDcs.init"))
        assertTrue(script.contains("session_789"))
    }

    @Test
    fun `test makeScript contains environment identifier`() {
        // Test with Test environment
        val testEnvTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Test,
            sessionId = testSessionId
        )

        val makeScriptMethod = FingerprintTokenizer::class.java.getDeclaredMethod("makeScript")
        makeScriptMethod.isAccessible = true
        val testScript = makeScriptMethod.invoke(testEnvTokenizer) as String

        // Check it contains the environment fingerprint identifier
        assertTrue(testScript.contains(PCPEnvironment.Test.fingerprintTokenizerIdentifier))

        // Test with Production environment
        val prodEnvTokenizer = FingerprintTokenizer(
            context = context,
            paylaPartnerId = testPaylaPartnerId,
            partnerMerchantId = testPartnerMerchantId,
            environment = PCPEnvironment.Production,
            sessionId = testSessionId
        )

        val prodScript = makeScriptMethod.invoke(prodEnvTokenizer) as String
        assertTrue(prodScript.contains(PCPEnvironment.Production.fingerprintTokenizerIdentifier))
    }

    @Test
    fun `test different partner IDs and merchant IDs are handled correctly`() {
        // Test various combinations
        val testCases = listOf(
            Triple("partner1", "merchant1", "session1"),
            Triple("123", "456", "789"),
            Triple("abc-def", "xyz-123", "test-session"),
        )

        testCases.forEach { (partnerId, merchantId, sessionId) ->
            val tokenizer = FingerprintTokenizer(
                context = context,
                paylaPartnerId = partnerId,
                partnerMerchantId = merchantId,
                environment = PCPEnvironment.Test,
                sessionId = sessionId
            )

            val snippetTokenField = FingerprintTokenizer::class.java.getDeclaredField("snippetToken")
            snippetTokenField.isAccessible = true
            val snippetToken = snippetTokenField.get(tokenizer) as String

            assertEquals("${partnerId}_${merchantId}_${sessionId}", snippetToken)
        }
    }
}
