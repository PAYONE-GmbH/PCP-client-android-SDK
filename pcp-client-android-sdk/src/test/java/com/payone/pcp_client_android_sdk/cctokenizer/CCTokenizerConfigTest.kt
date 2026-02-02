package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import org.junit.Assert.*
import org.junit.Test

class CCTokenizerConfigTest {

    // MARK: - FontStyle Tests

    @Test
    fun `test FontStyle initialization`() {
        val fontStyle = FontStyle(
            fontSize = "16px",
            fontWeight = "bold",
            fontSizeMobile = "14px"
        )

        assertEquals("16px", fontStyle.fontSize)
        assertEquals("bold", fontStyle.fontWeight)
        assertEquals("14px", fontStyle.fontSizeMobile)
    }

    @Test
    fun `test FontStyle initialization with null values`() {
        val fontStyle = FontStyle()

        assertNull(fontStyle.fontSize)
        assertNull(fontStyle.fontWeight)
        assertNull(fontStyle.fontSizeMobile)
    }

    // MARK: - UIConfig Tests

    @Test
    fun `test UIConfig initialization`() {
        val labelStyle = FontStyle(fontSize = "14px")
        val uiConfig = UIConfig(
            formBgColor = "#fff",
            formMarginLeft = "10px",
            fieldBgColor = "#f0f0f0",
            labelStyle = labelStyle
        )

        assertEquals("#fff", uiConfig.formBgColor)
        assertEquals("10px", uiConfig.formMarginLeft)
        assertEquals("#f0f0f0", uiConfig.fieldBgColor)
        assertNotNull(uiConfig.labelStyle)
        assertEquals("14px", uiConfig.labelStyle?.fontSize)
    }

    @Test
    fun `test UIConfig initialization with multiple properties`() {
        val uiConfig = UIConfig(
            formBgColor = "#fff",
            btnBgColor = "#007aff",
            btnTextColor = "#ffffff",
            fieldBorder = "1px solid #ccc",
            fontFamily = "Arial"
        )

        assertEquals("#fff", uiConfig.formBgColor)
        assertEquals("#007aff", uiConfig.btnBgColor)
        assertEquals("#ffffff", uiConfig.btnTextColor)
        assertEquals("1px solid #ccc", uiConfig.fieldBorder)
        assertEquals("Arial", uiConfig.fontFamily)
    }

    // MARK: - IframeConfig Tests

    @Test
    fun `test IframeConfig initialization`() {
        val config = IframeConfig(
            iframeWrapperId = "payment-frame",
            height = 400,
            width = 600,
            zIndex = 1000
        )

        assertEquals("payment-frame", config.iframeWrapperId)
        assertEquals(400, config.height)
        assertEquals(600, config.width)
        assertEquals(1000, config.zIndex)
    }

    @Test
    fun `test IframeConfig initialization with null optional values`() {
        val config = IframeConfig(
            iframeWrapperId = "payment-frame"
        )

        assertEquals("payment-frame", config.iframeWrapperId)
        assertNull(config.height)
        assertNull(config.width)
        assertNull(config.zIndex)
    }

    // MARK: - SubmitButtonConfig Tests

    @Test
    fun `test SubmitButtonConfig initialization`() {
        val config = SubmitButtonConfig(selector = "#submit-btn", element = null)

        assertEquals("#submit-btn", config.selector)
        assertNull(config.element)
    }

    @Test
    fun `test SubmitButtonConfig initialization with null values`() {
        val config = SubmitButtonConfig()

        assertNull(config.selector)
        assertNull(config.element)
    }

    // MARK: - Error Configuration Tests

    @Test
    fun `test CardNumberErrors initialization`() {
        val errors = CardNumberErrors(
            isRequired = "Required field",
            isInvalid = "Invalid input",
            isTooShort = "Too short",
            notSupported = "Not supported"
        )

        assertEquals("Required field", errors.isRequired)
        assertEquals("Invalid input", errors.isInvalid)
        assertEquals("Too short", errors.isTooShort)
        assertEquals("Not supported", errors.notSupported)
    }

