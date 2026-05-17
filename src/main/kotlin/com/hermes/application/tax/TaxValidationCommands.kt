package com.hermes.application.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.PriceTaxMode
import com.hermes.domain.tax.TaxCalculationResult
import com.hermes.domain.tax.TaxEmissionType
import com.hermes.domain.tax.TaxLineResult
import com.hermes.domain.tax.TaxProfileSnapshot
import java.time.Instant

data class TaxSaleValidationCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val occurredAt: Instant,
    val lines: List<TaxSaleValidationLine>,
)

data class TaxSaleValidationLine(
    val lineId: String,
    val catalogItemId: String? = null,
    val description: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val discount: Money = Money.zero(unitPrice.currency),
    val taxProfileCode: String,
    val priceTaxMode: PriceTaxMode = PriceTaxMode.TAX_EXCLUSIVE,
)

data class TaxSaleValidationResult(
    val organizationId: String,
    val occurredAt: Instant,
    val calculation: TaxCalculationResult,
)

data class TaxDocumentEmissionValidationCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val emissionType: TaxEmissionType,
    val occurredAt: Instant,
    val lines: List<TaxSaleValidationLine>,
)

data class TaxDocumentEmissionValidationResult(
    val organizationId: String,
    val emissionType: TaxEmissionType,
    val occurredAt: Instant,
    val calculation: TaxCalculationResult,
)

data class TaxPreparedSaleLine(
    val lineId: String,
    val catalogItemId: String?,
    val taxProfileSnapshot: TaxProfileSnapshot,
    val taxLineResult: TaxLineResult,
)

data class TaxPreparedSalePayload(
    val organizationId: String,
    val occurredAt: Instant,
    val lines: List<TaxPreparedSaleLine>,
    val calculation: TaxCalculationResult,
)
