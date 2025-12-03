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
    val securityCode: String? = null
) : Serializable

// Locale text placeholders
data class LocaleTextPlaceholders(
    val cardNumber: String? = null,
    val cardholderName: String? = null,
    val expiryDate: String? = null,
    val securityCode: String? = null
) : Serializable

// Locale text aria labels
data class LocaleTextAriaLabels(
    val cardNumber: String? = null,
    val cardholderName: String? = null,
    val expiryDate: String? = null,
    val securityCode: String? = null
) : Serializable

// Locale text errors
data class LocaleTextErrors(
    val cardNumber: CardNumberErrors? = null,
    val cardholderName: CardholderNameErrors? = null,
    val expiryDate: ExpiryDateErrors? = null,
    val securityCode: SecurityCodeErrors? = null
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

// Custom text config with multiple locales
data class CustomTextConfig(
    val en: LocaleTextConfig? = null,
    val de: LocaleTextConfig? = null,
    val locales: Map<String, LocaleTextConfig>? = null // For additional locales
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
    val customTextConfig: CustomTextConfig? = null,
    val submitButton: SubmitButtonConfig,
    val tokenizationSuccessCallback: (statusCode: Int, token: String, cardDetails: CardDetails, inputMode: String) -> Unit,
    val tokenizationFailureCallback: (statusCode: Int, errorResponse: Map<String, Any?>) -> Unit,
) : Serializable