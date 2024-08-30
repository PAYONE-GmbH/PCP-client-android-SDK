package com.payone.pcp_client_android_sdk.cctokenizer

data class Config(
    val fields: Fields,
    val defaultStyle: DefaultStyle,
    val autoCardtypeDetection: AutoCardtypeDetection,
    val language: String,
    val submitButton: ButtonConfig,
    val submitButtonWithOutCompleteCheck: ButtonConfig,
    val creditCardCheckCallback: ((String) -> Unit)? = null,
    val error: String
)

data class Fields(
    val cardpan: FieldConfig,
    val cardcvc2: FieldConfig,
    val cardexpiremonth: FieldConfig,
    val cardexpireyear: FieldConfig
)

data class FieldConfig(
    val selector: String,
    val type: String,
    val size: String? = null,
    val maxlength: String? = null,
    val length: Map<String, Int>? = null, // This maps card types to lengths
    val iframe: IFrameConfig? = null
)

data class IFrameConfig(
    val width: String? = null
)

data class DefaultStyle(
    val input: String,
    val inputFocus: String,
    val select: String,
    val iframe: Map<String, String> = emptyMap()
)

data class AutoCardtypeDetection(
    val supportedCardtypes: List<String>,
    val callback: ((String) -> Unit)? = null
)

data class ButtonConfig(
    val selector: String
)
