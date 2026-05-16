package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class SpecialHoursRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.SPECIAL_HOURS) {
    fun findByBranchAndRange(organizationId: String, branchId: String, from: Date, to: Date): List<Document> = findMany(
        filter = and(organizationFilter(organizationId), eq("branchId", branchId), gte("date", from), lte("date", to)),
        sort = Sorts.ascending("date"),
    )
}
