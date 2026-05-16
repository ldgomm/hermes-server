package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogAttributeRulesTest {
    @Test
    fun `validates required enum and decimal attributes`() {
        CatalogAttributeRules.validate(
            definitions = listOf(
                CatalogAttributeDefinition("material", "Material", CatalogAttributeType.ENUM, required = true, allowedValues = setOf("metal", "plastic")),
                CatalogAttributeDefinition("weight", "Peso", CatalogAttributeType.DECIMAL),
            ),
            values = listOf(
                CatalogAttributeValue("material", "metal"),
                CatalogAttributeValue("weight", "1.5"),
            ),
        )
    }

    @Test
    fun `rejects missing required attribute`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogAttributeRules.validate(
                definitions = listOf(CatalogAttributeDefinition("material", "Material", CatalogAttributeType.TEXT, required = true)),
                values = emptyList(),
            )
        }
    }

    @Test
    fun `rejects enum value outside allowed values`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogAttributeRules.validate(
                definitions = listOf(CatalogAttributeDefinition("size", "Tamaño", CatalogAttributeType.ENUM, allowedValues = setOf("S", "M"))),
                values = listOf(CatalogAttributeValue("size", "XL")),
            )
        }
    }

    @Test
    fun `rejects unknown attribute`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogAttributeRules.validate(
                definitions = emptyList(),
                values = listOf(CatalogAttributeValue("unknown", "value")),
            )
        }
    }
}
