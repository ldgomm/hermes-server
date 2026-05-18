package com.hermes.infrastructure.mongo.electronicinvoicing

import com.hermes.application.electronicinvoicing.ElectronicSequenceRepository
import com.hermes.application.electronicinvoicing.NextElectronicSequentialCommand
import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.ElectronicSequenceReservation
import com.hermes.domain.electronicinvoicing.SriSequential
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.conversions.Bson
import java.util.Date

class MongoElectronicSequenceRepository(
    database: MongoDatabase,
) : ElectronicSequenceRepository {
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
