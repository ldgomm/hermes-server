package com.hermes.infrastructure.mongo.documents

import com.hermes.application.documents.CommercialDocumentFile
import com.hermes.application.documents.CommercialDocumentFileStorage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

class FileSystemCommercialDocumentFileStorage(
    private val root: Path,
) : CommercialDocumentFileStorage {
    init {
        root.createDirectories()
    }

    override fun put(file: CommercialDocumentFile): CommercialDocumentFile {
        val target = root.resolve(file.objectKey).normalize()
        require(target.startsWith(root.normalize())) { "Invalid commercial document object key." }
        target.parent.createDirectories()
        Files.write(target, file.bytes)
        return file
    }

    override fun get(objectKey: String): CommercialDocumentFile? {
        val target = root.resolve(objectKey).normalize()
        require(target.startsWith(root.normalize())) { "Invalid commercial document object key." }
        if (!target.exists()) return null
        return CommercialDocumentFile(
            objectKey = objectKey,
            filename = target.name,
            contentType = "application/pdf",
            bytes = Files.readAllBytes(target),
        )
    }
}
