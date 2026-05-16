package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogIdentifierRulesTest {
    @Test
    fun `normalizes EAN 13 identifier`() {
        val identifier = CatalogIdentifier.create(
            type = CatalogIdentifierType.EAN_13,
            value = "786-100 1234567",
            scope = CatalogIdentifierScope.GLOBAL,
            source = CatalogIdentifierSource.PLATFORM,
            status = CatalogIdentifierStatus.VERIFIED,
        )

        CatalogIdentifierRules.validate(identifier)
        assertEquals("7861001234567", identifier.normalizedValue)
    }

    @Test
    fun `rejects invalid EAN 13`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogIdentifierRules.validate(
                CatalogIdentifier.create(
                    type = CatalogIdentifierType.EAN_13,
                    value = "123",
                    scope = CatalogIdentifierScope.GLOBAL,
                    source = CatalogIdentifierSource.PLATFORM,
                ),
            )
        }
    }

    @Test
    fun `rejects conflicting verified global identifiers`() {
        val id = CatalogIdentifier.create(
            type = CatalogIdentifierType.EAN_13,
            value = "7861001234567",
            scope = CatalogIdentifierScope.GLOBAL,
            source = CatalogIdentifierSource.PLATFORM,
            status = CatalogIdentifierStatus.VERIFIED,
        )

        assertFailsWith<DomainRuleViolation> {
            CatalogIdentifierRules.validateNoConflictingGlobalVerifiedIdentifiers(
                listOf(
                    CatalogItemIdentity("tpl_1", identifiers = listOf(id)),
                    CatalogItemIdentity("tpl_2", identifiers = listOf(id)),
                ),
            )
        }
    }

    @Test
    fun `rejects duplicated local sku inside same organization`() {
        val sku = CatalogIdentifier.create(
            type = CatalogIdentifierType.SKU_LOCAL,
            value = " SKU-001 ",
            scope = CatalogIdentifierScope.ORGANIZATION,
            source = CatalogIdentifierSource.ORGANIZATION,
        )

        assertFailsWith<DomainRuleViolation> {
            CatalogIdentifierRules.validateLocalSkuUniqueness(
                listOf(
                    CatalogItemIdentity("item_1", "org_1", listOf(sku)),
                    CatalogItemIdentity("item_2", "org_1", listOf(sku)),
                ),
            )
        }
    }
}
