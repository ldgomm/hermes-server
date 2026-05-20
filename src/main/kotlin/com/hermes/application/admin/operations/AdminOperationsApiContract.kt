package com.hermes.application.admin.operations

import com.hermes.domain.permission.PermissionCatalog

/**
 * Executable REST contract for Fase 13E — Sales, Cash & Reports Admin API.
 *
 * This contract is intentionally framework-free so Admin iOS/Web Admin,
 * smoke tests and lightweight generated docs can detect endpoint drift without Ktor.
 */
object AdminOperationsApiContract {
    val routes: List<AdminOperationsRouteContract> = listOf(
        AdminOperationsRouteContract("GET", "/api/v1/admin/sales", "Search sales"),
        AdminOperationsRouteContract("GET", "/api/v1/admin/sales/{saleId}", "Get sale detail"),

        AdminOperationsRouteContract("GET", "/api/v1/admin/cash-sessions", "Search cash sessions"),
        AdminOperationsRouteContract("GET", "/api/v1/admin/cash-sessions/current", "Get current/open cash session"),
        AdminOperationsRouteContract("GET", "/api/v1/admin/cash-sessions/{cashSessionId}", "Get cash session detail"),

        AdminOperationsRouteContract("GET", "/api/v1/admin/payments", "Search payments"),
        AdminOperationsRouteContract("GET", "/api/v1/admin/receivables", "Search receivables"),

        AdminOperationsRouteContract(
            "GET", "/api/v1/admin/reports/operational-today", "Get operational today dashboard"
        ),
        AdminOperationsRouteContract("GET", "/api/v1/admin/reports/sales-summary", "Get sales summary report"),
        AdminOperationsRouteContract("GET", "/api/v1/admin/reports/cash-summary", "Get cash summary report"),
        AdminOperationsRouteContract("GET", "/api/v1/admin/reports/tax-summary", "Get tax summary report"),
    )
}

data class AdminOperationsRouteContract(
    val method: String,
    val path: String,
    val description: String,
) {
    val key: String get() = "$method $path"
}

object AdminOperationsSecurityContract {
    val endpoints: List<AdminOperationsEndpointSecurityContract> = listOf(
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/sales",
            surface = AdminOperationsSurface.SALES,
            requiredPermissions = setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists organization-scoped sales for daily admin visibility.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/sales/{saleId}",
            surface = AdminOperationsSurface.SALES,
            requiredPermissions = setOf(PermissionCatalog.SALES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Returns sale detail with safe line/payment/document snapshots for Admin clients.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/cash-sessions",
            surface = AdminOperationsSurface.CASH_SESSIONS,
            requiredPermissions = setOf(
                PermissionCatalog.CASH_VIEW,
                PermissionCatalog.CASH_SESSION_VIEW_HISTORY,
                PermissionCatalog.REPORTS_CASH_VIEW,
            ),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists historical/open cash sessions with summary amounts.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/cash-sessions/current",
            surface = AdminOperationsSurface.CASH_SESSIONS,
            requiredPermissions = setOf(
                PermissionCatalog.CASH_VIEW,
                PermissionCatalog.CASH_SESSION_VIEW_CURRENT,
                PermissionCatalog.REPORTS_CASH_VIEW,
            ),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Returns the open cash session for the selected branch/organization if one exists.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/cash-sessions/{cashSessionId}",
            surface = AdminOperationsSurface.CASH_SESSIONS,
            requiredPermissions = setOf(
                PermissionCatalog.CASH_VIEW,
                PermissionCatalog.CASH_SESSION_VIEW_HISTORY,
                PermissionCatalog.REPORTS_CASH_VIEW,
            ),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Returns one cash session plus its movements without exposing internal Mongo documents.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/payments",
            surface = AdminOperationsSurface.PAYMENTS,
            requiredPermissions = setOf(PermissionCatalog.PAYMENTS_VIEW, PermissionCatalog.REPORTS_CASH_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists payment records for reconciliation screens.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/receivables",
            surface = AdminOperationsSurface.RECEIVABLES,
            requiredPermissions = setOf(PermissionCatalog.RECEIVABLES_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists accounts receivable and balances for admin follow-up.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/reports/operational-today",
            surface = AdminOperationsSurface.REPORTS,
            requiredPermissions = setOf(PermissionCatalog.REPORTS_DASHBOARD_VIEW, PermissionCatalog.REPORTS_SALES_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Mobile-first daily dashboard: sales, cash, receivables, documents, top items and alerts.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/reports/sales-summary",
            surface = AdminOperationsSurface.REPORTS,
            requiredPermissions = setOf(PermissionCatalog.REPORTS_SALES_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Sales summary grouped by operational/payment/document status for a date range.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/reports/cash-summary",
            surface = AdminOperationsSurface.REPORTS,
            requiredPermissions = setOf(PermissionCatalog.REPORTS_CASH_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Cash summary built from cash sessions and cash movements.",
        ),
        AdminOperationsEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/reports/tax-summary",
            surface = AdminOperationsSurface.REPORTS,
            requiredPermissions = setOf(PermissionCatalog.REPORTS_TAX_VIEW),
            permissionMode = AdminOperationsPermissionMode.ANY,
            organizationScoped = true,
            mutation = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Tax summary based on immutable sale/document tax snapshots. It does not recalculate historic taxes.",
        ),
    )

    val readEndpoints: List<AdminOperationsEndpointSecurityContract>
        get() = endpoints.filterNot { it.mutation }

    val mutationEndpoints: List<AdminOperationsEndpointSecurityContract>
        get() = endpoints.filter { it.mutation }

    fun find(method: String, path: String): AdminOperationsEndpointSecurityContract? =
        endpoints.firstOrNull { it.method == method.uppercase() && it.path == path }
}

data class AdminOperationsEndpointSecurityContract(
    val method: String,
    val path: String,
    val surface: AdminOperationsSurface,
    val requiredPermissions: Set<String>,
    val permissionMode: AdminOperationsPermissionMode,
    val organizationScoped: Boolean,
    val mutation: Boolean,
    val requiresReason: Boolean,
    val audited: Boolean,
    val critical: Boolean,
    val note: String,
) {
    val key: String get() = "$method $path"
}

enum class AdminOperationsSurface {
    SALES, CASH_SESSIONS, PAYMENTS, RECEIVABLES, REPORTS,
}

enum class AdminOperationsPermissionMode {
    ALL, ANY,
}
