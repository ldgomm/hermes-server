package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxEngineTest {
    private val iva = TaxProfile.ivaFull()
    private val zero = TaxProfile.ivaZero()
    private val exempt = TaxProfile.exemptIva()
    private val notSubject = TaxProfile.notSubjectToIva()

    @Test
    fun `calculates full IVA item with price without tax`() {
        val result = TaxEngine.calculate(
            listOf(line("a", "100.00", iva)),
        )

        assertEquals("100.00", result.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("15.00", result.summary.totalTax.amount.toPlainString())
        assertEquals("115.00", result.summary.grandTotal.amount.toPlainString())
    }

    @Test
    fun `calculates zero exempt and not subject items separately`() {
        val result = TaxEngine.calculate(
            listOf(
                line("zero", "10.00", zero),
                line("exempt", "20.00", exempt),
                line("not_subject", "30.00", notSubject),
            ),
        )

        assertEquals("10.00", result.summary.subtotalZero.amount.toPlainString())
        assertEquals("20.00", result.summary.subtotalExempt.amount.toPlainString())
        assertEquals("30.00", result.summary.subtotalNotSubject.amount.toPlainString())
        assertEquals("0.00", result.summary.totalTax.amount.toPlainString())
        assertEquals("60.00", result.summary.grandTotal.amount.toPlainString())
    }

    @Test
    fun `calculates mixed sale summary`() {
        val result = TaxEngine.calculate(
            listOf(
                line("iva", "100.00", iva),
                line("zero", "50.00", zero),
            ),
        )

        assertEquals("100.00", result.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("50.00", result.summary.subtotalZero.amount.toPlainString())
        assertEquals("15.00", result.summary.totalTax.amount.toPlainString())
        assertEquals("165.00", result.summary.grandTotal.amount.toPlainString())
    }

    @Test
    fun `extracts tax when price includes tax`() {
        val result = TaxEngine.calculate(
            listOf(line("included", "115.00", iva, priceIncludesTax = true)),
        )

        assertEquals("100.00", result.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("15.00", result.summary.totalTax.amount.toPlainString())
        assertEquals("115.00", result.summary.grandTotal.amount.toPlainString())
    }

    @Test
    fun `applies line discount before tax`() {
        val result = TaxEngine.calculate(
            listOf(line("discounted", "100.00", iva, discount = "10.00")),
        )

        assertEquals("90.00", result.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("13.50", result.summary.totalTax.amount.toPlainString())
        assertEquals("103.50", result.summary.grandTotal.amount.toPlainString())
    }

    @Test
    fun `prorates global discount before tax`() {
        val result = TaxEngine.calculate(
            lines = listOf(
                line("a", "100.00", iva),
                line("b", "100.00", zero),
            ),
            globalDiscount = Money.of("20.00"),
        )

        assertEquals("10.00", result.lines[0].discount.amount.toPlainString())
        assertEquals("10.00", result.lines[1].discount.amount.toPlainString())
        assertEquals("90.00", result.summary.subtotalTaxable.amount.toPlainString())
        assertEquals("90.00", result.summary.subtotalZero.amount.toPlainString())
        assertEquals("13.50", result.summary.totalTax.amount.toPlainString())
        assertEquals("193.50", result.summary.grandTotal.amount.toPlainString())
    }

    @Test
    fun `rejects line without tax profile`() {
        assertFailsWith<DomainRuleViolation> {
            TaxEngine.calculate(
                listOf(
                    TaxLineInput(
                        lineId = "a",
                        description = "No tax profile",
                        unitPrice = Money.of("10.00"),
                        quantity = Quantity.units(1),
                        taxProfile = null,
                    ),
                ),
            )
        }
    }

    @Test
    fun `rejects inactive tax profile for new sale`() {
        assertFailsWith<DomainRuleViolation> {
            TaxEngine.calculate(
                listOf(line("a", "10.00", iva.copy(status = TaxProfileStatus.INACTIVE))),
            )
        }
    }

    @Test
    fun `rejects manual discount when not allowed`() {
        assertFailsWith<DomainRuleViolation> {
            TaxEngine.calculate(
                listOf(line("a", "10.00", iva, discount = "1.00", manualDiscountAllowed = false)),
            )
        }
    }

    private fun line(
        id: String,
        price: String,
        profile: TaxProfile,
        discount: String = "0.00",
        priceIncludesTax: Boolean = false,
        manualDiscountAllowed: Boolean = true,
    ): TaxLineInput = TaxLineInput(
        lineId = id,
        description = "Line $id",
        unitPrice = Money.of(price),
        quantity = Quantity.units(1),
        taxProfile = profile,
        discount = Money.of(discount),
        priceIncludesTax = priceIncludesTax,
        manualDiscountAllowed = manualDiscountAllowed,
    )
}
