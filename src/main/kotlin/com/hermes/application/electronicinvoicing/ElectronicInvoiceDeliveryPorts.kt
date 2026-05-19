package com.hermes.application.electronicinvoicing

import java.time.Instant

interface ElectronicDocumentArtifactReader {
    fun get(objectKey: String): ElectronicDocumentArtifactFile?

    fun findLatest(
        organizationId: String,
        documentId: String,
        artifactType: ElectronicDocumentArtifactType,
    ): ElectronicDocumentArtifactFile?
}

data class ElectronicInvoiceGeneratedFile(
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
) {
    init {
        require(filename.isNotBlank()) { "Generated file filename cannot be blank." }
        require(contentType.isNotBlank()) { "Generated file content type cannot be blank." }
        require(bytes.isNotEmpty()) { "Generated file bytes cannot be empty." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElectronicInvoiceGeneratedFile) return false
        return filename == other.filename &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class ElectronicDocumentArtifactFile(
    val objectKey: String,
    val artifactType: ElectronicDocumentArtifactType,
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
    val sha256: String,
    val createdAt: Instant,
) {
    init {
        require(objectKey.isNotBlank()) { "Electronic artifact object key cannot be blank." }
        require(filename.isNotBlank()) { "Electronic artifact filename cannot be blank." }
        require(contentType.isNotBlank()) { "Electronic artifact content type cannot be blank." }
        require(bytes.isNotEmpty()) { "Electronic artifact bytes cannot be empty." }
        require(Regex("^[A-Fa-f0-9]{64}$").matches(sha256)) { "Electronic artifact SHA-256 is invalid." }
    }

    fun toStoredArtifact(): StoredElectronicDocumentArtifact = StoredElectronicDocumentArtifact(
        objectKey = objectKey,
        artifactType = artifactType,
        sha256 = sha256,
        sizeBytes = bytes.size.toLong(),
        createdAt = createdAt,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElectronicDocumentArtifactFile) return false
        return objectKey == other.objectKey &&
            artifactType == other.artifactType &&
            filename == other.filename &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes) &&
            sha256 == other.sha256 &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = objectKey.hashCode()
        result = 31 * result + artifactType.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + sha256.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

interface ElectronicInvoiceRideRenderer {
    fun render(command: ElectronicInvoiceRideRenderCommand): ElectronicInvoiceGeneratedFile
}

data class ElectronicInvoiceRideRenderCommand(
    val record: ElectronicInvoiceIssueRecord,
    val authorizedXml: ByteArray,
    val generatedAt: Instant,
) {
    init {
        if (authorizedXml.isEmpty()) throw IllegalArgumentException("Authorized XML cannot be empty for RIDE rendering.")
    }
}

interface ElectronicInvoiceEmailSender {
    fun send(email: ElectronicInvoiceEmail): Boolean
}

object NoopElectronicInvoiceEmailSender : ElectronicInvoiceEmailSender {
    override fun send(email: ElectronicInvoiceEmail): Boolean = true
}

data class ElectronicInvoiceEmail(
    val to: String,
    val subject: String,
    val message: String,
    val attachments: List<ElectronicInvoiceEmailAttachment>,
    val record: ElectronicInvoiceIssueRecord,
) {
    init {
        require(to.isNotBlank()) { "Electronic invoice email recipient cannot be blank." }
        require(subject.isNotBlank()) { "Electronic invoice email subject cannot be blank." }
        require(message.isNotBlank()) { "Electronic invoice email message cannot be blank." }
        require(attachments.isNotEmpty()) { "Electronic invoice email requires attachments." }
    }
}

data class ElectronicInvoiceEmailAttachment(
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
) {
    init {
        require(filename.isNotBlank()) { "Email attachment filename cannot be blank." }
        require(contentType.isNotBlank()) { "Email attachment content type cannot be blank." }
        require(bytes.isNotEmpty()) { "Email attachment bytes cannot be empty." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ElectronicInvoiceEmailAttachment) return false
        return filename == other.filename &&
            contentType == other.contentType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
