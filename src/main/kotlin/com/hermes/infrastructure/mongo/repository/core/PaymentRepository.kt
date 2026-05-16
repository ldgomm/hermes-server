package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Sorts
import org.bson.Document

class PaymentRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.PAYMENTS) {
    fun findBySale(organizationId: String, saleId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("saleId", saleId)), sort = Sorts.descending("paidAt"))

    fun findByCashSession(organizationId: String, cashSessionId: String): List<Document> = findMany(
        and(organizationFilter(organizationId), eq("cashSessionId", cashSessionId)), sort = Sorts.descending("paidAt")
    )

    fun findByExternalReference(organizationId: String, externalReference: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("externalReference", externalReference.trim())))
}
