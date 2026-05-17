package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxPrecisionRoundingTest {
    @Test
    fun `rounds tax exclusive tax to cents with half up`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "rounding_1",
                description = "Rounding product",
                quantity = Quantity.units(1),
                unitPrice = Money.of("0.05"),
                taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
            )
        )

        assertEquals("0.01", result.taxAmount.amount.toPlainString())
        assertEquals("0.06", result.total.amount.toPlainString())
    }

    @Test
    fun `rounds tax inclusive base and tax without changing total`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "rounding_2",
                description = "Inclusive rounding product",
                quantity = Quantity.units(1),
                unitPrice = Money.of("1.00"),
                taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                priceTaxMode = PriceTaxMode.TAX_INCLUSIVE,
            )
        )

        assertEquals("0.88", result.taxableBase.amount.toPlainString())
        assertEquals("0.12", result.taxAmount.amount.toPlainString())
        assertEquals("1.00", result.total.amount.toPlainString())
    }

    @Test
    fun `keeps summary totals equal to rounded line totals`() {
        val calculation = TaxEngine.calculate(
            listOf(
                TaxLineInput(
                    lineId = "line_1",
                    description = "Item 1",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("0.05"),
                    taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                ),
                TaxLineInput(
                    lineId = "line_2",
                    description = "Item 2",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("0.05"),
                    taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                ),
            )
        )

        assertEquals("0.10", calculation.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("0.02", calculation.summary.totalTax.amount.toPlainString())
        assertEquals("0.12", calculation.summary.grandTotal.amount.toPlainString())
    }
}
