package com.hermes.infrastructure.mongo.auth

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes

object CredentialAdminMongoBootstrap {
    fun ensureIndexes(database: MongoDatabase) {
        database.getCollection(CredentialAdminMongoCollectionNames.INVITATIONS).apply {
            createIndex(Indexes.ascending("tokenHash"), IndexOptions().unique(true))
            createIndex(Indexes.compoundIndex(Indexes.ascending("organizationId"), Indexes.ascending("email"), Indexes.ascending("status")))
            createIndex(Indexes.ascending("expiresAt"))
        }

        database.getCollection(CredentialAdminMongoCollectionNames.PASSWORD_RESET_TOKENS).apply {
            createIndex(Indexes.ascending("tokenHash"), IndexOptions().unique(true))
            createIndex(Indexes.compoundIndex(Indexes.ascending("userId"), Indexes.ascending("usedAt"), Indexes.ascending("revokedAt")))
            createIndex(Indexes.ascending("expiresAt"))
        }

        database.getCollection(CredentialAdminMongoCollectionNames.CREDENTIAL_EVENTS).apply {
            createIndex(Indexes.compoundIndex(Indexes.ascending("targetUserId"), Indexes.descending("createdAt")))
            createIndex(Indexes.compoundIndex(Indexes.ascending("organizationId"), Indexes.descending("createdAt")))
            createIndex(Indexes.ascending("action"))
        }
    }
}
