package com.hermes.infrastructure.mongo.signature

import com.hermes.application.signature.ElectronicSignatureRepository
import com.hermes.domain.signature.ElectronicSignature
import com.hermes.domain.signature.ElectronicSignatureStatus
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class MongoElectronicSignatureRepository(
    database: MongoDatabase,
) : ElectronicSignatureRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ELECTRONIC_SIGNATURES)

    override fun create(signature: ElectronicSignature) {
        collection.insertOne(signature.toDocument())
    }

    override fun update(signature: ElectronicSignature) {
        collection.replaceOne(
            eq(MongoDocumentFields.ID, signature.id),
            signature.toDocument(),
            ReplaceOptions().upsert(false)
        )
    }

    override fun findById(id: String): ElectronicSignature? =
        collection.find(eq(MongoDocumentFields.ID, id.trim())).firstOrNull()?.toDomain()

    override fun findActiveByOrganizationId(organizationId: String): ElectronicSignature? =
        collection.find(
            and(
                eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                eq("status", ElectronicSignatureStatus.VALID.toStorage()),
            )
        ).sort(Sorts.descending(MongoDocumentFields.UPDATED_AT)).firstOrNull()?.toDomain()

    override fun findByOrganizationId(organizationId: String): List<ElectronicSignature> =
        collection.find(eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()))
            .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .map(Document::toDomain)
            .toList()
}

private fun ElectronicSignature.toDocument(): Document = Document()
    .append(MongoDocumentFields.ID, id)
    .append(MongoDocumentFields.ORGANIZATION_ID, organizationId)
    .append("alias", id)
    .append("certificateSubject", subject)
    .append("issuer", issuer)
    .append("serialNumber", null)
    .append("validFrom", Date.from(validFrom))
    .append("validTo", Date.from(validTo))
    .append("status", status.toStorage())
    .append("encryptedFileObjectKey", storageKey)
    .append("encryptedPasswordRef", passwordSecretRef)
    .append("lastTestedAt", null)
    .append("lastUsedAt", lastUsedAt?.let(Date::from))
    .append("failureCount", 0)
    .append(MongoDocumentFields.CREATED_AT, Date.from(uploadedAt))
    .append(MongoDocumentFields.CREATED_BY, uploadedBy)
    .append(MongoDocumentFields.UPDATED_AT, Date.from(lastUsedAt ?: uploadedAt))
    .append(MongoDocumentFields.UPDATED_BY, uploadedBy)
    .append(MongoDocumentFields.VERSION, 1)
    .append(MongoDocumentFields.SCHEMA_VERSION, 1)

private fun Document.toDomain(): ElectronicSignature = ElectronicSignature.restore(
    id = getString(MongoDocumentFields.ID).required(MongoDocumentFields.ID),
    organizationId = getString(MongoDocumentFields.ORGANIZATION_ID).required(MongoDocumentFields.ORGANIZATION_ID),
    storageKey = getString("encryptedFileObjectKey").required("encryptedFileObjectKey"),
    passwordSecretRef = getString("encryptedPasswordRef").required("encryptedPasswordRef"),
    subject = getString("certificateSubject").required("certificateSubject"),
    issuer = getString("issuer") ?: "unknown",
    validFrom = MongoInstantMapper.readRequired(this, "validFrom"),
    validTo = MongoInstantMapper.readRequired(this, "validTo"),
    status = getString("status")?.toDomainStatus() ?: ElectronicSignatureStatus.UPLOADED,
    uploadedBy = getString(MongoDocumentFields.CREATED_BY) ?: getString(MongoDocumentFields.UPDATED_BY) ?: "system",
    uploadedAt = MongoInstantMapper.readRequired(this, MongoDocumentFields.CREATED_AT),
    lastUsedAt = MongoInstantMapper.readOptional(this, "lastUsedAt"),
)

private fun ElectronicSignatureStatus.toStorage(): String = when (this) {
    ElectronicSignatureStatus.UPLOADED -> "uploaded"
    ElectronicSignatureStatus.VALID -> "active"
    ElectronicSignatureStatus.NOT_YET_VALID -> "uploaded"
    ElectronicSignatureStatus.EXPIRED -> "expired"
    ElectronicSignatureStatus.REVOKED -> "revoked"
    ElectronicSignatureStatus.INVALID -> "failed"
    ElectronicSignatureStatus.DISABLED -> "archived"
}

private fun String.toDomainStatus(): ElectronicSignatureStatus = when (trim().lowercase()) {
    "uploaded" -> ElectronicSignatureStatus.UPLOADED
    "tested" -> ElectronicSignatureStatus.VALID
    "active" -> ElectronicSignatureStatus.VALID
    "expired" -> ElectronicSignatureStatus.EXPIRED
    "revoked" -> ElectronicSignatureStatus.REVOKED
    "failed" -> ElectronicSignatureStatus.INVALID
    "archived" -> ElectronicSignatureStatus.DISABLED
    else -> ElectronicSignatureStatus.INVALID
}

private fun String?.required(field: String): String = this?.trim()?.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("Required field '$field' is missing or blank.")
