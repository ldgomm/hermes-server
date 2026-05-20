package com.hermes.application.admin.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminAccessPhase13BClosureContractTest {
    @Test
    fun `api contract contains all phase 13B endpoints without duplicates`() {
        val routes = AdminAccessApiContract.routes
        val keys = routes.map { it.key }

        assertEquals(20, routes.size)
        assertEquals(keys.size, keys.toSet().size, "Admin access route contract contains duplicated endpoints.")

        assertTrue("GET /api/v1/admin/users" in keys)
        assertTrue("POST /api/v1/admin/users/temporary" in keys)
        assertTrue("PUT /api/v1/admin/users/{userId}" in keys)
        assertTrue("POST /api/v1/admin/users/{userId}/block" in keys)
        assertTrue("POST /api/v1/admin/users/{userId}/unblock" in keys)
        assertTrue("POST /api/v1/admin/users/{userId}/reset-password" in keys)
        assertTrue("POST /api/v1/admin/users/{userId}/revoke-sessions" in keys)

        assertTrue("POST /api/v1/admin/invitations" in keys)
        assertTrue("GET /api/v1/admin/invitations" in keys)
        assertTrue("GET /api/v1/admin/invitations/{invitationId}" in keys)
        assertTrue("POST /api/v1/admin/invitations/{invitationId}/resend" in keys)
        assertTrue("POST /api/v1/admin/invitations/{invitationId}/revoke" in keys)

        assertTrue("GET /api/v1/admin/roles" in keys)
        assertTrue("POST /api/v1/admin/roles" in keys)
        assertTrue("GET /api/v1/admin/roles/{roleId}" in keys)
        assertTrue("PUT /api/v1/admin/roles/{roleId}" in keys)
        assertTrue("POST /api/v1/admin/roles/{roleId}/activate" in keys)
        assertTrue("POST /api/v1/admin/roles/{roleId}/deactivate" in keys)

        assertTrue("GET /api/v1/admin/permissions" in keys)
    }

    @Test
    fun `every api contract route has a security matrix entry`() {
        val routeKeys = AdminAccessApiContract.routes.map { it.key }.toSet()
        val securityKeys = AdminAccessSecurityContract.endpoints.map { it.key }.toSet()

        assertEquals(routeKeys, securityKeys)
        routeKeys.forEach { key ->
            val parts = key.split(" ", limit = 2)
            assertNotNull(AdminAccessSecurityContract.find(parts[0], parts[1]), "Missing security entry for $key")
        }
    }

    @Test
    fun `security matrix has expected read and mutation split`() {
        assertEquals(7, AdminAccessSecurityContract.readEndpoints.size)
        assertEquals(13, AdminAccessSecurityContract.mutationEndpoints.size)

        assertEquals(8, AdminAccessSecurityContract.endpoints.count { it.surface == AdminAccessSurface.USERS })
        assertEquals(5, AdminAccessSecurityContract.endpoints.count { it.surface == AdminAccessSurface.INVITATIONS })
        assertEquals(6, AdminAccessSecurityContract.endpoints.count { it.surface == AdminAccessSurface.ROLES })
        assertEquals(1, AdminAccessSecurityContract.endpoints.count { it.surface == AdminAccessSurface.PERMISSIONS })
    }

    @Test
    fun `all mutation endpoints require reason audit and explicit permission`() {
        AdminAccessSecurityContract.mutationEndpoints.forEach { endpoint ->
            assertTrue(endpoint.requiresReason, "${endpoint.key} must require reason.")
            assertTrue(endpoint.audited, "${endpoint.key} must be auditable.")
            assertTrue(endpoint.critical, "${endpoint.key} must be marked critical.")
            assertTrue(endpoint.requiredPermissions.isNotEmpty(), "${endpoint.key} must declare permissions.")
            assertTrue(endpoint.requiredPermissions.none { it.isBlank() }, "${endpoint.key} has blank permission.")
        }
    }

    @Test
    fun `read endpoints do not require mutation reason and still declare permission`() {
        AdminAccessSecurityContract.readEndpoints.forEach { endpoint ->
            assertTrue(!endpoint.requiresReason, "${endpoint.key} should not require reason.")
            assertTrue(!endpoint.audited, "${endpoint.key} should not be audited as mutation.")
            assertTrue(endpoint.requiredPermissions.isNotEmpty(), "${endpoint.key} must declare permissions.")
        }
    }

    @Test
    fun `closure report matches executable contracts`() {
        val report = AdminAccessPhase13BClosure.report

        assertEquals("13B", report.phase)
        assertEquals(AdminAccessApiContract.routes.size, report.routeCount)
        assertEquals(AdminAccessSecurityContract.readEndpoints.size, report.readRouteCount)
        assertEquals(AdminAccessSecurityContract.mutationEndpoints.size, report.mutationRouteCount)
        assertTrue(report.totalRouteCountMatchesContract)
        assertTrue(report.coversAllSurfaces)
        assertTrue(report.completedCapabilities.size >= 10)
        assertTrue(report.safetyRules.size >= 10)
        assertEquals("13C — Catalog Admin API", report.nextRecommendedPhase)
    }
}
