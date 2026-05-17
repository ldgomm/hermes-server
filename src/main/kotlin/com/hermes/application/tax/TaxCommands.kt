package com.hermes.application.tax

import com.hermes.domain.money.Money
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.tax.PriceTaxMode
import java.time.Instant

data class TaxCalculatePreviewCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val occurredAt: Instant,
    val lines: List<TaxCalculatePreviewLine>,
)

data class TaxCalculatePreviewLine(
    val lineId: String,
    val description: String,
    val quantity: Quantity,
    val unitPrice: Money,
    val discount: Money,
    val taxProfileCode: String,
    val priceTaxMode: PriceTaxMode = PriceTaxMode.TAX_EXCLUSIVE,
)
