package com.hermes.application.documents

import com.hermes.domain.document.CommercialDocument

data class CommercialDocumentResult(
    val document: CommercialDocument,
    val file: CommercialDocumentFile? = null,
)

data class CommercialDocumentsResult(
    val documents: List<CommercialDocument>,
)

data class CommercialDocumentFile(
    val objectKey: String,
    val filename: String,
    val contentType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommercialDocumentFile) return false
        return objectKey == other.objectKey &&
                filename == other.filename &&
                contentType == other.contentType &&
                bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = objectKey.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

data class EmailCommercialDocumentResult(
    val document: CommercialDocument,
    val delivered: Boolean,
)
