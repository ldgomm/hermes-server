package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicInvoiceIssueRecord
import com.hermes.domain.electronicinvoicing.*
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document
import java.util.*

object MongoElectronicInvoiceIssueMappers {
    private const val SCHEMA_VERSION = 1

    fun toDocument(record: ElectronicInvoiceIssueRecord): Document =
        Document(MongoDocumentFields.ID, record.id).append(MongoDocumentFields.ORGANIZATION_ID, record.organizationId)
            .append("branchId", record.branchId).append("emissionPointId", record.emissionPointId)
            .append("saleId", record.saleId).append("environment", record.environment.storageValue)
            .append("documentType", record.documentType.storageValue)
            .append("establishmentCode", record.series.establishmentCode)
            .append("emissionPointCode", record.series.emissionPointCode).append("series", record.series.value)
            .append("documentNumber", record.documentNumber).append("accessKey", record.accessKey.value)
            .append("authorizationNumber", record.authorizationNumber).append("status", record.status.name)
            .append("schemaVersionCode", record.schemaVersionCode)
            .append("unsignedXmlObjectKey", record.unsignedXmlObjectKey)
            .append("unsignedXmlSha256", record.unsignedXmlSha256)
            .append("signedXmlObjectKey", record.signedXmlObjectKey).append("signedXmlSha256", record.signedXmlSha256)
            .append("authorizedXmlObjectKey", record.authorizedXmlObjectKey)
            .append("authorizedXmlSha256", record.authorizedXmlSha256)
            .append("ridePdfObjectKey", record.ridePdfObjectKey).append("ridePdfSha256", record.ridePdfSha256)
            .append("signatureId", record.signatureId).append("lastSriReceptionStatus", record.lastSriReceptionStatus)
            .append("lastSriAuthorizationStatus", record.lastSriAuthorizationStatus)
            .append("sriMessages", record.sriMessages.map(::messageToDocument))
            .append("lastErrorClassification", record.lastErrorClassification?.let(::classificationToDocument))
            .append("issuedAt", Date.from(record.issuedAt)).append("authorizedAt", record.authorizedAt?.let(Date::from))
            .append("rideGeneratedAt", record.rideGeneratedAt?.let(Date::from))
            .append("deliveryEmailTo", record.deliveryEmailTo)
            .append("deliveredAt", record.deliveredAt?.let(Date::from))
            .append("deliveryErrorMessage", record.deliveryErrorMessage)
            .append(MongoDocumentFields.CREATED_AT, Date.from(record.createdAt))
            .append(MongoDocumentFields.UPDATED_AT, Date.from(record.updatedAt))
            .append(MongoDocumentFields.CREATED_BY, record.createdBy)
            .append(MongoDocumentFields.UPDATED_BY, record.updatedBy)
            .append(MongoDocumentFields.VERSION, record.version)
            .append(MongoDocumentFields.SCHEMA_VERSION, SCHEMA_VERSION)

