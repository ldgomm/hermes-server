package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class SriSubmissionRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.SRI_SUBMISSIONS) {
    fun findByPayloadId(payloadId: String): List<Document> =
        findMany(eq("payloadId", payloadId), sort = Sorts.descending("submittedAt"))

    fun findByAccessKey(accessKey: String): List<Document> =
        findMany(eq("accessKey", accessKey), sort = Sorts.descending("submittedAt"))
}
