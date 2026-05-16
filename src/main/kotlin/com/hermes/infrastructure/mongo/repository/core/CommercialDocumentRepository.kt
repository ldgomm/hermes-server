package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class CommercialDocumentRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.COMMERCIAL_DOCUMENTS) {
    fun findBySale(organizationId: String, saleId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("saleId", saleId)))

    fun findByAccessKey(accessKey: String): Document? = findOne(eq("accessKey", accessKey.trim()))
    fun findByDocumentNumber(organizationId: String, documentNumber: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("documentNumber", documentNumber.trim())))
}
