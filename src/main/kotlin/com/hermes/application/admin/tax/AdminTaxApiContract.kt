package com.hermes.application.admin.tax

import com.hermes.domain.permission.PermissionCatalog

/**
 * Executable REST contract for Fase 13D — Tax Admin API.
 *
 * This contract is intentionally framework-free so Admin iOS/Web Admin,
 * smoke tests and lightweight docs can validate endpoint drift without Ktor.
 */
object AdminTaxApiContract {
    val routes: List<AdminTaxRouteContract> = listOf(
        AdminTaxRouteContract("GET", "/api/v1/admin/tax/profiles", "Search tax profiles"),
        AdminTaxRouteContract("POST", "/api/v1/admin/tax/profiles", "Create tax profile"),
        AdminTaxRouteContract("GET", "/api/v1/admin/tax/profiles/{profileId}", "Get tax profile detail"),
        AdminTaxRouteContract("PUT", "/api/v1/admin/tax/profiles/{profileId}", "Update tax profile"),

        AdminTaxRouteContract("GET", "/api/v1/admin/tax/rates", "Search tax rates"),
        AdminTaxRouteContract("POST", "/api/v1/admin/tax/rates", "Create tax rate"),
        AdminTaxRouteContract("GET", "/api/v1/admin/tax/rates/{rateId}", "Get tax rate detail"),
        AdminTaxRouteContract("PUT", "/api/v1/admin/tax/rates/{rateId}", "Update tax rate"),

        AdminTaxRouteContract(
            "POST",
            "/api/v1/admin/catalog/local/items/{itemId}/tax-profile",
            "Assign tax profile to local catalog item",
        ),

        AdminTaxRouteContract("GET", "/api/v1/admin/tax/readiness", "Get tax readiness"),
    )
}

data class AdminTaxRouteContract(
    val method: String,
    val path: String,
    val description: String,
) {
    val key: String get() = "$method $path"
}

object AdminTaxSecurityContract {
    val endpoints: List<AdminTaxEndpointSecurityContract> = listOf(
        AdminTaxEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/tax/profiles",
            surface = AdminTaxSurface.PROFILES,
            requiredPermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists tax profiles for the active organization context without exposing domain internals.",
        ),
        AdminTaxEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/tax/profiles",
            surface = AdminTaxSurface.PROFILES,
            requiredPermissions = setOf(PermissionCatalog.TAX_MANAGE),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates a configurable/versioned profile; no tax rate is hardcoded in the route.",
        ),
        AdminTaxEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/tax/profiles/{profileId}",
            surface = AdminTaxSurface.PROFILES,
            requiredPermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Reads one tax profile for admin screens.",
        ),
        AdminTaxEndpointSecurityContract(
            method = "PUT",
            path = "/api/v1/admin/tax/profiles/{profileId}",
            surface = AdminTaxSurface.PROFILES,
            requiredPermissions = setOf(PermissionCatalog.TAX_MANAGE),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Updates profile metadata, rate binding, SRI codes and vigency. Historic sale/document snapshots are not recalculated.",
        ),

        AdminTaxEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/tax/rates",
            surface = AdminTaxSurface.RATES,
            requiredPermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists/searches tax rates with optional status/kind/effective-at filters.",
        ),
        AdminTaxEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/tax/rates",
            surface = AdminTaxSurface.RATES,
            requiredPermissions = setOf(PermissionCatalog.TAX_MANAGE),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates a versioned tax rate with explicit vigency and legal basis.",
        ),
        AdminTaxEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/tax/rates/{rateId}",
            surface = AdminTaxSurface.RATES,
            requiredPermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Reads one tax rate for admin screens.",
        ),
        AdminTaxEndpointSecurityContract(
            method = "PUT",
            path = "/api/v1/admin/tax/rates/{rateId}",
            surface = AdminTaxSurface.RATES,
            requiredPermissions = setOf(PermissionCatalog.TAX_MANAGE),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Updates a rate as configuration; sale/document snapshots preserve their captured rate.",
        ),

        AdminTaxEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/local/items/{itemId}/tax-profile",
            surface = AdminTaxSurface.CATALOG_ASSIGNMENT,
            requiredPermissions = setOf(
                PermissionCatalog.TAX_PROFILES_ASSIGN_TO_ITEM,
                PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE,
            ),
            permissionMode = AdminTaxPermissionMode.ANY,
            mutation = true,
            organizationScoped = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Assigns only enabled organization tax profiles to local catalog items.",
        ),

        AdminTaxEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/tax/readiness",
            surface = AdminTaxSurface.READINESS,
            requiredPermissions = setOf(PermissionCatalog.TAX_SETTINGS_VIEW),
            permissionMode = AdminTaxPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Returns mobile-friendly status and next actions for tax configuration.",
        ),
    )

    val mutationEndpoints: List<AdminTaxEndpointSecurityContract>
        get() = endpoints.filter { it.mutation }

    val readEndpoints: List<AdminTaxEndpointSecurityContract>
        get() = endpoints.filterNot { it.mutation }

    fun find(method: String, path: String): AdminTaxEndpointSecurityContract? =
        endpoints.firstOrNull { it.method == method.uppercase() && it.path == path }
}

data class AdminTaxEndpointSecurityContract(
    val method: String,
    val path: String,
    val surface: AdminTaxSurface,
    val requiredPermissions: Set<String>,
    val permissionMode: AdminTaxPermissionMode,
    val mutation: Boolean,
    val organizationScoped: Boolean,
    val requiresReason: Boolean,
    val audited: Boolean,
    val critical: Boolean,
    val note: String,
) {
    val key: String get() = "$method $path"
}

enum class AdminTaxSurface {
    PROFILES,
    RATES,
    CATALOG_ASSIGNMENT,
    READINESS,
}

enum class AdminTaxPermissionMode {
    ALL,
    ANY,
}
