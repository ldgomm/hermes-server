package com.hermes.infrastructure.mongo.auth

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes

object AuthMongoBootstrap {
    fun ensureIndexes(database: MongoDatabase) {
        database.getCollection(MongoCollectionNames.USERS).run {
            createIndex(Indexes.ascending("email"), IndexOptions().name("users_email_unique_idx").unique(true))
            createIndex(Indexes.ascending("status", "createdAt"), IndexOptions().name("users_status_created_at_idx"))
        }

        database.getCollection(MongoCollectionNames.MEMBERSHIPS).run {
            createIndex(
                Indexes.ascending("organizationId", "userId"),
                IndexOptions().name("memberships_org_user_unique_idx").unique(true),
            )
            createIndex(
                Indexes.ascending("userId", "status"),
                IndexOptions().name("memberships_user_status_idx"),
            )
        }

        database.getCollection(MongoCollectionNames.ROLES).run {
            createIndex(Indexes.ascending("code", "scope"), IndexOptions().name("roles_code_scope_idx"))
            createIndex(Indexes.ascending("status"), IndexOptions().name("roles_status_idx"))
        }

        database.getCollection(MongoCollectionNames.USER_SESSIONS).run {
            createIndex(Indexes.ascending("userId", "status"), IndexOptions().name("user_sessions_user_status_idx"))
            createIndex(Indexes.ascending("expiresAt"), IndexOptions().name("user_sessions_expires_at_idx"))
        }

        database.getCollection(AuthMongoCollectionNames.REFRESH_TOKENS).run {
            createIndex(
                Indexes.ascending("tokenHash"),
                IndexOptions().name("refresh_tokens_hash_unique_idx").unique(true)
            )
            createIndex(
                Indexes.ascending("sessionId", "revokedAt", "usedAt"),
                IndexOptions().name("refresh_tokens_session_active_idx")
            )
            createIndex(
                Indexes.ascending("userId", "expiresAt"),
                IndexOptions().name("refresh_tokens_user_expires_idx")
            )
        }
    }
}
