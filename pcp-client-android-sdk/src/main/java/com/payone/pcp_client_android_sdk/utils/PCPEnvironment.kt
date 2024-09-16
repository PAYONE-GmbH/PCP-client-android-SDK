package com.payone.pcp_client_android_sdk.utils

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

// The environment you want the SDK to run against.
enum class PCPEnvironment(
    val ccTokenizerIdentifier: String,
    val fingerprintTokenizerIdentifier: String
) {
    Test("test", "t"),
    Production("prod", "p")
}
