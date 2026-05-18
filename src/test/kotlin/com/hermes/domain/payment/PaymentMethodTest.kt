package com.hermes.domain.payment

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaymentMethodTest {
    @Test
    fun `cash affects cash drawer and does not require external reference`() {
        assertTrue(PaymentMethod.CASH.affectsCashDrawer)
        assertFalse(PaymentMethod.CASH.requiresExternalReference)
    }

    @Test
    fun `external payment methods require reference`() {
        assertTrue(PaymentMethod.BANK_TRANSFER.requiresExternalReference)
        assertTrue(PaymentMethod.CARD_MANUAL.requiresExternalReference)
        assertTrue(PaymentMethod.CARD_GATEWAY.requiresExternalReference)
        assertTrue(PaymentMethod.DIGITAL_WALLET.requiresExternalReference)
    }

    @Test
    fun `other does not affect cash drawer`() {
        assertFalse(PaymentMethod.OTHER.affectsCashDrawer)
    }
}
