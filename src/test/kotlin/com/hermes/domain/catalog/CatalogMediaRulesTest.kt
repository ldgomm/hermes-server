package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogMediaRulesTest {
    @Test
    fun `allows approved https image`() {
        CatalogMediaRules.validate(
            CatalogMediaAsset(
                id = "media_1",
                ownerKind = CatalogMediaOwnerKind.MASTER,
                url = "https://cdn.example.com/image.webp",
                mimeType = "image/webp",
                status = CatalogMediaStatus.APPROVED,
                isPrimary = true,
            ),
        )
    }

    @Test
    fun `rejects unsupported mime type`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogMediaRules.validate(
                CatalogMediaAsset(
                    id = "media_1",
                    ownerKind = CatalogMediaOwnerKind.MASTER,
                    url = "https://cdn.example.com/file.pdf",
                    mimeType = "application/pdf",
                    status = CatalogMediaStatus.APPROVED,
                ),
            )
        }
    }

    @Test
    fun `rejects more than one primary image`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogMediaRules.validateCollection(
                listOf(
                    asset("media_1", primary = true),
                    asset("media_2", primary = true),
                ),
            )
        }
    }

    @Test
    fun `rejects rejected image as primary`() {
        assertFailsWith<DomainRuleViolation> {
            CatalogMediaRules.validate(asset("media_1", primary = true, status = CatalogMediaStatus.REJECTED))
        }
    }

    private fun asset(id: String, primary: Boolean, status: CatalogMediaStatus = CatalogMediaStatus.APPROVED): CatalogMediaAsset =
        CatalogMediaAsset(
            id = id,
            ownerKind = CatalogMediaOwnerKind.LOCAL,
            url = "storage://bucket/$id.webp",
            mimeType = "image/webp",
            status = status,
            isPrimary = primary,
        )
}
