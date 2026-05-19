package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactFile
import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactReader
import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactStorage
import com.hermes.application.electronicinvoicing.ElectronicDocumentArtifactType
import com.hermes.application.electronicinvoicing.StoreElectronicDocumentArtifactCommand
import com.hermes.application.electronicinvoicing.StoredElectronicDocumentArtifact
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.UpdateOptions
import org.bson.Document
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Date

class MongoElectronicDocumentArtifactStorage(
    database: MongoDatabase,
    root: Path,
) : ElectronicDocumentArtifactStorage, ElectronicDocumentArtifactReader {
    private val rootPath: Path = root.toAbsolutePath().normalize()
    private val collection: MongoCollection<Document> =
        database.getCollection(ElectronicInvoicingMongoCollectionNames.ELECTRONIC_DOCUMENT_ARTIFACTS)

    init {
        Files.createDirectories(rootPath)
    }

    override fun put(command: StoreElectronicDocumentArtifactCommand): StoredElectronicDocumentArtifact {
        val sha256 = command.content.sha256Hex()
        val objectKey = buildObjectKey(command, sha256)
        val target = rootPath.resolve(objectKey).normalize()

        require(target.startsWith(rootPath)) { "Invalid electronic document artifact object key." }

        Files.createDirectories(target.parent)
        Files.write(
            target,
            command.content,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )

        val stored = StoredElectronicDocumentArtifact(
            objectKey = objectKey,
            artifactType = command.artifactType,
            sha256 = sha256,
            sizeBytes = command.content.size.toLong(),
            createdAt = command.createdAt,
        )

        collection.updateOne(
            Filters.eq(MongoDocumentFields.ID, objectKey),
            Document(
                "\$setOnInsert",
                Document(MongoDocumentFields.ID, objectKey)
                    .append(MongoDocumentFields.ORGANIZATION_ID, command.organizationId)
                    .append("documentId", command.documentId)
                    .append("artifactType", command.artifactType.storageValue)
                    .append("objectKey", objectKey)
                    .append("sha256", sha256)
                    .append("sizeBytes", command.content.size.toLong())
                    .append("contentType", command.contentType)
                    .append("fileName", command.fileName)
                    .append(MongoDocumentFields.CREATED_AT, Date.from(command.createdAt))
                    .append(MongoDocumentFields.UPDATED_AT, Date.from(command.createdAt))
                    .append(MongoDocumentFields.VERSION, 1L)
                    .append(MongoDocumentFields.SCHEMA_VERSION, 1),
            ),
            UpdateOptions().upsert(true),
        )

        return stored
    }

    override fun get(objectKey: String): ElectronicDocumentArtifactFile? {
        val key = objectKey.trim().takeIf { it.isNotBlank() } ?: return null
        val metadata = collection.find(Filters.eq(MongoDocumentFields.ID, key)).firstOrNull()
            ?: return null
        return metadata.toArtifactFileOrNull()
    }

    override fun findLatest(
        organizationId: String,
        documentId: String,
        artifactType: ElectronicDocumentArtifactType,
    ): ElectronicDocumentArtifactFile? = collection
        .find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("documentId", documentId.trim()),
                Filters.eq("artifactType", artifactType.storageValue),
            )
        )
        .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
        .firstOrNull()
        ?.toArtifactFileOrNull()

    private fun Document.toArtifactFileOrNull(): ElectronicDocumentArtifactFile? {
        val objectKey = getString("objectKey") ?: getString(MongoDocumentFields.ID) ?: return null
        val target = rootPath.resolve(objectKey).normalize()
        if (!target.startsWith(rootPath) || !Files.exists(target)) return null
        val bytes = Files.readAllBytes(target)
        if (bytes.isEmpty()) return null
        return ElectronicDocumentArtifactFile(
            objectKey = objectKey,
            artifactType = ElectronicDocumentArtifactType.fromStorage(getString("artifactType")),
            filename = getString("fileName") ?: objectKey.substringAfterLast('/'),
            contentType = getString("contentType") ?: "application/octet-stream",
            bytes = bytes,
            sha256 = getString("sha256") ?: bytes.sha256Hex(),
            createdAt = (this[MongoDocumentFields.CREATED_AT] as? Date)?.toInstant()
                ?: (this[MongoDocumentFields.UPDATED_AT] as? Date)?.toInstant()
                ?: throw IllegalArgumentException("Artifact metadata does not contain createdAt."),
        )
    }

    private fun buildObjectKey(command: StoreElectronicDocumentArtifactCommand, sha256: String): String {
        val organization = command.organizationId.safeSegment()
        val document = command.documentId.safeSegment()
        val type = command.artifactType.storageValue.safeSegment()
        val timestamp = command.createdAt.toEpochMilli().toString()
        val fileName = command.fileName.safeFileName()
        return "electronic-invoicing/$organization/$document/$type/${timestamp}_${sha256.take(12)}_$fileName"
    }

    private fun String.safeSegment(): String =
        trim().replace(Regex("[^A-Za-z0-9_.-]"), "_").take(128).ifBlank { "unknown" }

    private fun String.safeFileName(): String =
        trim()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .take(160)
            .ifBlank { "artifact.bin" }

    private fun ByteArray.sha256Hex(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
