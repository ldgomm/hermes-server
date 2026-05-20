package com.hermes.application.admin.catalog

import com.hermes.domain.permission.PermissionCatalog

/**
 * Executable REST contract for Fase 13C.1 — Catalog Admin API.
 *
 * Keep this contract intentionally framework-free. It is consumed by tests,
 * Admin iOS/Web planning, generated docs and smoke-test checklists before the
 * actual Ktor route implementation lands in the following 13C sub-sprints.
 */
object AdminCatalogApiContract {
    val routes: List<AdminCatalogRouteContract> = listOf(
        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/master/templates", "Search platform catalog templates"),
        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/master/templates/{templateId}", "Get platform catalog template detail"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/master/templates", "Create platform catalog template"),

        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/master/categories", "List platform catalog categories"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/master/categories", "Create platform catalog category"),
        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/master/families", "List platform catalog families"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/master/families", "Create platform catalog family"),

        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/local/items", "List organization catalog items"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/local/items/copy-from-template", "Copy template into organization catalog"),
        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/local/items/{itemId}", "Get organization catalog item detail"),
        AdminCatalogRouteContract("PUT", "/api/v1/admin/catalog/local/items/{itemId}", "Update organization catalog item"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/local/items/{itemId}/activate", "Activate organization catalog item"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/local/items/{itemId}/deactivate", "Deactivate organization catalog item"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/local/items/{itemId}/remove", "Remove organization catalog item without destructive delete"),

        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/requests", "List organization catalog requests"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/requests", "Create catalog item request"),
        AdminCatalogRouteContract("GET", "/api/v1/admin/catalog/requests/{requestId}", "Get catalog item request detail"),
        AdminCatalogRouteContract("POST", "/api/v1/admin/catalog/requests/{requestId}/review", "Review catalog item request"),
    )
}

data class AdminCatalogRouteContract(
    val method: String,
    val path: String,
    val description: String,
) {
    val key: String get() = "$method $path"
}

/**
 * Permission, audit and organization-scope matrix for the Admin Catalog API.
 *
 * The matrix deliberately models "ANY" permission groups because some catalog
 * actions can be performed by different operational roles. For example, changing
 * a local item may require local-update, price-update or tax-profile permission
 * depending on the exact fields in the request body; the use case still performs
 * field-level validation.
 */
object AdminCatalogSecurityContract {
    val endpoints: List<AdminCatalogEndpointSecurityContract> = listOf(
        // Master templates.
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/master/templates",
            surface = AdminCatalogSurface.MASTER_TEMPLATES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = true,
            critical = false,
            note = "Business admins may search active master templates before copying them; platform admins may see the same contract for governance.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/master/templates/{templateId}",
            surface = AdminCatalogSurface.MASTER_TEMPLATES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = true,
            critical = false,
            note = "Read-only template detail for copy and review flows.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/master/templates",
            surface = AdminCatalogSurface.MASTER_TEMPLATES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = false,
            platformOnly = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates governed platform templates only from platform/master catalog roles.",
        ),

        // Master categories and families.
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/master/categories",
            surface = AdminCatalogSurface.MASTER_CATEGORIES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists public active category structure for Admin clients.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/master/categories",
            surface = AdminCatalogSurface.MASTER_CATEGORIES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = false,
            platformOnly = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates governed platform categories.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/master/families",
            surface = AdminCatalogSurface.MASTER_FAMILIES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists platform product/service families for grouping and template discovery.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/master/families",
            surface = AdminCatalogSurface.MASTER_FAMILIES,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = false,
            platformOnly = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Creates governed platform product/service families.",
        ),

        // Local catalog.
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/local/items",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Lists only items belonging to the active organization.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/local/items/copy-from-template",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_COPY_FROM_MASTER),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Requires valid activity, price and tax profile before creating a local catalog item.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/local/items/{itemId}",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Reads one local item scoped to the active organization.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "PUT",
            path = "/api/v1/admin/catalog/local/items/{itemId}",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(
                PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY,
                PermissionCatalog.CATALOG_LOCAL_CHANGE_PRICE,
                PermissionCatalog.CATALOG_LOCAL_CHANGE_TAX_PROFILE,
            ),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = true,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Field-level use case checks still decide whether name, price or tax profile can be changed.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/local/items/{itemId}/activate",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_UPDATE_LOCAL_COPY),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Reactivates a paused/draft item without bypassing tax profile validation.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/local/items/{itemId}/deactivate",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Pauses a local item without destructive deletion.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/local/items/{itemId}/remove",
            surface = AdminCatalogSurface.LOCAL_ITEMS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_DISABLE_LOCAL_COPY),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Marks the item as removed from account; routes must never hard-delete used catalog items.",
        ),

        // Catalog requests.
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/requests",
            surface = AdminCatalogSurface.REQUESTS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Organization admins see their own requests; platform reviewers may use this contract for review queues.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/requests",
            surface = AdminCatalogSurface.REQUESTS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_REQUEST_NEW_ITEM),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = true,
            critical = false,
            note = "The request body is the justification; no separate reason is required for the business user.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "GET",
            path = "/api/v1/admin/catalog/requests/{requestId}",
            surface = AdminCatalogSurface.REQUESTS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_LOCAL_VIEW, PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ANY,
            mutation = false,
            organizationScoped = true,
            platformOnly = false,
            requiresReason = false,
            audited = false,
            critical = false,
            note = "Reads one catalog request scoped either to organization or platform review context.",
        ),
        AdminCatalogEndpointSecurityContract(
            method = "POST",
            path = "/api/v1/admin/catalog/requests/{requestId}/review",
            surface = AdminCatalogSurface.REQUESTS,
            requiredPermissions = setOf(PermissionCatalog.CATALOG_MANAGE_MASTER),
            permissionMode = AdminCatalogPermissionMode.ALL,
            mutation = true,
            organizationScoped = false,
            platformOnly = true,
            requiresReason = true,
            audited = true,
            critical = true,
            note = "Approves, rejects, links to existing template or asks for more information.",
        ),
    )

    val mutationEndpoints: List<AdminCatalogEndpointSecurityContract>
        get() = endpoints.filter { it.mutation }

    val readEndpoints: List<AdminCatalogEndpointSecurityContract>
        get() = endpoints.filterNot { it.mutation }

    val platformOnlyEndpoints: List<AdminCatalogEndpointSecurityContract>
        get() = endpoints.filter { it.platformOnly }

    fun find(method: String, path: String): AdminCatalogEndpointSecurityContract? =
        endpoints.firstOrNull { it.method == method.uppercase() && it.path == path }
}

data class AdminCatalogEndpointSecurityContract(
    val method: String,
    val path: String,
    val surface: AdminCatalogSurface,
    val requiredPermissions: Set<String>,
    val permissionMode: AdminCatalogPermissionMode,
    val mutation: Boolean,
    val organizationScoped: Boolean,
    val platformOnly: Boolean,
    val requiresReason: Boolean,
    val audited: Boolean,
    val critical: Boolean,
    val note: String,
) {
    val key: String get() = "$method $path"
}

enum class AdminCatalogPermissionMode {
    ANY,
    ALL,
}

enum class AdminCatalogSurface {
    MASTER_TEMPLATES,
    MASTER_CATEGORIES,
    MASTER_FAMILIES,
    LOCAL_ITEMS,
    REQUESTS,
}
