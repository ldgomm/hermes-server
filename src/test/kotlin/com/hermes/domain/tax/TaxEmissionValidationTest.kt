package com.hermes.domain.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TaxEmissionValidationTest {
    @Test
    fun `allows electronic emission with SRI compatible snapshots`() {
        val calculation = TaxEngine.calculate(
            listOf(
                TaxLineInput(
                    lineId = "line_1",
                    description = "IVA product",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("10.00"),
                    taxProfileSnapshot = TaxFixtures.iva13Profile.snapshot(TaxFixtures.now, forEmission = true),
                )
            )
        )

        TaxEmissionValidation.assertCanPrepareEmission(
            emissionType = TaxEmissionType.ELECTRONIC_INVOICE,
            lines = calculation.lines,
            summary = calculation.summary,
        )
    }

    @Test
    fun `rejects electronic emission with internal no tax profile`() {
        val internalProfile = TaxProfile(
            id = "taxp_internal",
            code = "no_tax_internal",
            name = "No tax internal",
            treatment = TaxTreatment.NO_TAX_INTERNAL,
            status = TaxProfileStatus.ACTIVE,
            taxRate = null,
            sriTaxCode = null,
            sriRateCode = null,
            legalBasis = "Internal only",
            effectiveFrom = TaxFixtures.now.minusSeconds(60),
            createdAt = TaxFixtures.now,
            updatedAt = TaxFixtures.now,
        )

        val calculation = TaxEngine.calculate(
            listOf(
                TaxLineInput(
                    lineId = "line_1",
                    description = "Internal item",
                    quantity = Quantity.units(1),
                    unitPrice = Money.of("10.00"),
                    taxProfileSnapshot = internalProfile.snapshot(TaxFixtures.now),
                )
            )
        )

        assertFailsWith<DomainRuleViolation> {
            TaxEmissionValidation.assertCanPrepareEmission(
                emissionType = TaxEmissionType.ELECTRONIC_INVOICE,
                lines = calculation.lines,
                summary = calculation.summary,
            )
        }
    }
}
