package com.hermes.domain.tax

import java.time.Instant

/**
 * Ecuador initial tax seed.
 *
 * These values are not a substitute for live regulatory verification before release.
 * Keep them versioned in database and editable by platform administrators.
 */
object EcuadorTaxSeed {
    private val effectiveFrom2026: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val seedTime: Instant = Instant.parse("2026-05-16T00:00:00Z")

    val iva13 = TaxRate.of(
        id = "taxr_ec_iva_13_2026",
        code = "ec_iva_13_2026",
        name = "IVA 13%",
        kind = TaxKind.IVA,
        rate = "13.0000",
        sriTaxCode = "2",
        sriRateCode = "4",
        legalBasis = "SRI IVA vigente verificado para la fase 6; confirmar contra ficha técnica antes de producción.",
        effectiveFrom = effectiveFrom2026,
        now = seedTime,
    )

    val iva5Construction = TaxRate.of(
        id = "taxr_ec_iva_5_construction_2026",
        code = "ec_iva_5_construction_2026",
        name = "IVA 5% materiales de construcción",
        kind = TaxKind.IVA,
        rate = "5.0000",
        sriTaxCode = "2",
        sriRateCode = null,
        legalBasis = "SRI IVA materiales de construcción; confirmar código tarifa contra ficha técnica antes de producción.",
        effectiveFrom = effectiveFrom2026,
        now = seedTime,
    )

    val iva0 = TaxRate.of(
        id = "taxr_ec_iva_0_2026",
        code = "ec_iva_0_2026",
        name = "IVA 0%",
        kind = TaxKind.IVA,
        rate = "0.0000",
        sriTaxCode = "2",
        sriRateCode = "0",
        legalBasis = "SRI IVA tarifa 0%; confirmar contra ficha técnica antes de producción.",
        effectiveFrom = effectiveFrom2026,
        now = seedTime,
    )

    val profiles: List<TaxProfile> = listOf(
        TaxProfile(
            id = "taxp_ec_iva_current_full",
            code = "iva_current_full",
            name = "IVA tarifa vigente general",
            treatment = TaxTreatment.IVA_FULL,
            status = TaxProfileStatus.ACTIVE,
            taxRate = iva13,
            sriTaxCode = null,
            sriRateCode = null,
            legalBasis = iva13.legalBasis,
            effectiveFrom = effectiveFrom2026,
            source = TaxSource.SYSTEM_SEED,
            createdAt = seedTime,
            updatedAt = seedTime,
        ),
        TaxProfile(
            id = "taxp_ec_iva_5_construction",
            code = "iva_reduced_or_special_construction",
            name = "IVA reducido/especial materiales de construcción",
            treatment = TaxTreatment.IVA_REDUCED_OR_SPECIAL,
            status = TaxProfileStatus.ACTIVE,
            taxRate = iva5Construction,
            sriTaxCode = null,
            sriRateCode = null,
            legalBasis = iva5Construction.legalBasis,
            effectiveFrom = effectiveFrom2026,
            source = TaxSource.SYSTEM_SEED,
            createdAt = seedTime,
            updatedAt = seedTime,
        ),
        TaxProfile(
            id = "taxp_ec_iva_0",
            code = "iva_0",
            name = "IVA 0%",
            treatment = TaxTreatment.IVA_ZERO,
            status = TaxProfileStatus.ACTIVE,
            taxRate = iva0,
            sriTaxCode = null,
            sriRateCode = null,
            legalBasis = iva0.legalBasis,
            effectiveFrom = effectiveFrom2026,
            source = TaxSource.SYSTEM_SEED,
            createdAt = seedTime,
            updatedAt = seedTime,
        ),
        TaxProfile(
            id = "taxp_ec_exempt_iva",
            code = "exempt_iva",
            name = "Exento de IVA",
            treatment = TaxTreatment.EXEMPT_IVA,
            status = TaxProfileStatus.ACTIVE,
            taxRate = null,
            sriTaxCode = "2",
            sriRateCode = "7",
            legalBasis = "Ventas exentas de IVA; confirmar código tarifa contra ficha técnica antes de producción.",
            effectiveFrom = effectiveFrom2026,
            source = TaxSource.SYSTEM_SEED,
            createdAt = seedTime,
            updatedAt = seedTime,
        ),
        TaxProfile(
            id = "taxp_ec_not_subject_to_iva",
            code = "not_subject_to_iva",
            name = "No objeto de IVA",
            treatment = TaxTreatment.NOT_SUBJECT_TO_IVA,
            status = TaxProfileStatus.ACTIVE,
            taxRate = null,
            sriTaxCode = "2",
            sriRateCode = "6",
            legalBasis = "Transferencias no objeto de IVA; confirmar código tarifa contra ficha técnica antes de producción.",
            effectiveFrom = effectiveFrom2026,
            source = TaxSource.SYSTEM_SEED,
            createdAt = seedTime,
            updatedAt = seedTime,
        ),
        TaxProfile(
            id = "taxp_ec_no_tax_internal",
            code = "no_tax_internal",
            name = "Sin impuesto interno",
            treatment = TaxTreatment.NO_TAX_INTERNAL,
            status = TaxProfileStatus.ACTIVE,
            taxRate = null,
            sriTaxCode = null,
            sriRateCode = null,
            legalBasis = "Perfil interno para registros no emitibles electrónicamente.",
            effectiveFrom = effectiveFrom2026,
            source = TaxSource.SYSTEM_SEED,
            createdAt = seedTime,
            updatedAt = seedTime,
        ),
    )

    val rates: List<TaxRate> = listOf(iva13, iva5Construction, iva0)
}
