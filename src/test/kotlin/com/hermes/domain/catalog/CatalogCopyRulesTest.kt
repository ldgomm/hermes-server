package com.hermes.domain.catalog

import com.hermes.domain.money.Money
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogCopyRulesTest {
    @Test
    fun `copies active platform template into private local catalog item`() {
        val template = template()

        val item = CatalogCopyRules.copyFromTemplate(
            id = "item_1",
            organizationId = "org_1",
            branchId = "br_1",
            activityId = "act_1",
            template = template,
            localPrice = Money.of("2.50"),
            taxProfileId = "taxp_iva_full_current",
        )

        assertEquals("tpl_1", item.templateId)
        assertEquals("global_coca_500", item.globalCatalogId)
        assertEquals("Coca Cola 500ml", item.localName)
        assertEquals(PublicDiscoveryStatus.PRIVATE, item.publicDiscoveryStatus)
        assertEquals(CatalogItemStatus.ACTIVE, item.status)
        assertEquals("family_soft_drinks", item.productFamilyId)
        assertEquals("500ml", item.variantAttributes["presentation"])
    }

    @Test
    fun `rejects copying inactive template`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogCopyRules.copyFromTemplate(
                id = "item_1",
                organizationId = "org_1",
                activityId = "act_1",
                template = template().copy(status = CatalogTemplateStatus.PAUSED),
                localPrice = Money.of("2.50"),
                taxProfileId = "taxp_iva_full_current",
            )
        }
    }

    @Test
    fun `rejects copy without tax profile`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogCopyRules.copyFromTemplate(
                id = "item_1",
                organizationId = "org_1",
                activityId = "act_1",
                template = template(),
                localPrice = Money.of("2.50"),
                taxProfileId = "",
            )
        }
    }

    private fun template(): PlatformCatalogTemplate = PlatformCatalogTemplate(
        id = "tpl_1",
        globalCatalogId = "global_coca_500",
        canonicalName = "Coca Cola 500ml",
        normalizedName = "coca cola 500ml",
        type = CatalogItemType.PRODUCT,
        status = CatalogTemplateStatus.ACTIVE,
        productFamilyId = "family_soft_drinks",
        variantAttributes = mapOf("presentation" to "500ml"),
        identifiers = listOf(
            CatalogIdentifier.create(
                type = CatalogIdentifierType.EAN_13,
                value = "7861001234567",
                scope = CatalogIdentifierScope.GLOBAL,
                source = CatalogIdentifierSource.PLATFORM,
                status = CatalogIdentifierStatus.VERIFIED,
            ),
        ),
    )
}
