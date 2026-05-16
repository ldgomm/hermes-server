package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document

class ElectronicSignatureRepository(database: MongoDatabase) :
    DocumentMongoRepository(database, MongoCollectionNames.ELECTRONIC_SIGNATURES) {
    fun findActiveByOrganization(organizationId: String): Document? =
        findOne(and(organizationFilter(organizationId), eq("status", "valid")))

    fun findByFingerprint(fingerprint: String): Document? = findOne(eq("fingerprint", fingerprint.trim()))
}
