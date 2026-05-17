package com.hermes.domain.catalog

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class CatalogPriceHistory(
    val id: String,
    val organizationId: String,
    val catalogItemId: String,
    val oldPrice: Money,
    val newPrice: Money,
    val changedByUserId: String,
    val reason: String,
    val changedAt: Instant,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Catalog price history id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (catalogItemId.isBlank()) throw DomainRuleViolation("Catalog item id cannot be blank.")
        if (oldPrice.currency != newPrice.currency) throw DomainRuleViolation("Catalog price history requires same currency.")
        if (changedByUserId.isBlank()) throw DomainRuleViolation("Catalog price history changedBy cannot be blank.")
        if (reason.isBlank()) throw DomainRuleViolation("Catalog price history reason cannot be blank.")
    }
}
