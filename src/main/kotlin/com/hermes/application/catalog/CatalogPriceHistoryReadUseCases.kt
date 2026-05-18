package com.hermes.application.catalog

import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant

data class CatalogPriceHistoryQuery(
    val organizationId: String,
    val catalogItemId: String,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = DEFAULT_LIMIT,
) {
    init {
        require(organizationId.isNotBlank()) { "Organization id is required for catalog price history query." }
        require(catalogItemId.isNotBlank()) { "Catalog item id is required for catalog price history query." }
        require(limit in 1..MAX_LIMIT) { "Catalog price history query limit must be between 1 and $MAX_LIMIT." }
        if (from != null && to != null) require(!from.isAfter(to)) { "Catalog price history query from cannot be after to." }
    }

    companion object {
        const val DEFAULT_LIMIT = 100
        const val MAX_LIMIT = 300
    }
}

interface CatalogPriceHistoryQueryRepository {
    fun search(query: CatalogPriceHistoryQuery): List<CatalogPriceHistory>
}

data class CatalogListPriceHistoryCommand(
    val organizationId: String,
    val catalogItemId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val from: Instant? = null,
    val to: Instant? = null,
    val limit: Int = CatalogPriceHistoryQuery.DEFAULT_LIMIT,
)

data class CatalogPriceHistoryResult(
    val history: List<CatalogPriceHistory>,
)

class CatalogListPriceHistoryUseCase(
    private val priceHistoryRepository: CatalogPriceHistoryQueryRepository,
    private val auditLogger: CatalogAuditLogger = NoopCatalogAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: CatalogListPriceHistoryCommand): CatalogPriceHistoryResult {
        assertCanViewPriceHistory(command.actorEffectivePermissions)

        val organizationId = command.organizationId.trim()
        val catalogItemId = command.catalogItemId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (catalogItemId.isBlank()) throw DomainRuleViolation("Catalog item id is required.")

        val query = CatalogPriceHistoryQuery(
            organizationId = organizationId,
            catalogItemId = catalogItemId,
            from = command.from,
            to = command.to,
            limit = command.limit,
        )

        val history = priceHistoryRepository.search(query)

        auditLogger.log(
            CatalogAuditEvent(
                action = CatalogAuditAction.CATALOG_PRICE_HISTORY_VIEWED,
                actorUserId = command.actorUserId,
                organizationId = organizationId,
                targetId = catalogItemId,
                after = mapOf(
                    "catalogItemId" to catalogItemId,
                    "from" to query.from?.toString(),
                    "to" to query.to?.toString(),
                    "limit" to query.limit.toString(),
                    "resultCount" to history.size.toString(),
                ),
                createdAt = Instant.now(clock),
            )
        )

        return CatalogPriceHistoryResult(history)
    }

    private fun assertCanViewPriceHistory(effectivePermissions: Set<String>) {
        val allowed = PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_PRICE_HISTORY_VIEW) ||
            PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_LOCAL_VIEW) ||
            PermissionRules.canPerform(effectivePermissions, PermissionCatalog.CATALOG_MANAGE_MASTER)

        if (!allowed) {
            throw DomainRuleViolation(
                "Missing any required permission: ${PermissionCatalog.CATALOG_PRICE_HISTORY_VIEW}, " +
                    "${PermissionCatalog.CATALOG_LOCAL_VIEW}, ${PermissionCatalog.CATALOG_MANAGE_MASTER}."
            )
        }
    }
}
