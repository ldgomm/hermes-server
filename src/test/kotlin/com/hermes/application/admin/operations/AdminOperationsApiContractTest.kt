package com.hermes.application.admin.operations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdminOperationsApiContractTest {
    @Test
    fun `contract exposes expected 13E routes`() {
        assertEquals(11, AdminOperationsApiContract.routes.size)
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/sales"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/sales/{saleId}"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/cash-sessions"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/cash-sessions/current"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/payments"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/receivables"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/reports/operational-today"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/reports/sales-summary"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/reports/cash-summary"))
        assertNotNull(AdminOperationsSecurityContract.find("GET", "/api/v1/admin/reports/tax-summary"))
    }

    @Test
    fun `route keys are unique and security matrix matches route contract`() {
        val routeKeys = AdminOperationsApiContract.routes.map { it.key }
        val securityKeys = AdminOperationsSecurityContract.endpoints.map { it.key }

        assertEquals(routeKeys.size, routeKeys.toSet().size)
        assertEquals(routeKeys.toSet(), securityKeys.toSet())
    }

    @Test
    fun `13E is read only`() {
        assertTrue(AdminOperationsSecurityContract.mutationEndpoints.isEmpty())
        AdminOperationsSecurityContract.readEndpoints.forEach { endpoint ->
            assertTrue(endpoint.organizationScoped, "${endpoint.key} must be organization-scoped")
            assertTrue(!endpoint.requiresReason, "${endpoint.key} is read-only and must not require reason")
            assertTrue(!endpoint.audited, "${endpoint.key} is read-only and should not force audit event")
        }
    }

    @Test
    fun `closure report matches contract`() {
        val report = AdminOperationsPhase13EClosure.report

        assertTrue(report.totalRouteCountMatchesContract)
        assertTrue(report.coversAllSurfaces)
        assertEquals("13F — Global Audit & Support API", report.nextRecommendedPhase)
    }
}
