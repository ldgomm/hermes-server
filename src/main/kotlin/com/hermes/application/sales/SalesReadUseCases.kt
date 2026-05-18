package com.hermes.application.sales

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation

/**
 * Read-side sales search use case.
 *
 * Important:
 * Do not name this class SearchSalesUseCase because Fase 8 already has the
 * operational SearchSalesUseCase backed by OperationalSaleRepository.
 */
class SearchSalesReadUseCase(
    private val repository: SalesReadRepository,
) {
    fun execute(command: SalesSearchCommand): SalesSearchResult {
        requireOrganization(command.organizationId)
        requireLimit(command.limit, max = 200)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)
        return SalesSearchResult(repository.search(command))
    }
}

class ListPendingSalesUseCase(
    private val repository: SalesReadRepository,
) {
    fun execute(command: PendingSalesCommand): PendingSalesResult {
        requireOrganization(command.organizationId)
        requireLimit(command.limit, max = 300)
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)
        return PendingSalesResult(repository.findPending(command))
    }
}

class GetSalesDaySummaryUseCase(
    private val repository: SalesReadRepository,
) {
    fun execute(command: SalesDaySummaryCommand): SalesDaySummaryResult {
        requireOrganization(command.organizationId)
        if (!command.from.isBefore(command.to)) {
            throw DomainRuleViolation("Sales summary 'from' must be before 'to'.")
        }
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.SALES_VIEW)
        return repository.summarizeDay(command)
    }
}

private fun requireOrganization(organizationId: String) {
    if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
}

private fun requireLimit(limit: Int, max: Int) {
    if (limit !in 1..max) throw DomainRuleViolation("Limit must be between 1 and $max.")
}
