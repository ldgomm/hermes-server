package com.hermes.infrastructure.mongo

import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import org.bson.Document
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MongoConnectionTest {
    @Test
    fun `connects to MongoDB replica set through Testcontainers`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("mongo_connection_test"))

            val ping = database.runCommand(Document("ping", 1))
            val hello = database.runCommand(Document("hello", 1))

            assertEquals(1.0, ping.getDouble("ok"))
            assertNotNull(hello.getString("setName"))
            assertTrue(hello.containsKey("logicalSessionTimeoutMinutes"))
        }
    }
}
