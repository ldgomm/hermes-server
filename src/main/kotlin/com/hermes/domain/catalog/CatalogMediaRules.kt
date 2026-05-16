package com.hermes.domain.catalog

import com.hermes.domain.shared.DomainRuleViolation

object CatalogMediaRules {
    private val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp")

    fun validate(asset: CatalogMediaAsset) {
        if (!asset.url.startsWith("https://") && !asset.url.startsWith("storage://")) {
            throw DomainRuleViolation("Catalog media url must use https:// or storage://.")
        }
        if (asset.mimeType !in allowedMimeTypes) {
            throw DomainRuleViolation("Unsupported catalog media mime type ${asset.mimeType}.")
        }
        if (asset.status in setOf(CatalogMediaStatus.REJECTED, CatalogMediaStatus.HIDDEN) && asset.isPrimary) {
            throw DomainRuleViolation("Rejected or hidden media cannot be primary.")
        }
    }

    fun validateCollection(media: List<CatalogMediaAsset>) {
        media.forEach(::validate)
        if (media.count { it.isPrimary } > 1) {
            throw DomainRuleViolation("Only one catalog media asset can be primary.")
        }
    }
}
