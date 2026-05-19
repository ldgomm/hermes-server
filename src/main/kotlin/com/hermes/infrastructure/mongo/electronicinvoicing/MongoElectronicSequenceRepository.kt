package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicSequenceQueryRepository
import com.hermes.application.electronicinvoicing.ElectronicSequenceRepository
import com.hermes.application.electronicinvoicing.ElectronicSequenceSearchQuery
import com.hermes.application.electronicinvoicing.NextElectronicSequentialCommand
import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.ElectronicSequenceReservation
import com.hermes.domain.electronicinvoicing.SriSequential
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.*
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import org.bson.Document
import org.bson.conversions.Bson
import java.util.*

class MongoElectronicSequenceRepository(
    database: MongoDatabase,
) : ElectronicSequenceRepository, ElectronicSequenceQueryRepository {
    private val collection: MongoCollection<Document> =
        database.getCollection(ElectronicSequenceMongoCollectionNames.ELECTRONIC_SEQUENCES)

    override fun createIfMissing(sequence: ElectronicSequence): ElectronicSequence {
        collection.updateOne(
            sequence.key.filter(),
            Document("\$setOnInsert", MongoElectronicSequenceMappers.toDocument(sequence)),
            UpdateOptions().upsert(true),
        )
        return findByKey(sequence.key)
            ?: throw IllegalStateException("Electronic sequence ${sequence.key.storageKey} was not created or found.")
    }

    override fun findByKey(key: ElectronicSequenceKey): ElectronicSequence? =
        collection.find(key.filter()).firstOrNull()?.let(MongoElectronicSequenceMappers::fromDocument)

    override fun findById(organizationId: String, sequenceId: String): ElectronicSequence? =
        collection.find(
            and(
                eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                eq(MongoDocumentFields.ID, sequenceId.trim()),
            )
        ).firstOrNull()?.let(MongoElectronicSequenceMappers::fromDocument)

    override fun search(query: ElectronicSequenceSearchQuery): List<ElectronicSequence> {
        val filters = mutableListOf<Bson>(eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()))
        query.environment?.let { filters += eq("environment", it.storageValue) }
        query.documentType?.let { filters += eq("documentType", it.storageValue) }
        query.status?.let { filters += eq("status", it.storageValue) }
        return collection.find(and(filters))
            .sort(Sorts.ascending("environment", "documentType", "establishmentCode", "emissionPointCode"))
            .limit(query.limit.coerceIn(1, 200))
            .map(MongoElectronicSequenceMappers::fromDocument)
            .toList()
    }

    override fun nextSequential(command: NextElectronicSequentialCommand): ElectronicSequenceReservation {
        val key = ElectronicSequenceKey(
            organizationId = command.organizationId,
            environment = command.environment,
            documentType = command.documentType,
            series = command.series,
        )

        val updates = mutableListOf<Bson>(
            Updates.inc("currentValue", 1),
            Updates.set("lastIssuedAt", Date.from(command.issuedAt)),
            Updates.set(MongoDocumentFields.UPDATED_AT, Date.from(command.issuedAt)),
            Updates.inc(MongoDocumentFields.VERSION, 1),
        )
        val documentId = command.documentId?.trim()?.takeIf { it.isNotBlank() }
        if (documentId != null) updates += Updates.set("lastIssuedDocumentId", documentId)

        val updated = collection.findOneAndUpdate(
            Filters.and(
                key.filter(),
                Filters.eq("status", "active"),
                Filters.lt("currentValue", SriSequential.MAX_VALUE),
            ),
            Updates.combine(updates),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(false),
        ) ?: throw IllegalStateException(
            "Active electronic sequence ${key.storageKey} does not exist or is exhausted.",
        )

        val sequence = MongoElectronicSequenceMappers.fromDocument(updated)
        return ElectronicSequenceReservation(
            sequence = sequence,
            sequential = SriSequential(sequence.currentValue),
        )
    }

    private fun ElectronicSequenceKey.filter(): Bson = Filters.and(
        Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
        Filters.eq("environment", environment.storageValue),
        Filters.eq("documentType", documentType.storageValue),
        Filters.eq("establishmentCode", series.establishmentCode),
        Filters.eq("emissionPointCode", series.emissionPointCode),
    )
}
