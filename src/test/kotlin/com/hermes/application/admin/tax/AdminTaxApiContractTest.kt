package com.hermes.application.admin.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminTaxApiContractTest {
    @Test
    fun `contract exposes expected 13D routes`() {
        assertEquals(10, AdminTaxApiContract.routes.size)
        assertNotNull(AdminTaxSecurityContract.find("GET", "/api/v1/admin/tax/profiles"))
        assertNotNull(AdminTaxSecurityContract.find("POST", "/api/v1/admin/tax/profiles"))
        assertNotNull(AdminTaxSecurityContract.find("GET", "/api/v1/admin/tax/rates"))
        assertNotNull(AdminTaxSecurityContract.find("POST", "/api/v1/admin/tax/rates"))
        assertNotNull(AdminTaxSecurityContract.find("POST", "/api/v1/admin/catalog/local/items/{itemId}/tax-profile"))
        assertNotNull(AdminTaxSecurityContract.find("GET", "/api/v1/admin/tax/readiness"))
    }

    @Test
    fun `route keys are unique and security matrix matches route contract`() {
        val routeKeys = AdminTaxApiContract.routes.map { it.key }
        val securityKeys = AdminTaxSecurityContract.endpoints.map { it.key }

        assertEquals(routeKeys.size, routeKeys.toSet().size)
        assertEquals(routeKeys.toSet(), securityKeys.toSet())
    }

    @Test
    fun `every mutation requires reason and audit`() {
        assertTrue(AdminTaxSecurityContract.mutationEndpoints.isNotEmpty())
        AdminTaxSecurityContract.mutationEndpoints.forEach { endpoint ->
            assertTrue(endpoint.requiresReason, "${endpoint.key} must require reason")
            assertTrue(endpoint.audited, "${endpoint.key} must be auditable")
            assertTrue(endpoint.critical, "${endpoint.key} must be marked critical")
        }
    }

    @Test
    fun `closure report matches contract`() {
        val report = AdminTaxPhase13DClosure.report

        assertTrue(report.totalRouteCountMatchesContract)
        assertTrue(report.coversAllSurfaces)
        assertEquals("13E — Sales, Cash & Reports Admin API", report.nextRecommendedPhase)
    }
}
