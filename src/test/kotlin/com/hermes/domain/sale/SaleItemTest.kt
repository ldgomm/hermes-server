package com.hermes.domain.sale

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SaleItemTest {

    @Test
    fun `calculates gross total and line total`() {
        val item = SaleItem.create(
            id = "item_1",
            catalogItemId = "cat_1",
            name = "Parrillada",
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(2),
            discount = Money.of("1.00")
        )

        assertEquals("24.00", item.grossTotal.amount.toPlainString())
        assertEquals("23.00", item.lineTotal.amount.toPlainString())
    }

    @Test
    fun `rejects discount greater than gross total`() {
        assertFailsWith<DomainRuleViolation> {
            SaleItem.create(
                id = "item_1",
                catalogItemId = "cat_1",
                name = "Parrillada",
                unitPrice = Money.of("12.00"),
                quantity = Quantity.units(1),
                discount = Money.of("13.00")
            )
        }
    }

    @Test
    fun `moves item through operational flow`() {
        val delivered = SaleItem.create(
            id = "item_1",
            catalogItemId = "cat_1",
            name = "Parrillada",
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(1)
        ).start().markReady().deliver()

        assertEquals(SaleItemStatus.DELIVERED, delivered.status)
    }

    @Test
    fun `rejects canceling delivered item`() {
        val delivered = SaleItem.create(
            id = "item_1",
            catalogItemId = "cat_1",
            name = "Parrillada",
            unitPrice = Money.of("12.00"),
            quantity = Quantity.units(1)
        ).start().markReady().deliver()

        assertFailsWith<DomainRuleViolation> {
            delivered.cancel()
        }
    }
}
