package com.hermes.application.admin.business

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminBusinessFoundationApiContractTest {
    @Test
    fun `fase 13A exposes expected admin business route contract`() {
        val routes = AdminBusinessFoundationApiContract.routes

        assertEquals(22, routes.size)
        assertTrue(routes.any { it.method == "GET" && it.path == "/api/v1/admin/business/overview" })
        assertTrue(routes.any { it.method == "POST" && it.path == "/api/v1/admin/activities" })
        assertTrue(routes.any { it.method == "POST" && it.path == "/api/v1/admin/branches" })
        assertTrue(routes.any { it.method == "POST" && it.path == "/api/v1/admin/emission-points" })
    }
}
