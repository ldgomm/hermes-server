package com.hermes.application.admin.access

import kotlin.test.Test
import kotlin.test.assertTrue

class AdminAccessApiContractTest {
    @Test
    fun `contains admin access phase 13B routes`() {
        val paths = AdminAccessApiContract.routes.map { "${it.method} ${it.path}" }.toSet()

        assertTrue("GET /api/v1/admin/users" in paths)
        assertTrue("POST /api/v1/admin/users/temporary" in paths)
        assertTrue("POST /api/v1/admin/users/{userId}/reset-password" in paths)

        assertTrue("POST /api/v1/admin/invitations" in paths)
        assertTrue("GET /api/v1/admin/invitations" in paths)
        assertTrue("POST /api/v1/admin/invitations/{invitationId}/resend" in paths)
        assertTrue("POST /api/v1/admin/invitations/{invitationId}/revoke" in paths)

        assertTrue("GET /api/v1/admin/roles" in paths)
        assertTrue("POST /api/v1/admin/roles" in paths)
        assertTrue("GET /api/v1/admin/roles/{roleId}" in paths)
        assertTrue("PUT /api/v1/admin/roles/{roleId}" in paths)
        assertTrue("POST /api/v1/admin/roles/{roleId}/activate" in paths)
        assertTrue("POST /api/v1/admin/roles/{roleId}/deactivate" in paths)

        assertTrue("GET /api/v1/admin/permissions" in paths)
    }
}