    @Test
    fun `test CardholderNameErrors initialization`() {
        val errors = CardholderNameErrors(
            isRequired = "Name is required",
            isInvalid = "Invalid name"
        )

        assertEquals("Name is required", errors.isRequired)
        assertEquals("Invalid name", errors.isInvalid)
    }

    @Test
    fun `test ExpiryDateErrors initialization`() {
        val errors = ExpiryDateErrors(
            isRequired = "Date is required",
            isInvalid = "Invalid date"
        )

        assertEquals("Date is required", errors.isRequired)
        assertEquals("Invalid date", errors.isInvalid)
    }

    @Test
    fun `test SecurityCodeErrors initialization`() {
        val errors = SecurityCodeErrors(
            isRequired = "CVV is required",
            amexCardSecurityCodeError = "Amex CVV error",
            generalSecurityCodeError = "CVV error"
        )

        assertEquals("CVV is required", errors.isRequired)
        assertEquals("Amex CVV error", errors.amexCardSecurityCodeError)
        assertEquals("CVV error", errors.generalSecurityCodeError)
    }

    // MARK: - LocaleText Tests

    @Test
    fun `test LocaleTextLabels initialization`() {
        val labels = LocaleTextLabels(
            cardNumber = "Card Number",
            cardholderName = "Cardholder Name",
            expiryDate = "Expiry Date",
            securityCode = "CVV"
        )

        assertEquals("Card Number", labels.cardNumber)
        assertEquals("Cardholder Name", labels.cardholderName)
        assertEquals("Expiry Date", labels.expiryDate)
        assertEquals("CVV", labels.securityCode)
    }

    @Test
    fun `test LocaleTextPlaceholders initialization`() {
        val placeholders = LocaleTextPlaceholders(
            cardNumber = "1234 5678 9012 3456",
            cardholderName = "John Doe",
            expiryDate = "MM/YY",
            securityCode = "123"
        )

        assertEquals("1234 5678 9012 3456", placeholders.cardNumber)
        assertEquals("John Doe", placeholders.cardholderName)
        assertEquals("MM/YY", placeholders.expiryDate)
        assertEquals("123", placeholders.securityCode)
    }

    @Test
    fun `test LocaleTextAriaLabels initialization`() {
        val ariaLabels = LocaleTextAriaLabels(
            cardNumber = "Enter card number",
            cardholderName = "Enter cardholder name",
            expiryDate = "Enter expiry date",
            securityCode = "Enter security code"
        )

        assertEquals("Enter card number", ariaLabels.cardNumber)
        assertEquals("Enter cardholder name", ariaLabels.cardholderName)
        assertEquals("Enter expiry date", ariaLabels.expiryDate)
        assertEquals("Enter security code", ariaLabels.securityCode)
    }

    @Test
    fun `test LocaleTextErrors initialization`() {
        val cardNumberErrors = CardNumberErrors(isRequired = "Required")
        val cardholderNameErrors = CardholderNameErrors(isInvalid = "Invalid")
        val expiryDateErrors = ExpiryDateErrors(isRequired = "Required")
        val securityCodeErrors = SecurityCodeErrors(isRequired = "Required")

        val localeTextErrors = LocaleTextErrors(
            cardNumber = cardNumberErrors,
            cardholderName = cardholderNameErrors,
            expiryDate = expiryDateErrors,
            securityCode = securityCodeErrors
        )

        assertEquals("Required", localeTextErrors.cardNumber?.isRequired)
        assertEquals("Invalid", localeTextErrors.cardholderName?.isInvalid)
        assertEquals("Required", localeTextErrors.expiryDate?.isRequired)
        assertEquals("Required", localeTextErrors.securityCode?.isRequired)
    }

