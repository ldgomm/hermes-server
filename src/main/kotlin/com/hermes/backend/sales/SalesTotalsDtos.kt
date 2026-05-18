package com.hermes.backend.sales

import com.hermes.application.sales.PreviewSaleTotalsCommand
import com.hermes.application.sales.PreviewSaleTotalsLine
import com.hermes.application.sales.PreviewSaleTotalsLineResult
import com.hermes.application.sales.PreviewSaleTotalsResult
import com.hermes.domain.money.CurrencyCode
import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.PriceTaxMode
import com.hermes.domain.tax.TaxSummary
import com.hermes.domain.tax.TaxSummaryRateLine
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class PreviewSaleTotalsRequest(
    val occurredAt: String? = null,
    val lines: List<PreviewSaleTotalsLineRequest>,
)

@Serializable
data class PreviewSaleTotalsLineRequest(
    val catalogItemId: String,
    val quantity: SalesTotalsQuantityRequest,
    val unitPrice: SalesTotalsMoneyRequest? = null,
    val discount: SalesTotalsMoneyRequest? = null,
    val priceTaxMode: String = "TAX_EXCLUSIVE",
)

@Serializable
data class SalesTotalsMoneyRequest(
    val amount: String,
    val currency: String = "USD",
)

@Serializable
data class SalesTotalsQuantityRequest(
    val value: String,
    val unitCode: String = "unit",
    val allowsDecimal: Boolean = false,
)

@Serializable
data class PreviewSaleTotalsResponse(
    val organizationId: String,
    val occurredAt: String,
    val lines: List<PreviewSaleTotalsLineResponse>,
    val summary: SalesTaxSummaryResponse,
)

@Serializable
data class PreviewSaleTotalsLineResponse(
    val lineId: String,
    val catalogItemId: String,
    val catalogItemName: String,
    val quantity: SalesTotalsQuantityResponse,
    val unitPrice: SalesTotalsMoneyResponse,
    val discount: SalesTotalsMoneyResponse,
    val priceTaxMode: String,
    val grossAmount: SalesTotalsMoneyResponse,
    val taxableBase: SalesTotalsMoneyResponse,
    val zeroRateBase: SalesTotalsMoneyResponse,
    val exemptBase: SalesTotalsMoneyResponse,
    val notSubjectBase: SalesTotalsMoneyResponse,
    val internalNoTaxBase: SalesTotalsMoneyResponse,
    val taxAmount: SalesTotalsMoneyResponse,
    val total: SalesTotalsMoneyResponse,
    val taxProfileCode: String,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val treatment: String,
)

@Serializable
data class SalesTaxSummaryResponse(
    val currency: String,
    val grossSubtotal: SalesTotalsMoneyResponse,
    val totalDiscount: SalesTotalsMoneyResponse,
    val subtotalTaxable: SalesTotalsMoneyResponse,
    val subtotalZeroRate: SalesTotalsMoneyResponse,
    val subtotalExempt: SalesTotalsMoneyResponse,
    val subtotalNotSubject: SalesTotalsMoneyResponse,
    val subtotalInternalNoTax: SalesTotalsMoneyResponse,
    val totalTax: SalesTotalsMoneyResponse,
    val grandTotal: SalesTotalsMoneyResponse,
    val taxesByRate: List<SalesTaxSummaryRateLineResponse>,
)

@Serializable
data class SalesTaxSummaryRateLineResponse(
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val taxKind: String?,
    val rate: String,
    val treatment: String,
    val base: SalesTotalsMoneyResponse,
    val taxAmount: SalesTotalsMoneyResponse,
)

@Serializable
data class SalesTotalsMoneyResponse(
    val amount: String,
    val currency: String,
)

@Serializable
data class SalesTotalsQuantityResponse(
    val value: String,
    val unitCode: String,
    val allowsDecimal: Boolean,
)

fun PreviewSaleTotalsRequest.toCommand(
    organizationId: String,
    actorUserId: String,
    actorEffectivePermissions: Set<String>,
): PreviewSaleTotalsCommand =
    PreviewSaleTotalsCommand(
        organizationId = organizationId,
        actorUserId = actorUserId,
        actorEffectivePermissions = actorEffectivePermissions,
        occurredAt = occurredAt?.trim()?.takeIf { it.isNotBlank() }?.let(Instant::parse) ?: Instant.now(),
        lines = lines.map { it.toCommandLine() },
    )

fun PreviewSaleTotalsResult.toResponse(): PreviewSaleTotalsResponse =
    PreviewSaleTotalsResponse(
        organizationId = organizationId,
        occurredAt = occurredAt.toString(),
        lines = lines.map { it.toResponse() },
        summary = summary.toResponse(),
    )

private fun PreviewSaleTotalsLineRequest.toCommandLine(): PreviewSaleTotalsLine =
    PreviewSaleTotalsLine(
        catalogItemId = catalogItemId,
        quantity = quantity.toDomain(),
        unitPrice = unitPrice?.toDomain(),
        discount = discount?.toDomain(),
        priceTaxMode = PriceTaxMode.valueOf(priceTaxMode.trim().uppercase()),
    )

private fun SalesTotalsMoneyRequest.toDomain(): Money =
    Money.of(amount = BigDecimal(amount), currency = CurrencyCode(currency.trim().uppercase()))

private fun SalesTotalsQuantityRequest.toDomain(): Quantity =
    Quantity.of(value = BigDecimal(value), unitCode = unitCode.trim(), allowsDecimal = allowsDecimal)

private fun PreviewSaleTotalsLineResult.toResponse(): PreviewSaleTotalsLineResponse =
    PreviewSaleTotalsLineResponse(
        lineId = lineId,
        catalogItemId = catalogItemId,
        catalogItemName = catalogItemName,
        quantity = quantity.toResponse(),
        unitPrice = unitPrice.toResponse(),
        discount = discount.toResponse(),
        priceTaxMode = priceTaxMode.name,
        grossAmount = grossAmount.toResponse(),
        taxableBase = taxableBase.toResponse(),
        zeroRateBase = zeroRateBase.toResponse(),
        exemptBase = exemptBase.toResponse(),
        notSubjectBase = notSubjectBase.toResponse(),
        internalNoTaxBase = internalNoTaxBase.toResponse(),
        taxAmount = taxAmount.toResponse(),
        total = total.toResponse(),
        taxProfileCode = taxProfileCode,
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        treatment = treatment,
    )

private fun TaxSummary.toResponse(): SalesTaxSummaryResponse =
    SalesTaxSummaryResponse(
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

private fun TaxSummaryRateLine.toResponse(): SalesTaxSummaryRateLineResponse =
    SalesTaxSummaryRateLineResponse(
        sriTaxCode = sriTaxCode,
        sriRateCode = sriRateCode,
        taxKind = taxKind?.name,
        rate = rate.toPlainString(),
        treatment = treatment.name,
        base = base.toResponse(),
        taxAmount = taxAmount.toResponse(),
    )

private fun Money.toResponse(): SalesTotalsMoneyResponse =
    SalesTotalsMoneyResponse(amount = amount.toPlainString(), currency = currency.value)

private fun Quantity.toResponse(): SalesTotalsQuantityResponse =
    SalesTotalsQuantityResponse(value = value.toPlainString(), unitCode = unitCode, allowsDecimal = allowsDecimal)
