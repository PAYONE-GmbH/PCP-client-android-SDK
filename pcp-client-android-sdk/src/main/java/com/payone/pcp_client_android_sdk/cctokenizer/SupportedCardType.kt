package com.payone.pcp_client_android_sdk.cctokenizer

/*
 * This file is part of the PCPClient Android SDK.
 * Copyright © 2024 PAYONE GmbH. All rights reserved.
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

enum class SupportedCardType(val identifier: String) {
    Visa("V"),
    Mastercard("M"),
    AmericanExpress("A"),
    DinersClub("D"),
    JCB("J"),
    MaestroInternational("O"),
    ChinaUnionPay("P"),
    UATP("U"),
    Girocard("G")
}