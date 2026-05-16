package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class ElectronicDocumentPayloadRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.ELECTRONIC_DOCUMENT_PAYLOADS) {
    fun findByDocumentId(documentId: String): Document? = findOne(eq("documentId", documentId))
    fun findByAccessKey(accessKey: String): Document? = findOne(eq("accessKey", accessKey.trim()))
}
