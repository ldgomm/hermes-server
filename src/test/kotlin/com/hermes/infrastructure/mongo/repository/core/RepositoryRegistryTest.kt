package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.migration.HermesMongoMigrations
import com.hermes.infrastructure.mongo.migration.MongoMigrationRunner
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryRegistryTest {
    @Test
    fun `repository registry exposes every phase 4 collection wrapper`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database =
                client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_2_repository_registry_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            val registry = MongoRepositoryRegistry(database)
            val repositories = registry.documentRepositories
            val existingCollections = database.listCollectionNames().toSet()

            assertEquals(56, repositories.size)
            assertEquals(56, repositories.map { it.collectionName }.toSet().size)

            repositories.forEach { repository ->
                assertTrue(repository.collectionName in existingCollections) {
                    "Missing migrated Mongo collection for repository ${repository::class.simpleName}: ${repository.collectionName}"
                }
                assertEquals(0, repository.count())
            }
        }
    }
}
