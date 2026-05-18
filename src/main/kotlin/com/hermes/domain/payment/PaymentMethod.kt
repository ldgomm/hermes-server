package com.hermes.domain.payment

enum class PaymentMethod {
    CASH,
    BANK_TRANSFER,
    CARD_MANUAL,
    CARD_GATEWAY,
    DIGITAL_WALLET,
    OTHER;

    val affectsCashDrawer: Boolean
        get() = this == CASH

    val requiresExternalReference: Boolean
        get() = this in setOf(
            BANK_TRANSFER,
            CARD_MANUAL,
            CARD_GATEWAY,
            DIGITAL_WALLET,
        )

    val isExternal: Boolean
        get() = this in setOf(
            BANK_TRANSFER,
            CARD_MANUAL,
            CARD_GATEWAY,
            DIGITAL_WALLET,
        )
}
