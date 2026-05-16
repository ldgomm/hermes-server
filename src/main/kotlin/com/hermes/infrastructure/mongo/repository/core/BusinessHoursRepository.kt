package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class BusinessHoursRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.BUSINESS_HOURS) {
    fun findActiveForBranch(organizationId: String, branchId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("branchId", branchId), eq("status", "active")))

    fun findActiveForActivity(organizationId: String, activityId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("activityId", activityId), eq("status", "active")))
}