    @Test
    fun `test LocaleTextConfig initialization`() {
        val labels = LocaleTextLabels(cardNumber = "Card Number")
        val placeholders = LocaleTextPlaceholders(cardNumber = "1234 5678")
        val ariaLabels = LocaleTextAriaLabels(cardNumber = "Enter card")
        val errors = LocaleTextErrors(cardNumber = CardNumberErrors(isRequired = "Required"))

        val config = LocaleTextConfig(
            labels = labels,
            placeholders = placeholders,
            arialabels = ariaLabels,
            errors = errors
        )

        assertEquals("Card Number", config.labels?.cardNumber)
        assertEquals("1234 5678", config.placeholders?.cardNumber)
        assertEquals("Enter card", config.arialabels?.cardNumber)
        assertEquals("Required", config.errors?.cardNumber?.isRequired)
    }

    // MARK: - CardDetails Tests

    @Test
    fun `test CardDetails initialization`() {
        val cardDetails = CardDetails(
            cardholderName = "John Doe",
            cardNumber = "1234",
            expiryDate = "12/25",
            cardType = "visa"
        )

        assertEquals("John Doe", cardDetails.cardholderName)
        assertEquals("1234", cardDetails.cardNumber)
        assertEquals("12/25", cardDetails.expiryDate)
        assertEquals("visa", cardDetails.cardType)
    }

    // MARK: - CreditcardTokenizerConfig Tests

    @Test
    fun `test CreditcardTokenizerConfig initialization with required fields only`() {
        var successCalled = false
        var failureCalled = false

        val config = CreditcardTokenizerConfig(
            iframe = IframeConfig(iframeWrapperId = "payment-frame"),
            token = "test-token",
            submitButton = SubmitButtonConfig(selector = "#submit"),
            tokenizationSuccessCallback = { _, _, _, _ -> successCalled = true },
            tokenizationFailureCallback = { _, _ -> failureCalled = true }
        )

        assertEquals("payment-frame", config.iframe.iframeWrapperId)
        assertEquals("test-token", config.token)
        assertEquals("#submit", config.submitButton.selector)
        assertNull(config.uiConfig)
        assertNull(config.locale)
        assertNull(config.mode)
        assertNull(config.allowedCardSchemes)
        assertNull(config.customTextConfig)

        // Test callbacks
        config.tokenizationSuccessCallback(200, "token", CardDetails("John", "1234", "12/25", "visa"), "manual")
        assertTrue(successCalled)

        config.tokenizationFailureCallback(500, mapOf("error" to "Failed"))
        assertTrue(failureCalled)
    }

    @Test
    fun `test CreditcardTokenizerConfig initialization with all fields`() {
        val uiConfig = UIConfig(formBgColor = "#fff")
        val customTextConfig = mapOf(
            "de_DE" to LocaleTextConfig(
                labels = LocaleTextLabels(cardNumber = "Kartennummer")
            )
        )

        val config = CreditcardTokenizerConfig(
            iframe = IframeConfig(iframeWrapperId = "payment-frame", height = 400, width = 600),
            uiConfig = uiConfig,
            locale = "de_DE",
            token = "test-token",
            mode = "test",
            allowedCardSchemes = listOf("visa", "mastercard"),
            customTextConfig = customTextConfig,
            submitButton = SubmitButtonConfig(selector = "#submit"),
            tokenizationSuccessCallback = { _, _, _, _ -> },
            tokenizationFailureCallback = { _, _ -> }
        )

        assertEquals("payment-frame", config.iframe.iframeWrapperId)
        assertEquals(400, config.iframe.height)
        assertEquals(600, config.iframe.width)
        assertEquals("#fff", config.uiConfig?.formBgColor)
        assertEquals("de_DE", config.locale)
        assertEquals("test-token", config.token)
        assertEquals("test", config.mode)
        assertEquals(listOf("visa", "mastercard"), config.allowedCardSchemes)
        assertEquals("Kartennummer", config.customTextConfig?.get("de_DE")?.labels?.cardNumber)
    }
}
