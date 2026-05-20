package com.hermes.application.admin.operations

import com.hermes.application.auth.AuthorizationPolicy
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation

class SearchAdminSalesUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: SearchAdminSalesCommand): AdminSalesResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.limit.requireLimit(max = 250)
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
        )
        return AdminSalesResult(repository.searchSales(command))
    }
}

class GetAdminSaleUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetAdminSaleCommand): AdminSaleResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.saleId.requireNotBlank("Sale id")
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
        )
        return AdminSaleResult(
            repository.findSale(command) ?: throw DomainRuleViolation("Sale does not exist in this organization."),
        )
    }
}

class SearchAdminCashSessionsUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: SearchAdminCashSessionsCommand): AdminCashSessionsResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.limit.requireLimit(max = 250)
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(
                PermissionCatalog.CASH_VIEW,
                PermissionCatalog.CASH_SESSION_VIEW_HISTORY,
                PermissionCatalog.REPORTS_CASH_VIEW
            ),
        )
        return AdminCashSessionsResult(repository.searchCashSessions(command))
    }
}

class GetCurrentAdminCashSessionUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetCurrentAdminCashSessionCommand): AdminCashSessionResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(
                PermissionCatalog.CASH_VIEW,
                PermissionCatalog.CASH_SESSION_VIEW_CURRENT,
                PermissionCatalog.REPORTS_CASH_VIEW
            ),
        )
        return AdminCashSessionResult(repository.findCurrentCashSession(command))
    }
}

class GetAdminCashSessionUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetAdminCashSessionCommand): AdminCashSessionResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.cashSessionId.requireNotBlank("Cash session id")
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(
                PermissionCatalog.CASH_VIEW,
                PermissionCatalog.CASH_SESSION_VIEW_HISTORY,
                PermissionCatalog.REPORTS_CASH_VIEW
            ),
        )
        return AdminCashSessionResult(
            repository.findCashSession(command)
                ?: throw DomainRuleViolation("Cash session does not exist in this organization."),
        )
    }
}

class SearchAdminPaymentsUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: SearchAdminPaymentsCommand): AdminPaymentsResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.limit.requireLimit(max = 300)
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.PAYMENTS_VIEW, PermissionCatalog.REPORTS_CASH_VIEW),
        )
        return AdminPaymentsResult(repository.searchPayments(command))
    }
}

class SearchAdminReceivablesUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: SearchAdminReceivablesCommand): AdminReceivablesResult {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.limit.requireLimit(max = 300)
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.RECEIVABLES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
        )
        return AdminReceivablesResult(repository.searchReceivables(command))
    }
}

class GetAdminOperationalTodayReportUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetAdminOperationalTodayReportCommand): AdminOperationalTodayReport {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.requireValidRange()
        AuthorizationPolicy.requireAny(
            command.actorEffectivePermissions,
            setOf(PermissionCatalog.REPORTS_DASHBOARD_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
        )
        return repository.operationalToday(command)
    }
}

class GetAdminSalesSummaryReportUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetAdminSalesSummaryReportCommand): AdminSalesSummaryReport {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.requireValidRange()
        AuthorizationPolicy.requireAny(command.actorEffectivePermissions, setOf(PermissionCatalog.REPORTS_SALES_VIEW))
        return repository.salesSummary(command)
    }
}

class GetAdminCashSummaryReportUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetAdminCashSummaryReportCommand): AdminCashSummaryReport {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.requireValidRange()
        AuthorizationPolicy.requireAny(command.actorEffectivePermissions, setOf(PermissionCatalog.REPORTS_CASH_VIEW))
        return repository.cashSummary(command)
    }
}

class GetAdminTaxSummaryReportUseCase(private val repository: AdminOperationsQueryRepository) {
    fun execute(command: GetAdminTaxSummaryReportCommand): AdminTaxSummaryReport {
        command.organizationId.requireNotBlank("Organization id")
        command.actorUserId.requireNotBlank("Actor user id")
        command.requireValidRange()
        AuthorizationPolicy.requireAny(command.actorEffectivePermissions, setOf(PermissionCatalog.REPORTS_TAX_VIEW))
        return repository.taxSummary(command)
    }
}

private fun String.requireNotBlank(label: String): String =
    trim().takeIf { it.isNotBlank() } ?: throw DomainRuleViolation("$label cannot be blank.")

private fun Int.requireLimit(max: Int) {
    if (this !in 1..max) throw DomainRuleViolation("Limit must be between 1 and $max.")
}

private fun GetAdminOperationalTodayReportCommand.requireValidRange() {
    if (!from.isBefore(to)) throw DomainRuleViolation("Report 'from' must be before 'to'.")
}

private fun GetAdminSalesSummaryReportCommand.requireValidRange() {
    if (!from.isBefore(to)) throw DomainRuleViolation("Report 'from' must be before 'to'.")
}

private fun GetAdminCashSummaryReportCommand.requireValidRange() {
    if (!from.isBefore(to)) throw DomainRuleViolation("Report 'from' must be before 'to'.")
}

private fun GetAdminTaxSummaryReportCommand.requireValidRange() {
    if (!from.isBefore(to)) throw DomainRuleViolation("Report 'from' must be before 'to'.")
}
