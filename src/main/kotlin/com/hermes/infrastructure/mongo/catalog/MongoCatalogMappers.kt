package com.hermes.infrastructure.mongo.catalog

import com.hermes.domain.catalog.CatalogIdentifier
import com.hermes.domain.catalog.CatalogIdentifierScope
import com.hermes.domain.catalog.CatalogIdentifierSource
import com.hermes.domain.catalog.CatalogIdentifierStatus
import com.hermes.domain.catalog.CatalogIdentifierType
import com.hermes.domain.catalog.CatalogItemRequest
import com.hermes.domain.catalog.CatalogItemRequestStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.CatalogMediaAsset
import com.hermes.domain.catalog.CatalogMediaOwnerKind
import com.hermes.domain.catalog.CatalogMediaStatus
import com.hermes.domain.catalog.CatalogPriceHistory
import com.hermes.domain.catalog.CatalogTemplateStatus
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.catalog.PublicDiscoveryStatus
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.money.Money
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoDecimalMapper
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document

object MongoCatalogMappers {
    fun templateToDocument(template: PlatformCatalogTemplate): Document =
        Document(MongoDocumentFields.ID, template.id)
            .append("globalCatalogId", template.globalCatalogId)
            .append("canonicalName", template.canonicalName)
            .append("normalizedName", template.normalizedName)
            .append("type", template.type.name)
            .append("status", template.status.name)
            .append("productFamilyId", template.productFamilyId)
            .append("variantAttributes", Document(template.variantAttributes))
            .append("identifiers", template.identifiers.map(::identifierToDocument))
            .append("attributes", Document(template.attributes))
            .append("media", template.media.map(::mediaToDocument))

    fun templateFromDocument(document: Document): PlatformCatalogTemplate =
        PlatformCatalogTemplate(
            id = document.requiredString(MongoDocumentFields.ID),
            globalCatalogId = document.requiredString("globalCatalogId"),
            canonicalName = document.requiredString("canonicalName"),
            normalizedName = document.requiredString("normalizedName"),
            type = enumValueOf(document.requiredString("type")),
            status = enumValueOf(document.requiredString("status")),
            productFamilyId = document.optionalString("productFamilyId"),
            variantAttributes = document.documentToStringMap("variantAttributes"),
            identifiers = document.documentList("identifiers").map(::identifierFromDocument),
            attributes = document.documentToStringMap("attributes"),
            media = document.documentList("media").map(::mediaFromDocument),
        )

    fun itemToDocument(item: OrganizationCatalogItem): Document =
        Document(MongoDocumentFields.ID, item.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, item.organizationId)
            .append("branchId", item.branchId)
            .append("activityId", item.activityId)
            .append("templateId", item.templateId)
            .append("globalCatalogId", item.globalCatalogId)
            .append("localName", item.localName)
            .append("searchableText", item.searchableText)
            .append("type", item.type.name)
            .append("status", item.status.name)
            .append("localPrice", moneyToDocument(item.localPrice))
            .append("taxProfileId", item.taxProfileId)
            .append("publicDiscoveryStatus", item.publicDiscoveryStatus.name)
            .append("productFamilyId", item.productFamilyId)
            .append("variantAttributes", Document(item.variantAttributes))
            .append("identifiers", item.identifiers.map(::identifierToDocument))
            .append("attributes", Document(item.attributes))
            .append("media", item.media.map(::mediaToDocument))

    fun itemFromDocument(document: Document): OrganizationCatalogItem =
        OrganizationCatalogItem(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            branchId = document.optionalString("branchId"),
            activityId = document.requiredString("activityId"),
            templateId = document.requiredString("templateId"),
            globalCatalogId = document.requiredString("globalCatalogId"),
            localName = document.requiredString("localName"),
            searchableText = document.requiredString("searchableText"),
            type = enumValueOf(document.requiredString("type")),
            status = enumValueOf(document.requiredString("status")),
            localPrice = moneyFromDocument(document.get("localPrice", Document::class.java)),
            taxProfileId = document.requiredString("taxProfileId"),
            publicDiscoveryStatus = document.optionalEnum("publicDiscoveryStatus", PublicDiscoveryStatus.PRIVATE),
            productFamilyId = document.optionalString("productFamilyId"),
            variantAttributes = document.documentToStringMap("variantAttributes"),
            identifiers = document.documentList("identifiers").map(::identifierFromDocument),
            attributes = document.documentToStringMap("attributes"),
            media = document.documentList("media").map(::mediaFromDocument),
        )

    fun requestToDocument(request: CatalogItemRequest): Document =
        Document(MongoDocumentFields.ID, request.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, request.organizationId)
            .append("requestedByUserId", request.requestedByUserId)
            .append("requestedName", request.requestedName)
            .append("normalizedRequestedName", request.requestedName.trim().lowercase())
            .append("requestedType", request.requestedType.name)
            .append("description", request.description)
            .append("suggestedCategoryId", request.suggestedCategoryId)
            .append("suggestedTaxProfileCode", request.suggestedTaxProfileCode)
            .append("identifiers", request.identifiers.map(::identifierToDocument))
            .append("status", request.status.name)
            .append("reviewedByUserId", request.reviewedByUserId)
            .append("reviewedAt", request.reviewedAt?.let(MongoInstantMapper::toDate))
            .append("reviewReason", request.reviewReason)
            .append(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(request.createdAt))
            .append(MongoDocumentFields.UPDATED_AT, MongoInstantMapper.toDate(request.updatedAt))
            .append(MongoDocumentFields.VERSION, request.version)

