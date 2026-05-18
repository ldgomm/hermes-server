package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceStatus
import com.hermes.domain.electronicinvoicing.SriDocumentType
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriSeries
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import org.bson.Document
import java.time.Instant
import java.util.Date

object MongoElectronicSequenceMappers {
    fun toDocument(sequence: ElectronicSequence): Document = Document()
        .append(MongoDocumentFields.ID, sequence.id)
        .append(MongoDocumentFields.ORGANIZATION_ID, sequence.organizationId)
        .append("environment", sequence.environment.storageValue)
        .append("documentType", sequence.documentType.storageValue)
        .append("establishmentCode", sequence.series.establishmentCode)
        .append("emissionPointCode", sequence.series.emissionPointCode)
        .append("series", sequence.series.value)
        .append("currentValue", sequence.currentValue)
        .append("status", sequence.status.storageValue)
        .append("lastIssuedDocumentId", sequence.lastIssuedDocumentId)
        .append("lastIssuedAt", sequence.lastIssuedAt?.let(Date::from))
        .append(MongoDocumentFields.CREATED_AT, Date.from(sequence.createdAt))
        .append(MongoDocumentFields.UPDATED_AT, Date.from(sequence.updatedAt))
        .append(MongoDocumentFields.VERSION, sequence.version)
        .append(MongoDocumentFields.SCHEMA_VERSION, sequence.schemaVersion)

    fun fromDocument(document: Document): ElectronicSequence = ElectronicSequence(
        id = document.getString(MongoDocumentFields.ID).required(MongoDocumentFields.ID),
        organizationId = document.getString(MongoDocumentFields.ORGANIZATION_ID).required(MongoDocumentFields.ORGANIZATION_ID),
        environment = SriEnvironment.fromStorage(document.getString("environment").required("environment")),
        documentType = SriDocumentType.fromStorage(document.getString("documentType").required("documentType")),
        series = SriSeries(
            establishmentCode = document.getString("establishmentCode").required("establishmentCode"),
            emissionPointCode = document.getString("emissionPointCode").required("emissionPointCode"),
        ),
        currentValue = document.readInt("currentValue"),
        status = ElectronicSequenceStatus.fromStorage(document.getString("status").required("status")),
        lastIssuedDocumentId = document.getString("lastIssuedDocumentId"),
        lastIssuedAt = MongoInstantMapper.readOptional(document, "lastIssuedAt"),
        createdAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.CREATED_AT),
        updatedAt = MongoInstantMapper.readRequired(document, MongoDocumentFields.UPDATED_AT),
        version = document.readInt(MongoDocumentFields.VERSION, default = 1),
        schemaVersion = document.readInt(MongoDocumentFields.SCHEMA_VERSION, default = ElectronicSequence.SCHEMA_VERSION),
    )

    private fun String?.required(fieldName: String): String = this?.trim()?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Required field '$fieldName' is missing or blank.")

    private fun Document.readInt(fieldName: String, default: Int? = null): Int {
        val raw = this[fieldName] ?: return default
            ?: throw IllegalArgumentException("Required int field '$fieldName' is missing.")
        return when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull()
                ?: throw IllegalArgumentException("Int field '$fieldName' contains an invalid value: $raw")
            else -> throw IllegalArgumentException("Int field '$fieldName' has unsupported type: ${raw::class.qualifiedName}")
        }
    }
}
