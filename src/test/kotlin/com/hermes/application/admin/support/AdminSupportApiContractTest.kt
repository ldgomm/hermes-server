package com.hermes.application.admin.support

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminSupportApiContractTest {
    @Test
    fun `contract exposes exactly 13F routes`() {
        val keys = AdminSupportApiContract.routes.map { it.key }.toSet()

        assertEquals(5, AdminSupportApiContract.routes.size)
        assertTrue("GET /api/v1/admin/audit/logs" in keys)
        assertTrue("GET /api/v1/admin/audit/timeline" in keys)
        assertTrue("GET /api/v1/admin/support/diagnostics" in keys)
        assertTrue("GET /api/v1/admin/support/permissions" in keys)
        assertTrue("GET /api/v1/admin/support/modules" in keys)
    }

    @Test
    fun `security contract is read-only organization scoped and does not allow secrets`() {
        assertEquals(AdminSupportApiContract.routes.map { it.key }.toSet(), AdminSupportSecurityContract.endpoints.map { it.key }.toSet())
        assertEquals(5, AdminSupportSecurityContract.readEndpoints.size)
        assertEquals(0, AdminSupportSecurityContract.mutationEndpoints.size)

        AdminSupportSecurityContract.endpoints.forEach { endpoint ->
            assertTrue(endpoint.organizationScoped, endpoint.key)
            assertFalse(endpoint.mutation, endpoint.key)
            assertFalse(endpoint.requiresReason, endpoint.key)
            assertFalse(endpoint.secretsAllowed, endpoint.key)
            assertTrue(endpoint.requiredPermissions.isNotEmpty(), endpoint.key)
            assertNotNull(AdminSupportSecurityContract.find(endpoint.method, endpoint.path))
        }
    }

    @Test
    fun `closure report matches contract`() {
        val report = AdminSupportPhase13FClosure.report

        assertEquals("13F", report.phase)
        assertTrue(report.totalRouteCountMatchesContract)
        assertTrue(report.coversAllSurfaces)
        assertEquals(AdminSupportApiContract.routes.size, report.routeCount)
        assertTrue(report.safetyRules.any { it.contains("never expose", ignoreCase = true) })
    }
}
