package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.*
import com.hermes.domain.electronicinvoicing.ElectronicDocumentStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import org.bson.Document
import java.util.*

class MongoElectronicInvoiceHomologationRunRepository(
    database: MongoDatabase,
) : ElectronicInvoiceHomologationRunRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(ElectronicInvoicingMongoCollectionNames.ELECTRONIC_HOMOLOGATION_RUNS)

    override fun create(run: ElectronicInvoiceHomologationRun) {
        collection.insertOne(toDocument(run))
    }

    override fun findById(organizationId: String, runId: String): ElectronicInvoiceHomologationRun? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq(MongoDocumentFields.ID, runId.trim()),
            )
        ).firstOrNull()?.let(::fromDocument)

    override fun search(query: ElectronicInvoiceHomologationRunSearchQuery): List<ElectronicInvoiceHomologationRun> {
        val filters = mutableListOf(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()))
        if (query.statuses.isNotEmpty()) {
            filters += Filters.`in`("status", query.statuses.map { it.storageValue })
        }
        return collection.find(Filters.and(filters))
            .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .limit(query.limit)
            .map(::fromDocument)
            .toList()
    }

    override fun findLatestApprovedForProduction(organizationId: String): ElectronicInvoiceHomologationRun? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("approvedForProduction", true),
                Filters.eq("status", ElectronicInvoiceHomologationRunStatus.PASSED.storageValue),
                Filters.eq("environment", SriEnvironment.TEST.storageValue),
            )
        )
            .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .firstOrNull()
            ?.let(::fromDocument)

    private fun toDocument(run: ElectronicInvoiceHomologationRun): Document {
        val document = Document()
            .append(MongoDocumentFields.ID, run.id)
            .append(MongoDocumentFields.ORGANIZATION_ID, run.organizationId)
            .append("status", run.status.storageValue)
            .append("environment", run.environment.storageValue)
            .append("requestedByUserId", run.requestedByUserId)
            .append("requiredScenarioCodes", run.requiredScenarioCodes.map { it.name }.sorted())
            .append("scenarioResults", run.scenarioResults.map(::scenarioResultToDocument))
            .append("reportMarkdown", run.reportMarkdown)
            .append("approvedForProduction", run.approvedForProduction)
            .append("startedAt", Date.from(run.startedAt))
            .append("finishedAt", run.finishedAt?.let(Date::from))
            .append(MongoDocumentFields.CREATED_AT, Date.from(run.createdAt))
            .append(MongoDocumentFields.CREATED_BY, run.requestedByUserId)
            .append(MongoDocumentFields.UPDATED_AT, Date.from(run.updatedAt))
            .append(MongoDocumentFields.UPDATED_BY, run.requestedByUserId)
            .append(MongoDocumentFields.VERSION, run.version)
            .append(MongoDocumentFields.SCHEMA_VERSION, run.schemaVersion)

        run.productionDecision?.let { decision ->
            document.append("productionDecision", decisionToDocument(decision))
        }

        return document
    }

    private fun fromDocument(document: Document): ElectronicInvoiceHomologationRun = ElectronicInvoiceHomologationRun(
        id = document.getString(MongoDocumentFields.ID),
        organizationId = document.getString(MongoDocumentFields.ORGANIZATION_ID),
        status = ElectronicInvoiceHomologationRunStatus.fromStorage(document.getString("status")),
        environment = SriEnvironment.fromStorage(document.getString("environment")),
        requestedByUserId = document.getString("requestedByUserId"),
        requiredScenarioCodes = document.getStringList("requiredScenarioCodes")
            .map { ElectronicInvoiceHomologationScenarioCode.valueOf(it) }
            .toSet(),
        scenarioResults = document.getDocumentList("scenarioResults").map(::scenarioResultFromDocument),
        reportMarkdown = document.getString("reportMarkdown"),
        productionDecision = document.get("productionDecision", Document::class.java)?.let(::decisionFromDocument),
        approvedForProduction = document.getBoolean("approvedForProduction", false),
        startedAt = document.getDate("startedAt").toInstant(),
        finishedAt = document.getDate("finishedAt")?.toInstant(),
        createdAt = document.getDate(MongoDocumentFields.CREATED_AT).toInstant(),
        updatedAt = document.getDate(MongoDocumentFields.UPDATED_AT).toInstant(),
        version = document.getLongLike(MongoDocumentFields.VERSION),
        schemaVersion = document.getInteger(MongoDocumentFields.SCHEMA_VERSION, 1),
    )

    private fun scenarioResultToDocument(result: ElectronicInvoiceHomologationScenarioResult): Document = Document()
        .append("code", result.code.name)
        .append("status", result.status.name)
        .append("documentId", result.documentId)
        .append("saleId", result.saleId)
        .append("finalDocumentStatus", result.finalDocumentStatus?.name)
        .append("accessKey", result.accessKey)
        .append("authorized", result.authorized)
        .append("delivered", result.delivered)
        .append("artifactTypes", result.artifactTypes.map { it.name }.sorted())
        .append("messages", result.messages)
        .append("startedAt", Date.from(result.startedAt))
        .append("finishedAt", Date.from(result.finishedAt))

    private fun scenarioResultFromDocument(document: Document): ElectronicInvoiceHomologationScenarioResult =
        ElectronicInvoiceHomologationScenarioResult(
            code = ElectronicInvoiceHomologationScenarioCode.valueOf(document.getString("code")),
            status = ElectronicInvoiceHomologationStepStatus.valueOf(document.getString("status")),
            documentId = document.getString("documentId"),
            saleId = document.getString("saleId"),
            finalDocumentStatus = document.getString("finalDocumentStatus")?.let(ElectronicDocumentStatus::valueOf),
            accessKey = document.getString("accessKey"),
            authorized = document.getBoolean("authorized", false),
            delivered = document.getBoolean("delivered", false),
            artifactTypes = document.getStringList("artifactTypes").map(ElectronicDocumentArtifactType::valueOf)
                .toSet(),
            messages = document.getStringList("messages"),
            startedAt = document.getDate("startedAt").toInstant(),
            finishedAt = document.getDate("finishedAt").toInstant(),
        )

    private fun decisionToDocument(decision: ElectronicInvoiceProductionReadinessDecision): Document = Document()
        .append("approved", decision.approved)
        .append("environment", decision.environment.storageValue)
        .append("reasons", decision.reasons)
        .append("decidedAt", Date.from(decision.decidedAt))

    private fun decisionFromDocument(document: Document): ElectronicInvoiceProductionReadinessDecision =
        ElectronicInvoiceProductionReadinessDecision(
            approved = document.getBoolean("approved", false),
            environment = SriEnvironment.fromStorage(document.getString("environment")),
            reasons = document.getStringList("reasons"),
            decidedAt = document.getDate("decidedAt").toInstant(),
        )
}

private fun Document.getStringList(name: String): List<String> =
    getList(name, String::class.java).orEmpty()

private fun Document.getDocumentList(name: String): List<Document> =
    getList(name, Document::class.java).orEmpty()

private fun Document.getLongLike(name: String): Long = when (val value = get(name)) {
    is Long -> value
    is Int -> value.toLong()
    is Number -> value.toLong()
    else -> 1L
}
