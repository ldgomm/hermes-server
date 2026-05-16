package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class EmissionPointRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.EMISSION_POINTS,
) {
    fun findByCodes(
        organizationId: String,
        establishmentCode: String,
        emissionPointCode: String,
    ): Document? = findOne(
        and(
            organizationFilter(organizationId),
            eq("establishmentCode", establishmentCode.trim()),
            eq("emissionPointCode", emissionPointCode.trim()),
        ),
    )

    fun findActiveByBranch(organizationId: String, branchId: String): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("branchId", branchId), eq("status", "active")),
    )
}
