package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxSummaryTest {
    @Test
    fun `summarizes mixed tax lines`() {
        val calculation = TaxEngine.calculate(
            listOf(
                TaxLineInput(
                    lineId = "line_1",
                    description = "IVA product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("100.00"),
                    taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                ),
                TaxLineInput(
                    lineId = "line_2",
                    description = "Zero product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("20.00"),
                    taxProfileSnapshot = TaxFixtures.iva0Profile.snapshot(TaxFixtures.now),
                ),
                TaxLineInput(
                    lineId = "line_3",
                    description = "Exempt product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("10.00"),
                    taxProfileSnapshot = TaxFixtures.exemptProfile.snapshot(TaxFixtures.now),
                ),
            )
        )

        assertEquals("100.00", calculation.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("20.00", calculation.summary.subtotalZeroRate.amount.toPlainString())
        assertEquals("10.00", calculation.summary.subtotalExempt.amount.toPlainString())
        assertEquals("13.00", calculation.summary.totalTax.amount.toPlainString())
        assertEquals("143.00", calculation.summary.grandTotal.amount.toPlainString())
        assertEquals(3, calculation.summary.taxesByRate.size)
    }
}
