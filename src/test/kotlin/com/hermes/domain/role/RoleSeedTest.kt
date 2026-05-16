package com.hermes.domain.role

import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class RoleSeedTest {
    @Test
    fun `contains the eight required base roles`() {
        val codes = RoleSeed.all.map { it.code }.toSet()

        assertEquals(SystemRoleCode.codes, codes)
    }

    @Test
    fun `validates system role seed`() {
        RoleSeedRules.validate(RoleSeed.all)
    }

    @Test
    fun `creates two platform roles and six organization roles`() {
        assertEquals(2, RoleSeed.platformRoles().size)
        assertEquals(6, RoleSeed.organizationRoles().size)
    }

    @Test
    fun `allows wildcard only for platform super admin`() {
        val superAdmin = RoleSeed.get(SystemRoleCode.PLATFORM_SUPER_ADMIN)
        val support = RoleSeed.get(SystemRoleCode.PLATFORM_SUPPORT)

        assertTrue(PermissionCatalog.ALL in superAdmin.permissionKeys)
        assertFalse(PermissionCatalog.ALL in support.permissionKeys)
    }

    @Test
    fun `organization owner can manage users roles organization signature documents and audit`() {
        val owner = RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER)

        assertTrue(PermissionCatalog.CREDENTIALS_USERS_CREATE in owner.permissionKeys)
        assertTrue(PermissionCatalog.CREDENTIALS_ROLES_ASSIGN in owner.permissionKeys)
        assertTrue(PermissionCatalog.ORGANIZATION_UPDATE in owner.permissionKeys)
        assertTrue(PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE in owner.permissionKeys)
        assertTrue(PermissionCatalog.SIGNATURE_USE_FOR_INVOICING in owner.permissionKeys)
        assertTrue(PermissionCatalog.AUDIT_VIEW in owner.permissionKeys)
    }

    @Test
    fun `operator cannot manage credentials or issue electronic invoices`() {
        val operator = RoleSeed.get(SystemRoleCode.OPERATOR)

        assertFalse(PermissionCatalog.CREDENTIALS_USERS_CREATE in operator.permissionKeys)
        assertFalse(PermissionCatalog.CREDENTIALS_ROLES_ASSIGN in operator.permissionKeys)
        assertFalse(PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE in operator.permissionKeys)
        assertTrue(PermissionCatalog.SALES_CREATE in operator.permissionKeys)
        assertTrue(PermissionCatalog.PAYMENTS_COLLECT in operator.permissionKeys)
    }

    @Test
    fun `accountant can read documents and reports but cannot mutate operation`() {
        val accountant = RoleSeed.get(SystemRoleCode.ACCOUNTANT)

        assertTrue(PermissionCatalog.DOCUMENTS_VIEW in accountant.permissionKeys)
        assertTrue(PermissionCatalog.DOCUMENTS_DOWNLOAD_XML in accountant.permissionKeys)
        assertTrue(PermissionCatalog.REPORTS_TAX_VIEW in accountant.permissionKeys)
        assertFalse(PermissionCatalog.SALES_CREATE in accountant.permissionKeys)
        assertFalse(PermissionCatalog.PAYMENTS_COLLECT in accountant.permissionKeys)
    }

    @Test
    fun `read only role has no mutation permissions`() {
        val readOnly = RoleSeed.get(SystemRoleCode.READ_ONLY)
        val mutationPrefixes = listOf(
            "create",
            "update",
            "manage",
            "assign",
            "upload",
            "replace",
            "revoke",
            "collect",
            "close",
            "open",
            "adjust"
        )

        val mutationPermissions = readOnly.permissionKeys.filter { permission ->
            mutationPrefixes.any { marker -> permission.endsWith(".$marker") || permission.contains(".$marker") }
        }

        assertTrue(mutationPermissions.isEmpty(), "Read only role has mutation permissions: $mutationPermissions")
    }

    @Test
    fun `rejects duplicated role code`() {
        val duplicated = RoleSeed.all + RoleSeed.get(SystemRoleCode.OPERATOR).copy(id = "role_operator_copy")

        assertFailsWith<DomainRuleViolation> {
            RoleSeedRules.validate(duplicated)
        }
    }

    @Test
    fun `rejects organization role with platform permission`() {
        val badRole = RoleSeed.get(SystemRoleCode.OPERATOR).copy(
            permissionKeys = RoleSeed.get(SystemRoleCode.OPERATOR).permissionKeys + "platform.catalog.publish_template",
        )

        assertFailsWith<DomainRuleViolation> {
            RoleSeedRules.validate(listOf(badRole) + RoleSeed.all.filterNot { it.code == badRole.code })
        }
    }

    @Test
    fun `rejects electronic invoice role without signature use permission`() {
        val manager = RoleSeed.get(SystemRoleCode.MANAGER)
        val badRole = manager.copy(
            permissionKeys = manager.permissionKeys - PermissionCatalog.SIGNATURE_USE_FOR_INVOICING,
        )

        assertFailsWith<DomainRuleViolation> {
            RoleSeedRules.validate(listOf(badRole) + RoleSeed.all.filterNot { it.code == badRole.code })
        }
    }
}
