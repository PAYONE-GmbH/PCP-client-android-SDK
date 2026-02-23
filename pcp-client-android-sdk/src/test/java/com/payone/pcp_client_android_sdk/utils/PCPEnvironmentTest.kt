package com.payone.pcp_client_android_sdk.utils

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class PCPEnvironmentTest {

    @Test
    fun `test ccTokenizerIdentifier for Test environment is correct`() {
        assertEquals("test", PCPEnvironment.Test.ccTokenizerIdentifier)
    }

    @Test
    fun `test ccTokenizerIdentifier for Production environment is correct`() {
        assertEquals("prod", PCPEnvironment.Production.ccTokenizerIdentifier)
    }

    @Test
    fun `test fingerprintTokenizerIdentifier for Test environment is correct`() {
        assertEquals("t", PCPEnvironment.Test.fingerprintTokenizerIdentifier)
    }

    @Test
    fun `test fingerprintTokenizerIdentifier for Production environment is correct`() {
        assertEquals("p", PCPEnvironment.Production.fingerprintTokenizerIdentifier)
    }
}
