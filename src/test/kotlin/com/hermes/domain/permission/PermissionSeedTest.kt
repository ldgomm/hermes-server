package com.hermes.domain.permission

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PermissionSeedTest {

    @Test
    fun `permission seed has no duplicated codes`() {
        PermissionSeedRules.validate(PermissionSeed.all)

        val codes = PermissionSeed.all.map { it.code }

        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `permission seed contains phase five minimum permissions`() {
        PermissionSeedRules.assertRequiredPermissionsExist(
            definitions = PermissionSeed.all,
            requiredCodes = setOf(
                PermissionCatalog.CREDENTIALS_USERS_CREATE,
                PermissionCatalog.CREDENTIALS_ROLES_ASSIGN,
                PermissionCatalog.ORGANIZATION_VIEW,
                PermissionCatalog.ORGANIZATION_MEMBERS_MANAGE,
                PermissionCatalog.SETTINGS_EMISSION_POINTS_MANAGE,
                PermissionCatalog.SALES_CREATE,
                PermissionCatalog.PAYMENTS_COLLECT,
                PermissionCatalog.CASH_SESSION_OPEN,
                PermissionCatalog.CASH_SESSION_CLOSE,
                PermissionCatalog.DOCUMENTS_ISSUE_ELECTRONIC_INVOICE,
                PermissionCatalog.SIGNATURE_VIEW_METADATA,
                PermissionCatalog.SIGNATURE_USE_FOR_INVOICING,
                PermissionCatalog.AUDIT_VIEW,
            ),
        )
    }

    @Test
    fun `critical permissions require audit`() {
        val criticalPermissions = PermissionSeed.all.filter {
            it.riskLevel == PermissionRiskLevel.CRITICAL
        }

        assertTrue(criticalPermissions.isNotEmpty())
        assertTrue(criticalPermissions.all { it.requiresAudit })
    }

    @Test
    fun `reserved permissions require feature flag`() {
        val reservedPermissions = PermissionSeed.reserved

        assertTrue(reservedPermissions.isNotEmpty())
        assertTrue(reservedPermissions.all { !it.featureFlag.isNullOrBlank() })
    }

    @Test
    fun `rejects duplicated permission codes`() {
        val first = PermissionSeed.all.first()

        assertFailsWith<DomainRuleViolation> {
            PermissionSeedRules.validate(listOf(first, first.copy(name = "Duplicate")))
        }
    }
}
