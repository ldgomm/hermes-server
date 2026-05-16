package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class UnitConversionRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.UNIT_CONVERSIONS) {
    fun findConversion(organizationId: String?, itemId: String?, fromUnit: String, toUnit: String): Document? {
        val organizationFilter =
            if (organizationId == null) Document("organizationId", null) else eq("organizationId", organizationId)
        val itemFilter = if (itemId == null) Document("catalogItemId", null) else eq("catalogItemId", itemId)
        return findOne(
            and(
                organizationFilter, itemFilter, eq("fromUnit", fromUnit), eq("toUnit", toUnit), eq("status", "active")
            )
        )
    }

    fun findByCatalogItem(organizationId: String, catalogItemId: String): List<Document> =
        findMany(and(organizationFilter(organizationId), eq("catalogItemId", catalogItemId)))
}
