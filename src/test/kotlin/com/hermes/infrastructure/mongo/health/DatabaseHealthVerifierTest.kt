package com.hermes.infrastructure.mongo.health

import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseHealthVerifierTest {
    @Test
    fun `verifies MongoDB replica set and transaction readiness`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("health_test"))

            val result = DatabaseHealthVerifier(client, database).verify()

            assertTrue(result.ok, result.message)
            assertTrue(result.supportsSessions)
            assertEquals(TransactionProbeResult.SUPPORTED, result.transactionProbe)
            assertNotNull(result.replicaSetName)
        }
    }
}
