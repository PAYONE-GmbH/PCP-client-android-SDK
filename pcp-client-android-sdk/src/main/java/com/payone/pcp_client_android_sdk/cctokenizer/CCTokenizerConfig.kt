package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import java.io.Serializable

// Font style configuration
data class FontStyle(
    val fontSize: String? = null,
    val fontWeight: String? = null,
    val fontSizeMobile: String? = null
) : Serializable

// UI customization options
data class UIConfig(
    val formBgColor: String? = null,
    val formMarginLeft: String? = null,
    val formMarginRight: String? = null,
    val fieldBgColor: String? = null,
    val fieldBorder: String? = null,
    val fieldOutline: String? = null,
    val fieldLabelColor: String? = null,
    val fieldPlaceholderColor: String? = null,
    val fieldTextColor: String? = null,
    val fieldErrorCodeColor: String? = null,
    val fontFamily: String? = null,
    val fontUrl: String? = null,
    val labelStyle: FontStyle? = null,
    val inputStyle: FontStyle? = null,
    val errorValidationStyle: FontStyle? = null,
    val manualEntryFormLabelStyle: FontStyle? = null,
    val checkboxLabelStyle: FontStyle? = null,
    val termsTextStyle: FontStyle? = null,
    val checkboxLabelColor: String? = null,
    val checkboxSize: String? = null,
    val btnBgColor: String? = null,
    val btnTextColor: String? = null,
    val btnBorderColor: String? = null,
    val separatorColor: String? = null,
    val separatorTextColor: String? = null,
    val termsTextColor: String? = null,
    val inputBorderRadius: String? = null,
    val inputBorderColorDefault: String? = null,
    val inputBorderColorSuccess: String? = null,
    val inputBorderColorError: String? = null,
    val inputFocusOutline: String? = null,
    val inputPadding: String? = null,
    val fieldSpacingVertical: String? = null,
    val labelMarginBottom: String? = null,
    val inputMarginBottom: String? = null,
    val errorMarginBottom: String? = null,
    val buttonMarginBottom: String? = null,
    val separatorTextMarginBottom: String? = null,
    val checkboxTextMarginBottom: String? = null,
    val termsTextMarginBottom: String? = null,
    val iconWidth: String? = null,
    val iconPaddingRight: String? = null
) : Serializable

// Iframe config for the payment form
data class IframeConfig(
    val iframeWrapperId: String,
    val height: Int? = null,
    val width: Int? = null,
    val zIndex: Int? = null
) : Serializable

// Submit button config
data class SubmitButtonConfig(
    val selector: String? = null,
    val element: Any? = null // For Android, could be a View reference
) : Serializable

// Custom validation icons configuration (v1.4)
data class CustomIconsConfig(
    val useCustomValidationIcons: Boolean = false,
    val showCardBrandIcons: Boolean = false,
    val successIcon: String? = null,
    val errorIcon: String? = null
) : Serializable

// Visa-specific Click to Pay configuration (v1.4)
data class VisaConfig(
    val srcInitiatorId: String,
    val srcDpaId: String,
    val encryptionKey: String,
    val nModulus: String
) : Serializable

// Mastercard-specific Click to Pay configuration (v1.4)
data class MastercardConfig(
    val srcInitiatorId: String,
    val srcDpaId: String
) : Serializable

// Scheme configuration for Click to Pay (v1.4)
data class SchemeConfig(
    val merchantPresentationName: String? = null,
    val visaConfig: VisaConfig? = null,
    val mastercardConfig: MastercardConfig? = null
) : Serializable

// Transaction amount for Click to Pay (v1.4)
data class TransactionAmount(
    val amount: String,
    val currencyCode: String
) : Serializable

// UI configuration for Click to Pay component (v1.4)
data class CTPUiConfig(
    val buttonStyle: String? = null,
    val buttonTextCase: String? = null,
    val buttonAndBadgeColor: String? = null,
    val buttonFilledHoverColor: String? = null,
    val buttonOutlinedHoverColor: String? = null,
    val buttonDisabledColor: String? = null,
    val cardItemActiveColor: String? = null,
    val buttonAndBadgeTextColor: String? = null,
    val linkTextColor: String? = null,
    val accentColor: String? = null,
    val fontFamily: String? = null,
    val buttonAndInputRadius: String? = null,
    val cardItemRadius: String? = null
) : Serializable

// Click to Pay top-level configuration (v1.4)
data class CTPConfig(
    val enableCTP: Boolean = true,
    val enableCustomerOnboarding: Boolean = true,
    val schemeConfig: SchemeConfig,
    val transactionAmount: TransactionAmount,
    val uiConfig: CTPUiConfig? = null
) : Serializable

// Security code errors configuration
data class SecurityCodeErrors(
    val isRequired: String? = null,
    val amexCardSecurityCodeError: String? = null,
    val generalSecurityCodeError: String? = null
) : Serializable

