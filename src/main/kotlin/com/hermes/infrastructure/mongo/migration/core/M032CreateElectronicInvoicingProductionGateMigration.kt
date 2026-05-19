package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicInvoicingMongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes
import org.bson.Document

object M032CreateElectronicInvoicingProductionGateMigration : MongoMigration {
    override val id: String = "M032_create_electronic_invoicing_production_gate"
    override val description: String = "Create homologation runs and final operation indexes for SRI production gate."

    override fun up(database: MongoDatabase) {
        createHomologationRuns(database)
        createFinalElectronicInvoicingIndexes(database)
    }

    private fun createHomologationRuns(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append(MongoDocumentFields.VERSION, Document("bsonType", listOf("int", "long")))
            .append("status", MongoMigrationSupport.enum(listOf("running", "passed", "failed")))
            .append("environment", MongoMigrationSupport.enum(listOf("test", "production")))
            .append("requestedByUserId", MongoMigrationSupport.id(prefix = "usr_"))
            .append("requiredScenarioCodes", MongoMigrationSupport.array())
            .append("scenarioResults", MongoMigrationSupport.array())
            .append("reportMarkdown", MongoMigrationSupport.string(maxLength = 200_000))
            .append("productionDecision", Document("bsonType", listOf("object", "null")))
            .append("approvedForProduction", MongoMigrationSupport.bool())
            .append("startedAt", MongoMigrationSupport.date())
            .append("finishedAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = ElectronicInvoicingMongoCollectionNames.ELECTRONIC_HOMOLOGATION_RUNS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "status",
                    "environment",
                    "requestedByUserId",
                    "requiredScenarioCodes",
                    "scenarioResults",
                    "reportMarkdown",
                    "approvedForProduction",
                    "startedAt",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.descending(MongoDocumentFields.ORGANIZATION_ID, MongoDocumentFields.CREATED_AT),
            name = "electronic_homologation_runs_org_created_desc_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "status"),
                Indexes.descending(MongoDocumentFields.CREATED_AT),
            ),
            name = "electronic_homologation_runs_org_status_created_desc_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "approvedForProduction", "environment"),
                Indexes.descending(MongoDocumentFields.CREATED_AT),
            ),
            name = "electronic_homologation_runs_org_approved_environment_created_idx",
            partialFilterExpression = Filters.eq("approvedForProduction", true),
        )
    }

    private fun createFinalElectronicInvoicingIndexes(database: MongoDatabase) {
        val issues = database.getCollection(ElectronicInvoicingMongoCollectionNames.ELECTRONIC_INVOICE_ISSUES)
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID),
                Indexes.descending(MongoDocumentFields.CREATED_AT),
            ),
            name = "electronic_invoice_issues_org_created_desc_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "status"),
                Indexes.descending(MongoDocumentFields.CREATED_AT),
            ),
            name = "electronic_invoice_issues_org_status_created_desc_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "saleId"),
            name = "electronic_invoice_issues_org_sale_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "accessKey"),
            name = "electronic_invoice_issues_org_access_key_unique_idx",
            unique = true,
            partialFilterExpression = Document("accessKey", Document("\$type", "string")),
        )
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "documentNumber"),
            name = "electronic_invoice_issues_org_document_number_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.ascending(
                MongoDocumentFields.ORGANIZATION_ID,
                "environment",
                "documentType",
                "series",
            ),
            name = "electronic_invoice_issues_org_environment_doc_series_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = issues,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID),
                Indexes.descending("authorizedAt"),
            ),
            name = "electronic_invoice_issues_org_authorized_at_desc_idx",
            sparse = true,
        )

        val audit = database.getCollection(MongoCollectionNames.AUDIT_LOGS)
        MongoMigrationSupport.createIndex(
            collection = audit,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "entityId", MongoDocumentFields.CREATED_AT),
            name = "audit_logs_electronic_invoice_document_timeline_idx",
            partialFilterExpression = Filters.eq("module", "electronic_invoicing"),
        )
        MongoMigrationSupport.createIndex(
            collection = audit,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "action"),
                Indexes.descending(MongoDocumentFields.CREATED_AT),
            ),
            name = "audit_logs_electronic_invoice_action_created_desc_idx",
            partialFilterExpression = Filters.eq("module", "electronic_invoicing"),
        )
        MongoMigrationSupport.createIndex(
            collection = audit,
            keys = Indexes.compoundIndex(
                Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "actorUserId"),
                Indexes.descending(MongoDocumentFields.CREATED_AT),
            ),
            name = "audit_logs_electronic_invoice_actor_created_desc_idx",
            partialFilterExpression = Filters.eq("module", "electronic_invoicing"),
        )

        val settings = database.getCollection(ElectronicInvoicingMongoCollectionNames.ORGANIZATION_SRI_SETTINGS)
        MongoMigrationSupport.createIndex(
            collection = settings,
            keys = Indexes.ascending(MongoDocumentFields.ORGANIZATION_ID, "productionEnabled"),
            name = "organization_sri_settings_org_production_enabled_idx",
        )
    }
}
