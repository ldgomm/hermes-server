package com.hermes.application.admin.catalog

import com.hermes.domain.permission.PermissionCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminCatalogApiContractTest {
    @Test
    fun `contains admin catalog phase 13C routes`() {
        val paths = AdminCatalogApiContract.routes.map { it.key }.toSet()

        assertTrue("GET /api/v1/admin/catalog/master/templates" in paths)
        assertTrue("GET /api/v1/admin/catalog/master/templates/{templateId}" in paths)
        assertTrue("POST /api/v1/admin/catalog/master/templates" in paths)

        assertTrue("GET /api/v1/admin/catalog/master/categories" in paths)
        assertTrue("POST /api/v1/admin/catalog/master/categories" in paths)
        assertTrue("GET /api/v1/admin/catalog/master/families" in paths)
        assertTrue("POST /api/v1/admin/catalog/master/families" in paths)

        assertTrue("GET /api/v1/admin/catalog/local/items" in paths)
        assertTrue("POST /api/v1/admin/catalog/local/items/copy-from-template" in paths)
        assertTrue("GET /api/v1/admin/catalog/local/items/{itemId}" in paths)
        assertTrue("PUT /api/v1/admin/catalog/local/items/{itemId}" in paths)
        assertTrue("POST /api/v1/admin/catalog/local/items/{itemId}/activate" in paths)
        assertTrue("POST /api/v1/admin/catalog/local/items/{itemId}/deactivate" in paths)
        assertTrue("POST /api/v1/admin/catalog/local/items/{itemId}/remove" in paths)

        assertTrue("GET /api/v1/admin/catalog/requests" in paths)
        assertTrue("POST /api/v1/admin/catalog/requests" in paths)
        assertTrue("GET /api/v1/admin/catalog/requests/{requestId}" in paths)
        assertTrue("POST /api/v1/admin/catalog/requests/{requestId}/review" in paths)
    }

    @Test
    fun `security matrix matches route contract`() {
        val routeKeys = AdminCatalogApiContract.routes.map { it.key }.toSet()
        val securityKeys = AdminCatalogSecurityContract.endpoints.map { it.key }.toSet()

        assertEquals(routeKeys, securityKeys)
        routeKeys.forEach { key ->
            val parts = key.split(' ', limit = 2)
            assertNotNull(AdminCatalogSecurityContract.find(parts[0], parts[1]), "Missing security entry for $key")
        }
    }

    @Test
    fun `catalog contract has expected read mutation and surface counts`() {
        assertEquals(18, AdminCatalogApiContract.routes.size)
        assertEquals(8, AdminCatalogSecurityContract.readEndpoints.size)
        assertEquals(10, AdminCatalogSecurityContract.mutationEndpoints.size)

        assertEquals(
            3, AdminCatalogSecurityContract.endpoints.count { it.surface == AdminCatalogSurface.MASTER_TEMPLATES })
        assertEquals(
            2, AdminCatalogSecurityContract.endpoints.count { it.surface == AdminCatalogSurface.MASTER_CATEGORIES })
        assertEquals(
            2, AdminCatalogSecurityContract.endpoints.count { it.surface == AdminCatalogSurface.MASTER_FAMILIES })
        assertEquals(7, AdminCatalogSecurityContract.endpoints.count { it.surface == AdminCatalogSurface.LOCAL_ITEMS })
        assertEquals(4, AdminCatalogSecurityContract.endpoints.count { it.surface == AdminCatalogSurface.REQUESTS })

        assertEquals(
            AdminCatalogSurface.entries.toSet(), AdminCatalogSecurityContract.endpoints.map { it.surface }.toSet()
        )
    }

    @Test
    fun `critical mutations are audited and reason protected`() {
        AdminCatalogSecurityContract.mutationEndpoints.filter { it.critical }.forEach { endpoint ->
                assertTrue(endpoint.audited, "${endpoint.key} must be audited")
                assertTrue(endpoint.requiresReason, "${endpoint.key} must require reason")
            }
    }

    @Test
    fun `platform-only routes require master catalog permission`() {
        AdminCatalogSecurityContract.platformOnlyEndpoints.forEach { endpoint ->
            assertTrue(PermissionCatalog.CATALOG_MANAGE_MASTER in endpoint.requiredPermissions, endpoint.key)
            assertTrue(endpoint.platformOnly, endpoint.key)
        }
    }

    @Test
    fun `local item routes are organization scoped`() {
        AdminCatalogSecurityContract.endpoints.filter { it.surface == AdminCatalogSurface.LOCAL_ITEMS }
            .forEach { endpoint -> assertTrue(endpoint.organizationScoped, endpoint.key) }
    }
}
