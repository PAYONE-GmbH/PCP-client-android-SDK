package com.payone.pcp_client_android_sdk.fingerprinttokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

sealed class FingerprintError : Throwable() {
    data class ScriptError(val error: Throwable) : FingerprintError()
    data object Undefined : FingerprintError() {
        private fun readResolve(): Any = Undefined
    }
}