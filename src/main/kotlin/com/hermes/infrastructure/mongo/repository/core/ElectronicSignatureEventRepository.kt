package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class ElectronicSignatureEventRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.ELECTRONIC_SIGNATURE_EVENTS) {
    fun findBySignature(signatureId: String): List<Document> =
        findMany(eq("signatureId", signatureId), sort = Sorts.descending("createdAt"))
}
