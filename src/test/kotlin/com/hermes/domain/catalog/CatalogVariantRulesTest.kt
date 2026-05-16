package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogVariantRulesTest {
    @Test
    fun `allows concrete variant with family and attributes`() {
        CatalogVariantRules.assertTemplateIsSellableVariant(template("tpl_1", mapOf("size" to "500ml")))
    }

    @Test
    fun `rejects variant without attributes`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogVariantRules.assertTemplateIsSellableVariant(template("tpl_1", emptyMap()))
        }
    }

    @Test
    fun `rejects duplicated variants inside family`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogVariantRules.validateUniqueVariants(
                listOf(
                    template("tpl_1", mapOf("size" to "500ml")),
                    template("tpl_2", mapOf("size" to "500ml")),
                ),
            )
        }
    }

    @Test
    fun `rejects selling family directly`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogVariantRules.assertFamilyIsNotSoldDirectly(
                PlatformCatalogFamily("family_1", "Bebidas gaseosas"),
                templateIdToSell = "family_1",
            )
        }
    }

    private fun template(id: String, attrs: Map<String, String>): PlatformCatalogTemplate = PlatformCatalogTemplate(
        id = id,
        globalCatalogId = "global_$id",
        canonicalName = "Coca Cola",
        normalizedName = "coca cola",
        type = CatalogItemType.PRODUCT,
        status = CatalogTemplateStatus.ACTIVE,
        productFamilyId = "family_1",
        variantAttributes = attrs,
    )
}