    fun requestFromDocument(document: Document): CatalogItemRequest =
        CatalogItemRequest(
            id = document.requiredString(MongoDocumentFields.ID),
            organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
            requestedByUserId = document.requiredString("requestedByUserId"),
            requestedName = document.requiredString("requestedName"),
            requestedType = enumValueOf(document.requiredString("requestedType")),
            description = document.optionalString("description"),
            suggestedCategoryId = document.optionalString("suggestedCategoryId"),
            suggestedTaxProfileCode = document.optionalString("suggestedTaxProfileCode"),
            identifiers = document.documentList("identifiers").map(::identifierFromDocument),
            status = enumValueOf(document.requiredString("status")),
            reviewedByUserId = document.optionalString("reviewedByUserId"),
            reviewedAt = MongoInstantMapper.readOptional(document, "reviewedAt"),
            reviewReason = document.optionalString("reviewReason"),
            linkedTemplateId = document.optionalString("linkedTemplateId"),
            adminMessage = document.optionalString("adminMessage"),
            createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
            updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
            version = document.optionalLong(MongoDocumentFields.VERSION, 1L),
        )

    fun priceHistoryToDocument(history: CatalogPriceHistory): Document =
        Document(MongoDocumentFields.ID, history.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, history.organizationId)
            .append("catalogItemId", history.catalogItemId)
            .append("oldPrice", moneyToDocument(history.oldPrice))
            .append("newPrice", moneyToDocument(history.newPrice))
            .append("changedByUserId", history.changedByUserId)
            .append("reason", history.reason)
            .append("changedAt", MongoInstantMapper.toDate(history.changedAt))

    private fun identifierToDocument(identifier: CatalogIdentifier): Document =
        Document("type", identifier.type.name)
            .append("value", identifier.value)
            .append("normalizedValue", identifier.normalizedValue)
            .append("scope", identifier.scope.name)
            .append("status", identifier.status.name)
            .append("source", identifier.source.name)
            .append("isPrimary", identifier.isPrimary)

    private fun identifierFromDocument(document: Document): CatalogIdentifier =
        CatalogIdentifier(
            type = enumValueOf(document.requiredString("type")),
            value = document.requiredString("value"),
            normalizedValue = document.requiredString("normalizedValue"),
            scope = enumValueOf(document.requiredString("scope")),
            status = enumValueOf(document.requiredString("status")),
            source = enumValueOf(document.requiredString("source")),
            isPrimary = document.getBoolean("isPrimary", false),
        )

    private fun mediaToDocument(media: CatalogMediaAsset): Document =
        Document("id", media.id)
            .append("ownerKind", media.ownerKind.name)
            .append("url", media.url)
            .append("mimeType", media.mimeType)
            .append("status", media.status.name)
            .append("isPrimary", media.isPrimary)
            .append("sortOrder", media.sortOrder)

    private fun mediaFromDocument(document: Document): CatalogMediaAsset =
        CatalogMediaAsset(
            id = document.requiredString("id"),
            ownerKind = enumValueOf(document.requiredString("ownerKind")),
            url = document.requiredString("url"),
            mimeType = document.requiredString("mimeType"),
            status = enumValueOf(document.requiredString("status")),
            isPrimary = document.getBoolean("isPrimary", false),
            sortOrder = document.getInteger("sortOrder", 0),
        )

    private fun moneyToDocument(money: Money): Document =
        Document("amount", MongoDecimalMapper.moneyToDecimal128(money.amount))
            .append("currency", money.currency.value)

    private fun moneyFromDocument(document: Document): Money =
        Money.of(
            amount = MongoDecimalMapper.readRequired(document, "amount"),
            currency = com.hermes.domain.money.CurrencyCode(document.requiredString("currency")),
        )
}

private fun Document.requiredString(field: String): String =
    getString(field)?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required string field '$field' is missing or blank.")

private fun Document.optionalString(field: String): String? =
    getString(field)?.takeIf { it.isNotBlank() }

private fun Document.optionalLong(field: String, default: Long): Long =
    when (val raw = this[field]) {
        null -> default
        is Int -> raw.toLong()
        is Long -> raw
        else -> default
    }

private inline fun <reified T : Enum<T>> Document.optionalEnum(field: String, default: T): T =
    optionalString(field)?.let { enumValueOf<T>(it) } ?: default

@Suppress("UNCHECKED_CAST")
private fun Document.documentList(field: String): List<Document> =
    (this[field] as? List<*>)?.filterIsInstance<Document>().orEmpty()

private fun Document.documentToStringMap(field: String): Map<String, String> =
    (get(field, Document::class.java) ?: Document()).mapValues { it.value.toString() }