// Locale text labels
data class LocaleTextLabels(
    val cardNumber: String? = null,
    val cardholderName: String? = null,
    val expiryDate: String? = null,
    val securityCode: String? = null,
    // v1.4 additions
    val separatorText: String? = null,
    val manualCardEntryBtnText: String? = null,
    val formTitle: String? = null,
    val email: String? = null,
    val country: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val mobileNumber: String? = null,
    val selectedCountry: String? = null,
    val addresslevel1: String? = null,
    val stateProvince: String? = null,
    val city: String? = null,
    val zipCode: String? = null
) : Serializable

// Locale text placeholders
data class LocaleTextPlaceholders(
    val cardNumber: String? = null,
    val cardholderName: String? = null,
    val expiryDate: String? = null,
    val securityCode: String? = null,
    // v1.4 additions
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val mobileNumber: String? = null,
    val addressLevel1: String? = null,
    val stateProvince: String? = null,
    val city: String? = null,
    val zipCode: String? = null
) : Serializable

// Locale text aria labels
data class LocaleTextAriaLabels(
    val cardNumber: String? = null,
    val cardholderName: String? = null,
    val expiryDate: String? = null,
    val securityCode: String? = null,
    // v1.4 additions
    val email: String? = null,
    val country: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val mobile: String? = null,
    val addressLevel1: String? = null,
    val city: String? = null,
    val stateProvince: String? = null,
    val zipCode: String? = null
) : Serializable

// Email error fields (v1.4)
data class EmailErrors(
    val isRequired: String? = null,
    val isInvalid: String? = null
) : Serializable

// First name error fields (v1.4)
data class FirstNameErrors(
    val isRequired: String? = null
) : Serializable

// Last name error fields (v1.4)
data class LastNameErrors(
    val isRequired: String? = null
) : Serializable

// Country error fields (v1.4)
data class CountryErrors(
    val isRequired: String? = null
) : Serializable

// Mobile number error fields (v1.4)
data class MobileNumberErrors(
    val isRequired: String? = null,
    val isInvalid: String? = null
) : Serializable

// Address level 1 error fields (v1.4)
data class AddressLevel1Errors(
    val isRequired: String? = null,
    val isTooLong: String? = null
) : Serializable

// City error fields (v1.4)
data class CityErrors(
    val isRequired: String? = null,
    val isTooLong: String? = null,
    val isTooShort: String? = null
) : Serializable

// State/Province error fields (v1.4)
data class StateProvinceErrors(
    val isRequired: String? = null,
    val isTooLong: String? = null,
    val isTooShort: String? = null
) : Serializable

// Zip code error fields (v1.4)
data class ZipCodeErrors(
    val isRequired: String? = null,
    val isInvalid: String? = null
) : Serializable

// Locale text errors
data class LocaleTextErrors(
    val cardNumber: CardNumberErrors? = null,
    val cardholderName: CardholderNameErrors? = null,
    val expiryDate: ExpiryDateErrors? = null,
    val securityCode: SecurityCodeErrors? = null,
    // v1.4 additions
    val email: EmailErrors? = null,
    val firstName: FirstNameErrors? = null,
    val lastName: LastNameErrors? = null,
    val country: CountryErrors? = null,
    val mobileNumber: MobileNumberErrors? = null,
    val addressLevel1: AddressLevel1Errors? = null,
    val city: CityErrors? = null,
    val stateProvince: StateProvinceErrors? = null,
    val zipCode: ZipCodeErrors? = null
) : Serializable

// Card number error fields
data class CardNumberErrors(
    val isRequired: String? = null,
    val isInvalid: String? = null,
    val isTooShort: String? = null,
    val notSupported: String? = null
) : Serializable

// Cardholder name error fields
data class CardholderNameErrors(
    val isRequired: String? = null,
    val isInvalid: String? = null
) : Serializable

// Expiry date error fields
data class ExpiryDateErrors(
    val isRequired: String? = null,
    val isInvalid: String? = null
) : Serializable

// Locale text configuration
data class LocaleTextConfig(
    val labels: LocaleTextLabels? = null,
    val placeholders: LocaleTextPlaceholders? = null,
    val arialabels: LocaleTextAriaLabels? = null,
    val errors: LocaleTextErrors? = null
) : Serializable

// Card details returned on successful tokenization
data class CardDetails(
    val cardholderName: String,
    val cardNumber: String,
    val expiryDate: String,
    val cardType: String
) : Serializable

// The configuration object to set up the credit card tokenizer.
data class CreditcardTokenizerConfig(
    val iframe: IframeConfig,
    val uiConfig: UIConfig? = null,
    val locale: String? = null,
    val token: String,
    val mode: String? = null, // "test" or "live"
    val allowedCardSchemes: List<String>? = null, // e.g., "amex", "visa", "mastercard", etc.
    val customTextConfig: Map<String, LocaleTextConfig>? = null,
    val submitButton: SubmitButtonConfig,
    // v1.4 additions
    val customIconsConfig: CustomIconsConfig? = null,
    val ctpConfig: CTPConfig? = null,
    val tokenizationSuccessCallback: (statusCode: Int, token: String, cardDetails: CardDetails, inputMode: String) -> Unit,
    val tokenizationFailureCallback: (statusCode: Int, errorResponse: Map<String, Any?>) -> Unit,
) : Serializable
