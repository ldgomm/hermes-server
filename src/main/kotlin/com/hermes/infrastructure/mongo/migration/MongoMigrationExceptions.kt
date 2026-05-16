package com.hermes.infrastructure.mongo.migration

open class MongoMigrationException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class MongoMigrationLockException(
    message: String,
    cause: Throwable? = null,
) : MongoMigrationException(message, cause)

class MongoMigrationValidationException(message: String) : MongoMigrationException(message)

class MongoMigrationFailedException(
    migrationId: String,
    cause: Throwable,
) : MongoMigrationException("Mongo migration '$migrationId' failed.", cause)
