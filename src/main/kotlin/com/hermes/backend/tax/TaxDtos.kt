package com.hermes.backend.tax

import com.hermes.application.tax.TaxCalculatePreviewCommand
import com.hermes.application.tax.TaxCalculatePreviewLine
import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.*
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MoneyRequest(
    val amount: String,
    val currency: String = "USD",
)

@Serializable
data class QuantityRequest(
    val value: String,
    val unitCode: String = "unit",
    val allowsDecimal: Boolean = false,
)

@Serializable
data class TaxCalculatePreviewRequest(
    val occurredAt: String? = null,
    val lines: List<TaxCalculatePreviewLineRequest>,
)

@Serializable
data class TaxCalculatePreviewLineRequest(
    val lineId: String,
    val description: String,
    val quantity: QuantityRequest,
    val unitPrice: MoneyRequest,
    val discount: MoneyRequest? = null,
    val taxProfileCode: String,
    val priceTaxMode: String = "TAX_EXCLUSIVE",
)

@Serializable
data class MoneyResponse(
    val amount: String,
    val currency: String,
)

@Serializable
data class TaxRateResponse(
    val id: String,
    val code: String,
    val name: String,
    val kind: String,
    val rate: String,
    val status: String,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val legalBasis: String,
    val effectiveFrom: String,
    val effectiveTo: String?,
    val source: String,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
    val schemaVersion: Int,
)

@Serializable
data class TaxProfileResponse(
    val id: String,
    val code: String,
    val name: String,
    val treatment: String,
    val status: String,
    val taxRate: TaxRateResponse?,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val legalBasis: String,
    val effectiveFrom: String,
    val effectiveTo: String?,
    val source: String,
    val createdAt: String,
    val updatedAt: String,
    val version: Long,
    val schemaVersion: Int,
)

@Serializable
data class OrganizationTaxSettingsResponse(
    val id: String,
    val organizationId: String,
    val regime: String,
    val defaultTaxProfileCode: String,
    val enabledTaxProfileCodes: Set<String>,
    val allowTaxInclusivePrices: Boolean,
    val allowManualLineDiscounts: Boolean,
    val requireTaxProfileForCatalogItems: Boolean,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val updatedBy: String,
    val version: Long,
    val schemaVersion: Int,
)

@Serializable
data class TaxProfileSnapshotResponse(
    val profileId: String,
    val profileCode: String,
    val profileName: String,
    val treatment: String,
    val taxKind: String?,
    val rateCode: String?,
    val rateName: String?,
    val rate: String,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val legalBasis: String,
    val effectiveFrom: String,
    val effectiveTo: String?,
    val source: String,
    val capturedAt: String,
    val profileVersion: Long,
    val rateVersion: Long?,
    val electronicEmissionCompatible: Boolean,
)

@Serializable
data class TaxLineResultResponse(
    val lineId: String,
    val description: String,
    val grossAmount: MoneyResponse,
    val discount: MoneyResponse,
    val taxableBase: MoneyResponse,
    val zeroRateBase: MoneyResponse,
    val exemptBase: MoneyResponse,
    val notSubjectBase: MoneyResponse,
    val internalNoTaxBase: MoneyResponse,
    val taxAmount: MoneyResponse,
    val total: MoneyResponse,
    val taxProfileSnapshot: TaxProfileSnapshotResponse,
)

@Serializable
data class TaxSummaryRateLineResponse(
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val taxKind: String?,
    val rate: String,
    val treatment: String,
    val base: MoneyResponse,
    val taxAmount: MoneyResponse,
)

@Serializable
data class TaxSummaryResponse(
    val currency: String,
    val grossSubtotal: MoneyResponse,
    val totalDiscount: MoneyResponse,
    val subtotalTaxable: MoneyResponse,
    val subtotalZeroRate: MoneyResponse,
    val subtotalExempt: MoneyResponse,
    val subtotalNotSubject: MoneyResponse,
    val subtotalInternalNoTax: MoneyResponse,
    val totalTax: MoneyResponse,
    val grandTotal: MoneyResponse,
    val taxesByRate: List<TaxSummaryRateLineResponse>,
)

@Serializable
data class TaxCalculationPreviewResponse(
    val lines: List<TaxLineResultResponse>,
    val summary: TaxSummaryResponse,
)

@Serializable
data class TaxRatesResponse(
    val rates: List<TaxRateResponse>,
)

@Serializable
data class TaxProfilesResponse(
    val profiles: List<TaxProfileResponse>,
)

@Serializable
data class TaxSettingsResponse(
    val settings: OrganizationTaxSettingsResponse,
)

fun TaxCalculatePreviewRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): TaxCalculatePreviewCommand =
    TaxCalculatePreviewCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        occurredAt = occurredAt
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(Instant::parse)
            ?: Instant.now(),
        lines = lines.map { it.toCommandLine() },
    )

