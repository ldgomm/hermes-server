package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxMandatoryCalculationCasesTest {
    @Test
    fun `calculates mixed sale with IVA zero exempt and not subject bases`() {
        val calculation = TaxEngine.calculate(
            listOf(
                TaxLineInput(
                    lineId = "iva",
                    description = "IVA product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("100.00"),
                    taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                ),
                TaxLineInput(
                    lineId = "zero",
                    description = "Zero rate product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("20.00"),
                    taxProfileSnapshot = TaxFixtures.iva0Profile.snapshot(TaxFixtures.now),
                ),
                TaxLineInput(
                    lineId = "exempt",
                    description = "Exempt product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("10.00"),
                    taxProfileSnapshot = TaxFixtures.exemptProfile.snapshot(TaxFixtures.now),
                ),
                TaxLineInput(
                    lineId = "not_subject",
                    description = "Not subject product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("7.00"),
                    taxProfileSnapshot = TaxFixtures.notSubjectProfile.snapshot(TaxFixtures.now),
                ),
            )
        )

        assertEquals("100.00", calculation.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("20.00", calculation.summary.subtotalZeroRate.amount.toPlainString())
        assertEquals("10.00", calculation.summary.subtotalExempt.amount.toPlainString())
        assertEquals("7.00", calculation.summary.subtotalNotSubject.amount.toPlainString())
        assertEquals("13.00", calculation.summary.totalTax.amount.toPlainString())
        assertEquals("150.00", calculation.summary.grandTotal.amount.toPlainString())
        assertEquals(4, calculation.summary.taxesByRate.size)
    }

    @Test
    fun `calculates tax-inclusive price preserving grand total`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "inclusive",
                description = "Tax inclusive item",
                quantity = Quantity.units(1),
                unitPrice = Money.of("11.30"),
                taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
                priceTaxMode = PriceTaxMode.TAX_INCLUSIVE,
            )
        )

        assertEquals("10.00", result.taxableBase.amount.toPlainString())
        assertEquals("1.30", result.taxAmount.amount.toPlainString())
        assertEquals("11.30", result.total.amount.toPlainString())
    }

    @Test
    fun `applies line discount before tax calculation`() {
        val result = TaxEngine.calculateLine(
            TaxLineInput(
                lineId = "discount",
                description = "Discounted item",
                quantity = Quantity.units(2),
                unitPrice = Money.of("10.00"),
                discount = Money.of("5.00"),
                taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now),
            )
        )

        assertEquals("20.00", result.grossAmount.amount.toPlainString())
        assertEquals("5.00", result.discount.amount.toPlainString())
        assertEquals("15.00", result.taxableBase.amount.toPlainString())
        assertEquals("1.95", result.taxAmount.amount.toPlainString())
        assertEquals("16.95", result.total.amount.toPlainString())
    }

    @Test
    fun `rejects empty calculation`() {
        assertFailsWith<DomainRuleViolation> {
            TaxEngine.calculate(emptyList())
        }
    }
}
