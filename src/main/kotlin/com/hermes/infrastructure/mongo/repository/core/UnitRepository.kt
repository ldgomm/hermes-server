package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class UnitRepository(database: MongoDatabase) : DocumentMongoRepository(database, MongoCollectionNames.UNITS) {
    fun findByCode(code: String): Document? = findOne(eq("code", code.trim()))
}
