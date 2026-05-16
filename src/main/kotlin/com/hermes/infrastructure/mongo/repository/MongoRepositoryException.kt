package com.hermes.infrastructure.mongo.repository

open class MongoRepositoryException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class MongoEntityNotFoundException(
    collectionName: String,
    id: String,
) : MongoRepositoryException("Entity '$id' was not found in collection '$collectionName'.")

class MongoOptimisticLockException(
    collectionName: String,
    id: String,
    expectedVersion: Long,
) : MongoRepositoryException(
    "Optimistic lock failed for entity '$id' in collection '$collectionName'. Expected version: $expectedVersion.",
)

class MongoDuplicateEntityException(
    collectionName: String,
    id: String,
    cause: Throwable? = null,
) : MongoRepositoryException("Entity '$id' already exists in collection '$collectionName'.", cause)
