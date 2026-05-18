package com.hermes.application.sales

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.PriceTaxMode
import com.hermes.domain.tax.TaxSummary
import java.time.Instant

data class PreviewSaleTotalsCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val occurredAt: Instant,
    val lines: List<PreviewSaleTotalsLine>,
)

data class PreviewSaleTotalsLine(
    val catalogItemId: String,
    val quantity: Quantity,
    val unitPrice: Money? = null,
    val discount: Money? = null,
    val priceTaxMode: PriceTaxMode = PriceTaxMode.TAX_EXCLUSIVE,
)

data class PreviewSaleTotalsResult(
    val organizationId: String,
    val occurredAt: Instant,
    val lines: List<PreviewSaleTotalsLineResult>,
    val summary: TaxSummary,
)

data class PreviewSaleTotalsLineResult(
    val lineId: String,
    val catalogItemId: String,
    val catalogItemName: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val discount: Money,
    val priceTaxMode: PriceTaxMode,
    val grossAmount: Money,
    val taxableBase: Money,
    val zeroRateBase: Money,
    val exemptBase: Money,
    val notSubjectBase: Money,
    val internalNoTaxBase: Money,
    val taxAmount: Money,
    val total: Money,
    val taxProfileCode: String,
    val sriTaxCode: String?,
    val sriRateCode: String?,
    val treatment: String,
)