private fun TaxCalculatePreviewLineRequest.toCommandLine(): TaxCalculatePreviewLine {
    val unitPriceMoney = unitPrice.toMoney()

    return TaxCalculatePreviewLine(
        lineId = lineId,
        description = description,
        quantity = quantity.toQuantity(),
        unitPrice = unitPriceMoney,
        discount = discount?.toMoney() ?: Money.zero(unitPriceMoney.currency),
        taxProfileCode = taxProfileCode,
        priceTaxMode = PriceTaxMode.valueOf(priceTaxMode.trim().uppercase()),
    )
}

private fun MoneyRequest.toMoney(): Money =
    Money.of(amount = amount, currency = currency.trim().uppercase())

private fun QuantityRequest.toQuantity(): Quantity =
    Quantity.of(
        value = value,
        unitCode = unitCode.trim(),
        allowsDecimal = allowsDecimal,
    )

fun TaxRate.toResponse(): TaxRateResponse =
    TaxRateResponse(
        id = id,
        code = code,
        name = name,
        kind = kind.name,
        rate = rate.toPlainString(),
        status = status.name,
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        legalBasis = legalBasis,
        effectiveFrom = effectiveFrom.toString(),
        effectiveTo = effectiveTo?.toString(),
        source = source.name,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        version = version,
        schemaVersion = schemaVersion,
    )

fun TaxProfile.toResponse(): TaxProfileResponse =
    TaxProfileResponse(
        id = id,
        code = code,
        name = name,
        treatment = treatment.name,
        status = status.name,
        taxRate = taxRate?.toResponse(),
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        legalBasis = legalBasis,
        effectiveFrom = effectiveFrom.toString(),
        effectiveTo = effectiveTo?.toString(),
        source = source.name,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        version = version,
        schemaVersion = schemaVersion,
    )

fun OrganizationTaxSettings.toResponse(): OrganizationTaxSettingsResponse =
    OrganizationTaxSettingsResponse(
        id = id,
        organizationId = organizationId,
        regime = regime.name,
        defaultTaxProfileCode = defaultTaxProfileCode,
        enabledTaxProfileCodes = enabledTaxProfileCodes,
        allowTaxInclusivePrices = allowTaxInclusivePrices,
        allowManualLineDiscounts = allowManualLineDiscounts,
        requireTaxProfileForCatalogItems = requireTaxProfileForCatalogItems,
        status = status.name,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
        createdBy = createdBy,
        updatedBy = updatedBy,
        version = version,
        schemaVersion = schemaVersion,
    )

fun TaxCalculationResult.toResponse(): TaxCalculationPreviewResponse =
    TaxCalculationPreviewResponse(
        lines = lines.map { it.toResponse() },
        summary = summary.toResponse(),
    )

private fun TaxLineResult.toResponse(): TaxLineResultResponse =
    TaxLineResultResponse(
        lineId = lineId,
        description = description,
        grossAmount = grossAmount.toResponse(),
        discount = discount.toResponse(),
        taxableBase = taxableBase.toResponse(),
        zeroRateBase = zeroRateBase.toResponse(),
        exemptBase = exemptBase.toResponse(),
        notSubjectBase = notSubjectBase.toResponse(),
        internalNoTaxBase = internalNoTaxBase.toResponse(),
        taxAmount = taxAmount.toResponse(),
        total = total.toResponse(),
        taxProfileSnapshot = taxProfileSnapshot.toResponse(),
    )

private fun TaxSummary.toResponse(): TaxSummaryResponse =
    TaxSummaryResponse(
        currency = currency.value,
        grossSubtotal = grossSubtotal.toResponse(),
        totalDiscount = totalDiscount.toResponse(),
        subtotalTaxable = subtotalTaxable.toResponse(),
        subtotalZeroRate = subtotalZeroRate.toResponse(),
        subtotalExempt = subtotalExempt.toResponse(),
        subtotalNotSubject = subtotalNotSubject.toResponse(),
        subtotalInternalNoTax = subtotalInternalNoTax.toResponse(),
        totalTax = totalTax.toResponse(),
        grandTotal = grandTotal.toResponse(),
        taxesByRate = taxesByRate.map { it.toResponse() },
    )

private fun TaxSummaryRateLine.toResponse(): TaxSummaryRateLineResponse =
    TaxSummaryRateLineResponse(
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        taxKind = taxKind?.name,
        rate = rate.toPlainString(),
        treatment = treatment.name,
        base = base.toResponse(),
        taxAmount = taxAmount.toResponse(),
    )

private fun TaxProfileSnapshot.toResponse(): TaxProfileSnapshotResponse =
    TaxProfileSnapshotResponse(
        profileId = profileId,
        profileCode = profileCode,
        profileName = profileName,
        treatment = treatment.name,
        taxKind = taxKind?.name,
        rateCode = rateCode,
        rateName = rateName,
        rate = rate.toPlainString(),
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        legalBasis = legalBasis,
        effectiveFrom = effectiveFrom.toString(),
        effectiveTo = effectiveTo?.toString(),
        source = source.name,
        capturedAt = capturedAt.toString(),
        profileVersion = profileVersion,
        rateVersion = rateVersion,
        electronicEmissionCompatible = isElectronicEmissionCompatible,
    )

private fun Money.toResponse(): MoneyResponse =
    MoneyResponse(
        amount = amount.toPlainString(),
        currency = currency.value,
    )