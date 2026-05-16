package com.hermes.domain.permission

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PermissionRulesTest {
    @Test
    fun `merges permissions from multiple roles`() {
        val permissions = PermissionRules.effectivePermissions(
            listOf(
                role("seller", setOf(PermissionCatalog.SALES_CREATE)),
                role("cashier", setOf(PermissionCatalog.PAYMENTS_COLLECT)),
            ),
        )

        assertTrue(PermissionRules.canPerform(permissions, PermissionCatalog.SALES_CREATE))
        assertTrue(PermissionRules.canPerform(permissions, PermissionCatalog.PAYMENTS_COLLECT))
        assertFalse(PermissionRules.canPerform(permissions, PermissionCatalog.AUDIT_VIEW))
    }

    @Test
    fun `allows wildcard only for system role`() {
        val permissions = PermissionRules.effectivePermissions(
            listOf(role("owner", setOf(PermissionCatalog.ALL), systemRole = true)),
        )

        assertTrue(PermissionRules.canPerform(permissions, PermissionCatalog.AUDIT_VIEW))
    }

    @Test
    fun `rejects unknown permission`() {
        assertFailsWith<DomainRuleViolation> {
            PermissionRules.validateRole(role("bad", setOf("unknown.permission")))
        }
    }

    @Test
    fun `rejects wildcard in custom role`() {
        assertFailsWith<DomainRuleViolation> {
            PermissionRules.validateRole(role("bad", setOf(PermissionCatalog.ALL), systemRole = false))
        }
    }

    @Test
    fun `rejects signature use without metadata permission`() {
        assertFailsWith<DomainRuleViolation> {
            PermissionRules.validateRole(role("bad", setOf(PermissionCatalog.SIGNATURE_USE_FOR_INVOICING)))
        }
    }

    private fun role(name: String, permissions: Set<String>, systemRole: Boolean = false): Role = Role(
        id = "role_$name",
        organizationId = if (systemRole) null else "org_1",
        name = name,
        permissions = permissions,
        systemRole = systemRole,
    )
}
