package com.hermes.application.auth

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizationPolicyTest {
    @Test
    fun `allows wildcard permission`() {
        assertTrue(
            AuthorizationPolicy.canPerform(
                effectivePermissions = setOf(PermissionCatalog.ALL),
                requiredPermission = PermissionCatalog.SALES_CREATE,
            ),
        )
    }

    @Test
    fun `denies missing permission`() {
        assertFalse(
            AuthorizationPolicy.canPerform(
                effectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                requiredPermission = PermissionCatalog.SALES_CREATE,
            ),
        )

        assertFailsWith<DomainRuleViolation> {
            AuthorizationPolicy.requirePermission(
                effectivePermissions = setOf(PermissionCatalog.SALES_VIEW),
                requiredPermission = PermissionCatalog.SALES_CREATE,
            )
        }
    }
}
