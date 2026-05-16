package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.MongoWriteException
import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValidatorsTest {
    @Test
    fun `organization validator rejects invalid documents and unique tax id is enforced`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_validators_org_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            val organizations = database.getCollection(MongoCollectionNames.ORGANIZATIONS)

            assertFailsWith<MongoWriteException> {
                organizations.insertOne(Document("_id", "org_invalid"))
            }

            organizations.insertOne(validOrganization("org_001", "0503638371001"))

            assertFailsWith<MongoWriteException> {
                organizations.insertOne(validOrganization("org_002", "0503638371001"))
            }

            assertEquals(1, organizations.countDocuments())
        }
    }

    @Test
    fun `organization catalog validator requires organization id and decimal price`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database =
                client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_validators_catalog_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            val catalog = database.getCollection(MongoCollectionNames.ORGANIZATION_CATALOG_ITEMS)

            assertFailsWith<MongoWriteException> {
                catalog.insertOne(
                    baseDocument("item_invalid")
                        .append("activityId", "act_restaurant")
                        .append("localName", "Cuy entero")
                        .append("normalizedName", "cuy entero")
                        .append("itemType", "product")
                        .append("status", "active")
                        .append("price", Document("amount", "24.00").append("currency", "USD"))
                        .append("searchableText", "cuy entero"),
                )
            }

            catalog.insertOne(
                baseDocument("item_001")
                    .append("organizationId", "org_001")
                    .append("activityId", "act_restaurant")
                    .append("localName", "Cuy entero")
                    .append("normalizedName", "cuy entero")
                    .append("itemType", "product")
                    .append("status", "active")
                    .append("price", Document("amount", Decimal128(BigDecimal("24.00"))).append("currency", "USD"))
                    .append("searchableText", "cuy entero restaurante"),
            )

            assertEquals(1, catalog.countDocuments())
        }
    }

    private fun validOrganization(id: String, taxId: String): Document =
        baseDocument(id)
            .append("legalName", "ALTOS DEL MURCO")
            .append("commercialName", "Altos del Murco")
            .append("taxId", taxId)
            .append("taxIdType", "ruc")
            .append("countryCode", "EC")
            .append("timezone", "America/Guayaquil")
            .append("defaultCurrency", "USD")
            .append("taxRegime", "rimpe_entrepreneur")
            .append("businessModel", "multi_activity")
            .append("primaryBusinessType", "restaurant")
            .append("status", "onboarding")

    private fun baseDocument(id: String): Document =
        Document("_id", id)
            .append("createdAt", Date())
            .append("createdBy", "usr_test")
            .append("updatedAt", Date())
            .append("updatedBy", "usr_test")
            .append("version", 1)
            .append("schemaVersion", 1)
}