    fun fromDocument(document: Document): ElectronicInvoiceIssueRecord = ElectronicInvoiceIssueRecord(
        id = document.requiredString(MongoDocumentFields.ID),
        organizationId = document.requiredString(MongoDocumentFields.ORGANIZATION_ID),
        branchId = document.requiredString("branchId"),
        emissionPointId = document.requiredString("emissionPointId"),
        saleId = document.requiredString("saleId"),
        environment = SriEnvironment.fromStorage(document.requiredString("environment")),
        documentType = SriDocumentType.fromStorage(document.requiredString("documentType")),
        series = SriSeries(
            establishmentCode = document.requiredString("establishmentCode"),
            emissionPointCode = document.requiredString("emissionPointCode"),
        ),
        documentNumber = document.requiredString("documentNumber"),
        accessKey = SriAccessKey(document.requiredString("accessKey")),
        authorizationNumber = document.requiredString("authorizationNumber"),
        status = statusFromStorage(document.requiredString("status")),
        schemaVersionCode = document.optionalString("schemaVersionCode"),
        unsignedXmlObjectKey = document.optionalString("unsignedXmlObjectKey"),
        unsignedXmlSha256 = document.optionalString("unsignedXmlSha256"),
        signedXmlObjectKey = document.optionalString("signedXmlObjectKey"),
        signedXmlSha256 = document.optionalString("signedXmlSha256"),
        authorizedXmlObjectKey = document.optionalString("authorizedXmlObjectKey"),
        authorizedXmlSha256 = document.optionalString("authorizedXmlSha256"),
        ridePdfObjectKey = document.optionalString("ridePdfObjectKey"),
        ridePdfSha256 = document.optionalString("ridePdfSha256"),
        signatureId = document.optionalString("signatureId"),
        lastSriReceptionStatus = document.optionalString("lastSriReceptionStatus"),
        lastSriAuthorizationStatus = document.optionalString("lastSriAuthorizationStatus"),
        sriMessages = document.messages(),
        lastErrorClassification = document.optionalDocument("lastErrorClassification")
            ?.let(::classificationFromDocument),
        issuedAt = MongoInstantMapper.readRequired(document, "issuedAt"),
        authorizedAt = MongoInstantMapper.readOptional(document, "authorizedAt"),
        rideGeneratedAt = MongoInstantMapper.readOptional(document, "rideGeneratedAt"),
        deliveryEmailTo = document.optionalString("deliveryEmailTo"),
        deliveredAt = MongoInstantMapper.readOptional(document, "deliveredAt"),
        deliveryErrorMessage = document.optionalString("deliveryErrorMessage"),
        createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
        updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
        createdBy = document.requiredString(MongoDocumentFields.CREATED_BY),
        updatedBy = document.requiredString(MongoDocumentFields.UPDATED_BY),
        version = document.requiredLong(MongoDocumentFields.VERSION),
    )

    private fun messageToDocument(message: SriMessage): Document =
        Document("identifier", message.identifier).append("message", message.message)
            .append("additionalInfo", message.additionalInfo).append("type", message.type.name)

    private fun Document.messages(): List<SriMessage> =
        (this["sriMessages"] as? List<*>).orEmpty().mapNotNull { it as? Document }.map { doc ->
            SriMessage(
                identifier = doc.optionalString("identifier"),
                message = doc.requiredString("message"),
                additionalInfo = doc.optionalString("additionalInfo"),
                type = SriMessageType.valueOf(doc.optionalString("type") ?: SriMessageType.ERROR.name),
            )
        }

    private fun classificationToDocument(classification: SriErrorClassification): Document =
        Document("category", classification.category.name).append("recoverability", classification.recoverability.name)
            .append("userActionRequired", classification.userActionRequired)
            .append("shouldKeepSameAccessKey", classification.shouldKeepSameAccessKey)
            .append("reason", classification.reason)

    private fun classificationFromDocument(document: Document): SriErrorClassification = SriErrorClassification(
        category = SriErrorCategory.valueOf(document.requiredString("category")),
        recoverability = SriErrorRecoverability.valueOf(document.requiredString("recoverability")),
        userActionRequired = document.getBoolean("userActionRequired", false),
        shouldKeepSameAccessKey = document.getBoolean("shouldKeepSameAccessKey", true),
        reason = document.requiredString("reason"),
    )

    private fun statusFromStorage(value: String): ElectronicDocumentStatus =
        ElectronicDocumentStatus.valueOf(value.trim().uppercase())

    private fun Document.requiredString(fieldName: String): String =
        getString(fieldName)?.trim()?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Required field '$fieldName' is missing or blank.")

    private fun Document.optionalString(fieldName: String): String? =
        getString(fieldName)?.trim()?.takeIf { it.isNotBlank() }

    private fun Document.optionalDocument(fieldName: String): Document? = this[fieldName] as? Document

    private fun Document.requiredLong(fieldName: String): Long {
        val raw = this[fieldName] ?: throw IllegalArgumentException("Required long field '$fieldName' is missing.")
        return when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull()
                ?: throw IllegalArgumentException("Long field '$fieldName' contains an invalid value: $raw")

            else -> throw IllegalArgumentException("Long field '$fieldName' has unsupported type: ${raw::class.qualifiedName}")
        }
    }
}
