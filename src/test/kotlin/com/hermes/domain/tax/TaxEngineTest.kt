package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class wTaxEngineTest {
    @Test
    fun `calculates tax exclusive IVA line`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "line_1",
                description = "Product",
                quantity = Quantity.units(2),
                unitPrice = Money.of("10.00"),
                discount = Money.of("0.00"),
                taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
            )
        )

        assertEquals("20.00", result.taxableBase.amount.toPlainString())
        assertEquals("2.60", result.taxAmount.amount.toPlainString())
        assertEquals("22.60", result.total.amount.toPlainString())
    }

    @Test
    fun `calculates tax inclusive IVA line`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "line_1",
                description = "Product",
                quantity = Quantity.units(1),
                unitPrice = Money.of("11.30"),
                discount = Money.of("0.00"),
                taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                priceTaxMode = PriceTaxMode.TAX_INCLUSIVE,
            )
        )

        assertEquals("10.00", result.taxableBase.amount.toPlainString())
        assertEquals("1.30", result.taxAmount.amount.toPlainString())
        assertEquals("11.30", result.total.amount.toPlainString())
    }

    @Test
    fun `separates zero rate base`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "line_1",
                description = "Zero product",
                quantity = Quantity.units(1),
                unitPrice = Money.of("5.00"),
                taxProfileSnapshot = TaxFixtures.iva0Profile.snapshot(TaxFixtures.now),
            )
        )

        assertEquals("5.00", result.zeroRateBase.amount.toPlainString())
        assertEquals("0.00", result.taxAmount.amount.toPlainString())
    }

    @Test
    fun `rejects discount greater than gross`() {
        assertFailsWith<DomainRuleViolation> {
            TaxEngine.calculateLine(
                TaxLineInput(
                    lineId = "line_1",
                    description = "Product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("5.00"),
                    discount = Money.of("5.01"),
                    taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                )
            )
        }
    }
}
