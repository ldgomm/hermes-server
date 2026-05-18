package com.hermes.application.documents

class InMemoryCommercialDocumentFileStorage : CommercialDocumentFileStorage {
    private val files = linkedMapOf<String, CommercialDocumentFile>()

    override fun put(file: CommercialDocumentFile): CommercialDocumentFile {
        files[file.objectKey] = file
        return file
    }

    override fun get(objectKey: String): CommercialDocumentFile? = files[objectKey]
}
