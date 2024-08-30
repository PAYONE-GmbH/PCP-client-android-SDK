package com.payone.pcp_client_android_sdk.cctokenizer

data class Request(
    val request: String,
    val responsetype: String,
    val mode: String,
    val mid: String,
    val aid: String,
    val portalid: String,
    val encoding: String,
    val storecarddata: String,
    val api_version: String,
    val hash: String
)
