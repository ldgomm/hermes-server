package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import org.bson.Document
import org.bson.conversions.Bson

object MongoMigrationSupport {
    fun ensureCollection(
        database: MongoDatabase,
        name: String,
        validator: Document,
    ): MongoCollection<Document> {
        if (!collectionExists(database, name)) {
            database.createCollection(
                name,
                CreateCollectionOptions().validationOptions(
                    ValidationOptions()
                        .validator(validator)
                        .validationAction(ValidationAction.ERROR)
                        .validationLevel(ValidationLevel.MODERATE),
                ),
            )
        } else {
            database.runCommand(
                Document("collMod", name)
                    .append("validator", validator)
                    .append("validationAction", "error")
                    .append("validationLevel", "moderate"),
            )
        }

        return database.getCollection(name)
    }

    fun createIndex(
        collection: MongoCollection<Document>,
        keys: Bson,
        name: String,
        unique: Boolean = false,
        sparse: Boolean = false,
        partialFilterExpression: Bson? = null,
    ) {
        val options = IndexOptions()
            .name(name)
            .unique(unique)
            .sparse(sparse)

        if (partialFilterExpression != null) {
            options.partialFilterExpression(partialFilterExpression)
        }

        collection.createIndex(keys, options)
    }

    fun jsonSchema(
        required: List<String>,
        properties: Document,
        additionalProperties: Boolean = true,
    ): Document = Document(
        "\$jsonSchema",
        Document("bsonType", "object")
            .append("required", required)
            .append("additionalProperties", additionalProperties)
            .append("properties", properties),
    )

    fun commonRootProperties(requireOrganizationId: Boolean = true): Document {
        val properties = Document()
            .append(MongoDocumentFields.ID, id())
            .append("createdAt", date())
            .append("createdBy", nullableString())
            .append("updatedAt", date())
            .append("updatedBy", nullableString())
            .append("version", int())
            .append(MongoDocumentFields.SCHEMA_VERSION, int())

        if (requireOrganizationId) {
            properties.append(MongoDocumentFields.ORGANIZATION_ID, id(prefix = "org_"))
        }

        return properties
    }

    fun commonRequired(requireOrganizationId: Boolean = true): List<String> {
        val fields = mutableListOf(
            MongoDocumentFields.ID,
            "createdAt",
            "updatedAt",
            "version",
            MongoDocumentFields.SCHEMA_VERSION,
        )
        if (requireOrganizationId) fields += MongoDocumentFields.ORGANIZATION_ID
        return fields
    }

    fun id(prefix: String? = null): Document {
        val doc = Document("bsonType", "string")
            .append("minLength", 3)
            .append("maxLength", 128)
        if (prefix != null) {
            doc.append("pattern", "^${Regex.escape(prefix)}")
        }
        return doc
    }

    fun string(minLength: Int = 1, maxLength: Int = 512): Document =
        Document("bsonType", "string")
            .append("minLength", minLength)
            .append("maxLength", maxLength)

    fun nullableString(maxLength: Int = 512): Document =
        Document("bsonType", listOf("string", "null"))
            .append("maxLength", maxLength)

    fun enum(values: List<String>): Document =
        Document("bsonType", "string").append("enum", values)

    fun bool(): Document = Document("bsonType", "bool")

    fun int(): Document = Document("bsonType", "int")

    fun date(): Document = Document("bsonType", "date")

    fun nullableDate(): Document = Document("bsonType", listOf("date", "null"))

    fun decimal(): Document = Document("bsonType", "decimal")

    fun array(items: Document? = null): Document {
        val doc = Document("bsonType", "array")
        if (items != null) doc.append("items", items)
        return doc
    }

    fun obj(properties: Document = Document(), required: List<String> = emptyList()): Document {
        val doc = Document("bsonType", "object").append("properties", properties)
        if (required.isNotEmpty()) doc.append("required", required)
        return doc
    }

    fun moneyObject(): Document = obj(
        properties = Document()
            .append("amount", decimal())
            .append("currency", enum(listOf("USD"))),
        required = listOf("amount", "currency"),
    )

    private fun collectionExists(database: MongoDatabase, name: String): Boolean =
        database.listCollectionNames().into(mutableListOf()).contains(name)
}
