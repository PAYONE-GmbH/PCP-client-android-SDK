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

    // MARK: - CustomIconsConfig Tests (v1.4)

    @Test
    fun `test CustomIconsConfig initialization with all fields`() {
        val config = CustomIconsConfig(
            useCustomValidationIcons = true,
            showCardBrandIcons = false,
            successIcon = "/valid.svg",
            errorIcon = "/invalid.svg"
        )

        assertTrue(config.useCustomValidationIcons)
        assertFalse(config.showCardBrandIcons)
        assertEquals("/valid.svg", config.successIcon)
        assertEquals("/invalid.svg", config.errorIcon)
    }

    @Test
    fun `test CustomIconsConfig default values`() {
        val config = CustomIconsConfig()

        assertFalse(config.useCustomValidationIcons)
        assertFalse(config.showCardBrandIcons)
        assertNull(config.successIcon)
        assertNull(config.errorIcon)
    }

    @Test
    fun `test CustomIconsConfig showCardBrandIcons enabled`() {
        val config = CustomIconsConfig(
            useCustomValidationIcons = true,
            showCardBrandIcons = true,
            successIcon = "/valid.png",
            errorIcon = "/invalid.png"
        )

        assertTrue(config.useCustomValidationIcons)
        assertTrue(config.showCardBrandIcons)
        assertEquals("/valid.png", config.successIcon)
        assertEquals("/invalid.png", config.errorIcon)
    }

    // MARK: - CTPConfig Tests (v1.4)

    @Test
    fun `test VisaConfig initialization`() {
        val visaConfig = VisaConfig(
            srcInitiatorId = "PAYONE-VISA-UUID",
            srcDpaId = "MERCHANT-UUID",
            encryptionKey = "ENC-KEY",
            nModulus = "MODULUS"
        )

        assertEquals("PAYONE-VISA-UUID", visaConfig.srcInitiatorId)
        assertEquals("MERCHANT-UUID", visaConfig.srcDpaId)
        assertEquals("ENC-KEY", visaConfig.encryptionKey)
        assertEquals("MODULUS", visaConfig.nModulus)
    }

    @Test
    fun `test MastercardConfig initialization`() {
        val mastercardConfig = MastercardConfig(
            srcInitiatorId = "PAYONE-MC-UUID",
            srcDpaId = "MERCHANT-UUID"
        )

        assertEquals("PAYONE-MC-UUID", mastercardConfig.srcInitiatorId)
        assertEquals("MERCHANT-UUID", mastercardConfig.srcDpaId)
    }

    @Test
    fun `test SchemeConfig initialization with both schemes`() {
        val visaConfig = VisaConfig("v-init", "v-dpa", "enc", "mod")
        val mastercardConfig = MastercardConfig("mc-init", "mc-dpa")
        val schemeConfig = SchemeConfig(
            merchantPresentationName = "MyMerchant",
            visaConfig = visaConfig,
            mastercardConfig = mastercardConfig
        )

        assertEquals("MyMerchant", schemeConfig.merchantPresentationName)
        assertNotNull(schemeConfig.visaConfig)
        assertNotNull(schemeConfig.mastercardConfig)
    }

    @Test
    fun `test SchemeConfig initialization with null optional fields`() {
        val schemeConfig = SchemeConfig()

        assertNull(schemeConfig.merchantPresentationName)
        assertNull(schemeConfig.visaConfig)
        assertNull(schemeConfig.mastercardConfig)
    }

    @Test
    fun `test TransactionAmount initialization`() {
        val amount = TransactionAmount(amount = "3123", currencyCode = "USD")

        assertEquals("3123", amount.amount)
        assertEquals("USD", amount.currencyCode)
    }

    @Test
    fun `test CTPUiConfig initialization`() {
        val ctpUiConfig = CTPUiConfig(
            buttonStyle = "solid",
            buttonTextCase = "capitalize",
            buttonAndBadgeColor = "#3B82F6",
            fontFamily = "Sansation",
            buttonAndInputRadius = "1rem",
            cardItemRadius = "2rem"
        )

        assertEquals("solid", ctpUiConfig.buttonStyle)
        assertEquals("capitalize", ctpUiConfig.buttonTextCase)
        assertEquals("#3B82F6", ctpUiConfig.buttonAndBadgeColor)
        assertEquals("Sansation", ctpUiConfig.fontFamily)
        assertEquals("1rem", ctpUiConfig.buttonAndInputRadius)
        assertEquals("2rem", ctpUiConfig.cardItemRadius)
    }

    @Test
    fun `test CTPUiConfig default null values`() {
        val ctpUiConfig = CTPUiConfig()

        assertNull(ctpUiConfig.buttonStyle)
        assertNull(ctpUiConfig.buttonTextCase)
        assertNull(ctpUiConfig.buttonAndBadgeColor)
        assertNull(ctpUiConfig.buttonFilledHoverColor)
        assertNull(ctpUiConfig.buttonOutlinedHoverColor)
        assertNull(ctpUiConfig.buttonDisabledColor)
        assertNull(ctpUiConfig.cardItemActiveColor)
        assertNull(ctpUiConfig.buttonAndBadgeTextColor)
        assertNull(ctpUiConfig.linkTextColor)
        assertNull(ctpUiConfig.accentColor)
        assertNull(ctpUiConfig.fontFamily)
        assertNull(ctpUiConfig.buttonAndInputRadius)
        assertNull(ctpUiConfig.cardItemRadius)
    }

    @Test
    fun `test CTPConfig initialization`() {
        val schemeConfig = SchemeConfig(
            mastercardConfig = MastercardConfig("mc-init", "mc-dpa")
        )
        val transactionAmount = TransactionAmount("100", "EUR")
        val ctpConfig = CTPConfig(
            enableCTP = true,
            enableCustomerOnboarding = true,
            schemeConfig = schemeConfig,
            transactionAmount = transactionAmount,
            uiConfig = null
        )

        assertTrue(ctpConfig.enableCTP)
        assertTrue(ctpConfig.enableCustomerOnboarding)
        assertNotNull(ctpConfig.schemeConfig)
        assertEquals("100", ctpConfig.transactionAmount.amount)
        assertEquals("EUR", ctpConfig.transactionAmount.currencyCode)
        assertNull(ctpConfig.uiConfig)
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

    // MARK: - New v1.4 Error Tests

    @Test
    fun `test EmailErrors initialization`() {
        val errors = EmailErrors(
            isRequired = "Email is required",
            isInvalid = "Invalid email address"
        )

        assertEquals("Email is required", errors.isRequired)
        assertEquals("Invalid email address", errors.isInvalid)
    }

    @Test
    fun `test FirstNameErrors initialization`() {
        val errors = FirstNameErrors(isRequired = "First name is required")

        assertEquals("First name is required", errors.isRequired)
    }

    @Test
    fun `test LastNameErrors initialization`() {
        val errors = LastNameErrors(isRequired = "Last name is required")

        assertEquals("Last name is required", errors.isRequired)
    }

    @Test
    fun `test CountryErrors initialization`() {
        val errors = CountryErrors(isRequired = "Please select a country")

        assertEquals("Please select a country", errors.isRequired)
    }

    @Test
    fun `test MobileNumberErrors initialization`() {
        val errors = MobileNumberErrors(
            isRequired = "Mobile number is required",
            isInvalid = "Enter a valid international mobile number"
        )

        assertEquals("Mobile number is required", errors.isRequired)
        assertEquals("Enter a valid international mobile number", errors.isInvalid)
    }

    @Test
    fun `test AddressLevel1Errors initialization`() {
        val errors = AddressLevel1Errors(
            isRequired = "Address is required",
            isTooLong = "Address cannot exceed 50 characters"
        )

        assertEquals("Address is required", errors.isRequired)
        assertEquals("Address cannot exceed 50 characters", errors.isTooLong)
    }

    @Test
    fun `test CityErrors initialization`() {
        val errors = CityErrors(
            isRequired = "City is required",
            isTooLong = "City name cannot exceed 40 characters",
            isTooShort = "City name is too short"
        )

        assertEquals("City is required", errors.isRequired)
        assertEquals("City name cannot exceed 40 characters", errors.isTooLong)
        assertEquals("City name is too short", errors.isTooShort)
    }

    @Test
    fun `test StateProvinceErrors initialization`() {
        val errors = StateProvinceErrors(
            isRequired = "State/Province is required",
            isTooLong = "State/Province name cannot exceed 40 characters",
            isTooShort = "State/Province name is too short"
        )

        assertEquals("State/Province is required", errors.isRequired)
        assertEquals("State/Province name cannot exceed 40 characters", errors.isTooLong)
        assertEquals("State/Province name is too short", errors.isTooShort)
    }

    @Test
    fun `test ZipCodeErrors initialization`() {
        val errors = ZipCodeErrors(
            isRequired = "Zip code is required",
            isInvalid = "Invalid zip code"
        )

        assertEquals("Zip code is required", errors.isRequired)
        assertEquals("Invalid zip code", errors.isInvalid)
    }

    // MARK: - LocaleText Tests

    @Test
    fun `test LocaleTextLabels initialization with v1 3 fields`() {
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
    fun `test LocaleTextLabels initialization with v1 4 fields`() {
        val labels = LocaleTextLabels(
            cardNumber = "Card Number",
            cardholderName = "Cardholder Name",
            expiryDate = "Expiry Date",
            securityCode = "CVV",
            separatorText = "or use",
            manualCardEntryBtnText = "Manual card entry",
            formTitle = "Enter card details",
            email = "Email Address",
            country = "Country",
            firstName = "First Name",
            lastName = "Last Name",
            mobileNumber = "Mobile Number",
            selectedCountry = "Select a Country",
            addresslevel1 = "Address",
            stateProvince = "State/Province",
            city = "City",
            zipCode = "Zip Code"
        )

        assertEquals("Card Number", labels.cardNumber)
        assertEquals("or use", labels.separatorText)
        assertEquals("Manual card entry", labels.manualCardEntryBtnText)
        assertEquals("Enter card details", labels.formTitle)
        assertEquals("Email Address", labels.email)
        assertEquals("Country", labels.country)
        assertEquals("First Name", labels.firstName)
        assertEquals("Last Name", labels.lastName)
        assertEquals("Mobile Number", labels.mobileNumber)
        assertEquals("Select a Country", labels.selectedCountry)
        assertEquals("Address", labels.addresslevel1)
        assertEquals("State/Province", labels.stateProvince)
        assertEquals("City", labels.city)
        assertEquals("Zip Code", labels.zipCode)
    }

    @Test
    fun `test LocaleTextPlaceholders initialization with v1 3 fields`() {
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
    fun `test LocaleTextPlaceholders initialization with v1 4 fields`() {
        val placeholders = LocaleTextPlaceholders(
            cardNumber = "1234 5678 9012 3456",
            cardholderName = "John Doe",
            expiryDate = "MM/YY",
            securityCode = "123",
            email = "john@example.com",
            firstName = "John",
            lastName = "Doe",
            mobileNumber = "+49 170 1234567",
            addressLevel1 = "123 Main St",
            stateProvince = "Bavaria",
            city = "Munich",
            zipCode = "80331"
        )

        assertEquals("john@example.com", placeholders.email)
        assertEquals("John", placeholders.firstName)
        assertEquals("Doe", placeholders.lastName)
        assertEquals("+49 170 1234567", placeholders.mobileNumber)
        assertEquals("123 Main St", placeholders.addressLevel1)
        assertEquals("Bavaria", placeholders.stateProvince)
        assertEquals("Munich", placeholders.city)
        assertEquals("80331", placeholders.zipCode)
    }

    @Test
    fun `test LocaleTextAriaLabels initialization with v1 3 fields`() {
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
    fun `test LocaleTextAriaLabels initialization with v1 4 fields`() {
        val ariaLabels = LocaleTextAriaLabels(
            cardNumber = "Enter card number",
            cardholderName = "Enter cardholder name",
            expiryDate = "Enter expiry date",
            securityCode = "Enter security code",
            email = "Enter your email address",
            country = "Select your country",
            firstName = "Enter your first name",
            lastName = "Enter your last name",
            mobile = "Enter your mobile number",
            addressLevel1 = "Enter your address",
            city = "Enter your city",
            stateProvince = "Enter your state or province",
            zipCode = "Enter your zip code"
        )

        assertEquals("Enter your email address", ariaLabels.email)
        assertEquals("Select your country", ariaLabels.country)
        assertEquals("Enter your first name", ariaLabels.firstName)
        assertEquals("Enter your last name", ariaLabels.lastName)
        assertEquals("Enter your mobile number", ariaLabels.mobile)
        assertEquals("Enter your address", ariaLabels.addressLevel1)
        assertEquals("Enter your city", ariaLabels.city)
        assertEquals("Enter your state or province", ariaLabels.stateProvince)
        assertEquals("Enter your zip code", ariaLabels.zipCode)
    }

    @Test
    fun `test LocaleTextErrors initialization with v1 3 fields`() {
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
    fun `test LocaleTextErrors initialization with v1 4 fields`() {
        val localeTextErrors = LocaleTextErrors(
            cardNumber = CardNumberErrors(isRequired = "Required"),
            email = EmailErrors(isRequired = "Email required", isInvalid = "Invalid email"),
            firstName = FirstNameErrors(isRequired = "First name required"),
            lastName = LastNameErrors(isRequired = "Last name required"),
            country = CountryErrors(isRequired = "Country required"),
            mobileNumber = MobileNumberErrors(isRequired = "Mobile required", isInvalid = "Invalid mobile"),
            addressLevel1 = AddressLevel1Errors(isRequired = "Address required", isTooLong = "Too long"),
            city = CityErrors(isRequired = "City required", isTooLong = "Too long", isTooShort = "Too short"),
            stateProvince = StateProvinceErrors(isRequired = "State required", isTooLong = "Too long", isTooShort = "Too short"),
            zipCode = ZipCodeErrors(isRequired = "Zip required", isInvalid = "Invalid zip")
        )

        assertEquals("Email required", localeTextErrors.email?.isRequired)
        assertEquals("Invalid email", localeTextErrors.email?.isInvalid)
        assertEquals("First name required", localeTextErrors.firstName?.isRequired)
        assertEquals("Last name required", localeTextErrors.lastName?.isRequired)
        assertEquals("Country required", localeTextErrors.country?.isRequired)
        assertEquals("Mobile required", localeTextErrors.mobileNumber?.isRequired)
        assertEquals("Invalid mobile", localeTextErrors.mobileNumber?.isInvalid)
        assertEquals("Address required", localeTextErrors.addressLevel1?.isRequired)
        assertEquals("Too long", localeTextErrors.addressLevel1?.isTooLong)
        assertEquals("City required", localeTextErrors.city?.isRequired)
        assertEquals("Too long", localeTextErrors.city?.isTooLong)
        assertEquals("Too short", localeTextErrors.city?.isTooShort)
        assertEquals("State required", localeTextErrors.stateProvince?.isRequired)
        assertEquals("Zip required", localeTextErrors.zipCode?.isRequired)
        assertEquals("Invalid zip", localeTextErrors.zipCode?.isInvalid)
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
        assertNull(config.customIconsConfig)
        assertNull(config.ctpConfig)

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
        val customIconsConfig = CustomIconsConfig(
            useCustomValidationIcons = true,
            showCardBrandIcons = false,
            successIcon = "/valid.svg",
            errorIcon = "/invalid.svg"
        )
        val ctpConfig = CTPConfig(
            enableCTP = true,
            enableCustomerOnboarding = true,
            schemeConfig = SchemeConfig(
                mastercardConfig = MastercardConfig("mc-init", "mc-dpa")
            ),
            transactionAmount = TransactionAmount("500", "EUR")
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
            customIconsConfig = customIconsConfig,
            ctpConfig = ctpConfig,
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
        assertTrue(config.customIconsConfig?.useCustomValidationIcons == true)
        assertEquals("/valid.svg", config.customIconsConfig?.successIcon)
        assertEquals("/invalid.svg", config.customIconsConfig?.errorIcon)
        assertTrue(config.ctpConfig?.enableCTP == true)
        assertEquals("500", config.ctpConfig?.transactionAmount?.amount)
        assertEquals("EUR", config.ctpConfig?.transactionAmount?.currencyCode)
    }
}
