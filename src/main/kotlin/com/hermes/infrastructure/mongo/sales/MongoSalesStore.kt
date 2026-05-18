package com.hermes.infrastructure.mongo.sales

import com.hermes.application.sales.OperationalReservationRepository
import com.hermes.application.sales.OperationalSaleRepository
import com.hermes.application.sales.ReservationSearchQuery
import com.hermes.application.sales.SaleSearchQuery
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.sale.Sale
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document
import org.bson.conversions.Bson

class MongoSalesStore(database: MongoDatabase) {
    val saleRepository: OperationalSaleRepository = MongoOperationalSaleRepository(database)
    val reservationRepository: OperationalReservationRepository = MongoOperationalReservationRepository(database)
}

private class MongoOperationalSaleRepository(database: MongoDatabase) : OperationalSaleRepository {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.SALES)

    override fun create(sale: Sale) {
        collection.insertOne(MongoSalesMappers.saleToDocument(sale))
    }

    override fun update(sale: Sale) {
        collection.replaceOne(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, sale.id),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, sale.organizationId),
            ),
            MongoSalesMappers.saleToDocument(sale),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findById(organizationId: String, saleId: String): Sale? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, saleId.trim()),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
            )
        ).firstOrNull()?.let(MongoSalesMappers::saleFromDocument)

    override fun search(query: SaleSearchQuery): List<Sale> {
        val filters = mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()))
        if (query.statuses.isNotEmpty()) filters += Filters.`in`(
            "operationalStatus",
            query.statuses.map { it.name.lowercase() })
        query.customerId?.takeIf { it.isNotBlank() }?.let { filters += Filters.eq("customerId", it.trim()) }
        query.activityId?.takeIf { it.isNotBlank() }?.let { filters += Filters.eq("activityId", it.trim()) }
        query.from?.let { filters += Filters.gte(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(it)) }
        query.to?.let { filters += Filters.lte(MongoDocumentFields.CREATED_AT, MongoInstantMapper.toDate(it)) }

        return collection.find(Filters.and(filters))
            .sort(Sorts.descending(MongoDocumentFields.CREATED_AT))
            .limit(query.limit.coerceIn(1, 500))
            .into(mutableListOf())
            .map(MongoSalesMappers::saleFromDocument)
    }
}

private class MongoOperationalReservationRepository(database: MongoDatabase) : OperationalReservationRepository {
    private val collection: MongoCollection<Document> = database.getCollection(MongoCollectionNames.RESERVATIONS)

    override fun create(reservation: Reservation) {
        collection.insertOne(MongoSalesMappers.reservationToDocument(reservation))
    }

    override fun update(reservation: Reservation) {
        collection.replaceOne(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, reservation.id),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, reservation.organizationId),
            ),
            MongoSalesMappers.reservationToDocument(reservation),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findById(organizationId: String, reservationId: String): Reservation? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, reservationId.trim()),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
            )
        ).firstOrNull()?.let(MongoSalesMappers::reservationFromDocument)

    override fun search(query: ReservationSearchQuery): List<Reservation> {
        val filters = mutableListOf<Bson>(Filters.eq(MongoDocumentFields.ORGANIZATION_ID, query.organizationId.trim()))
        if (query.statuses.isNotEmpty()) filters += Filters.`in`("status", query.statuses.map { it.name.lowercase() })
        query.customerId?.takeIf { it.isNotBlank() }?.let { filters += Filters.eq("customerId", it.trim()) }
        query.activityId?.takeIf { it.isNotBlank() }?.let { filters += Filters.eq("activityId", it.trim()) }
        query.from?.let { filters += Filters.gte("startAt", MongoInstantMapper.toDate(it)) }
        query.to?.let { filters += Filters.lte("startAt", MongoInstantMapper.toDate(it)) }

        return collection.find(Filters.and(filters))
            .sort(Sorts.ascending("startAt"))
            .limit(query.limit.coerceIn(1, 500))
            .into(mutableListOf())
            .map(MongoSalesMappers::reservationFromDocument)
    }
}
