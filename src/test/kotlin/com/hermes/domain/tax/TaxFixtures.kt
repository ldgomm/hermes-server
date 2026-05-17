package com.hermes.domain.tax

import java.time.Instant

object TaxFixtures {
    val now: Instant = Instant.parse("2026-05-16T00:00:00Z")

    val iva13Rate: TaxRate = TaxRate.of(
        id = "taxr_iva13",
        code = "iva_13",
        name = "IVA 13%",
        rate = "13.0000",
        sriTaxCode = "2",
        sriRateCode = "4",
        legalBasis = "Test legal basis",
        effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
        now = now,
    )

    val iva0Rate: TaxRate = TaxRate.of(
        id = "taxr_iva0",
        code = "iva_0_rate",
        name = "IVA 0%",
        rate = "0.0000",
        sriTaxCode = "2",
        sriRateCode = "0",
        legalBasis = "Test legal basis",
        effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
        now = now,
    )

    val iva13Profile = TaxProfile(
        id = "taxp_iva13",
        code = "iva_current_full",
        name = "IVA current full",
        treatment = TaxTreatment.IVA_FULL,
        status = TaxProfileStatus.ACTIVE,
        taxRate = iva13Rate,
        sriTaxCode = null,
        sriRateCode = null,
        legalBasis = "Test legal basis",
        effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
        createdAt = now,
        updatedAt = now,
    )

    val iva0Profile = TaxProfile(
        id = "taxp_iva0",
        code = "iva_0",
        name = "IVA 0",
        treatment = TaxTreatment.IVA_ZERO,
        status = TaxProfileStatus.ACTIVE,
        taxRate = iva0Rate,
        sriTaxCode = null,
        sriRateCode = null,
        legalBasis = "Test legal basis",
        effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
        createdAt = now,
        updatedAt = now,
    )

    val exemptProfile = TaxProfile(
        id = "taxp_exempt",
        code = "exempt_iva",
        name = "Exempt IVA",
        treatment = TaxTreatment.EXEMPT_IVA,
        status = TaxProfileStatus.ACTIVE,
        taxRate = null,
        sriTaxCode = "2",
        sriRateCode = "7",
        legalBasis = "Test legal basis",
        effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
        createdAt = now,
        updatedAt = now,
    )
}
