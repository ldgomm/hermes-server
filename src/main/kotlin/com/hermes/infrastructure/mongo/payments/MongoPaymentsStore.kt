package com.hermes.infrastructure.mongo.payments

import com.hermes.application.payments.CashMovementRepository
import com.hermes.application.payments.PaymentCashSessionRepository
import com.hermes.application.payments.PaymentRepository
import com.hermes.application.payments.PaymentSettlement
import com.hermes.application.payments.PaymentSettlementRepository
import com.hermes.application.payments.ReceivableRepository
import com.hermes.domain.cash.CashMovement
import com.hermes.domain.cash.CashSession
import com.hermes.domain.payment.Payment
import com.hermes.domain.payment.Receivable
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.hermes.infrastructure.mongo.mapping.MongoInstantMapper
import com.hermes.infrastructure.mongo.sales.MongoSalesMappers
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bson.Document

class MongoPaymentsStore(
    private val client: MongoClient,
    private val database: MongoDatabase,
) {
    private val payments = database.getCollection(MongoCollectionNames.PAYMENTS)
    private val receivables = database.getCollection(MongoCollectionNames.RECEIVABLES)
    private val cashSessions = database.getCollection(MongoCollectionNames.CASH_SESSIONS)
    private val cashMovements = database.getCollection(MongoCollectionNames.CASH_MOVEMENTS)
    private val sales = database.getCollection(MongoCollectionNames.SALES)

    val paymentRepository: PaymentRepository = MongoPaymentRepository(payments)
    val receivableRepository: ReceivableRepository = MongoReceivableRepository(receivables)
    val cashSessionRepository: PaymentCashSessionRepository = MongoCashSessionRepository(cashSessions, cashMovements)
    val cashMovementRepository: CashMovementRepository = MongoCashMovementRepository(cashMovements)
    val settlementRepository: PaymentSettlementRepository = MongoPaymentSettlementRepository(
        client = client,
        payments = payments,
        receivables = receivables,
        cashSessions = cashSessions,
        cashMovements = cashMovements,
        sales = sales,
    )
}

private class MongoPaymentRepository(
    private val collection: MongoCollection<Document>,
) : PaymentRepository {
    override fun create(payment: Payment) {
        collection.insertOne(MongoPaymentMappers.paymentToDocument(payment, branchId = null))
    }

    override fun findEffectiveBySale(organizationId: String, saleId: String): List<Payment> =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("saleId", saleId.trim()),
                Filters.`in`("status", listOf("confirmed", "allocated")),
            )
        ).sort(Sorts.descending("paidAt"))
            .into(mutableListOf())
            .map(MongoPaymentMappers::paymentFromDocument)
}

private class MongoReceivableRepository(
    private val collection: MongoCollection<Document>,
) : ReceivableRepository {
    override fun create(receivable: Receivable) {
        collection.insertOne(MongoPaymentMappers.receivableToDocument(receivable))
    }

    override fun update(receivable: Receivable) {
        collection.replaceOne(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, receivable.id),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, receivable.organizationId),
            ),
            MongoPaymentMappers.receivableToDocument(receivable),
            ReplaceOptions().upsert(false),
        )
    }

    override fun findById(organizationId: String, receivableId: String): Receivable? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, receivableId.trim()),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
            )
        ).firstOrNull()?.let(MongoPaymentMappers::receivableFromDocument)

    override fun findBySaleId(organizationId: String, saleId: String): Receivable? =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("saleId", saleId.trim()),
            )
        ).firstOrNull()?.let(MongoPaymentMappers::receivableFromDocument)
}

private class MongoCashSessionRepository(
    private val sessions: MongoCollection<Document>,
    private val movements: MongoCollection<Document>,
) : PaymentCashSessionRepository {
    override fun create(session: CashSession) {
        sessions.insertOne(MongoPaymentMappers.cashSessionToDocument(session))
    }

    override fun findById(organizationId: String, cashSessionId: String): CashSession? =
        sessions.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, cashSessionId.trim()),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
            )
        ).firstOrNull()?.let { document ->
            MongoPaymentMappers.cashSessionFromDocument(document, findMovements(organizationId, cashSessionId))
        }

    override fun findOpenByOrganization(organizationId: String): CashSession? =
        sessions.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("status", "open"),
            )
        ).firstOrNull()?.let { document ->
            val id = document.getString(MongoDocumentFields.ID)
            MongoPaymentMappers.cashSessionFromDocument(document, findMovements(organizationId, id))
        }

    override fun findOpenByBranch(organizationId: String, branchId: String): CashSession? =
        sessions.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("branchId", branchId.trim()),
                Filters.eq("status", "open"),
            )
        ).firstOrNull()?.let { document ->
            val id = document.getString(MongoDocumentFields.ID)
            MongoPaymentMappers.cashSessionFromDocument(document, findMovements(organizationId, id))
        }

    override fun update(session: CashSession) {
        sessions.replaceOne(
            Filters.and(
                Filters.eq(MongoDocumentFields.ID, session.id),
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, session.organizationId),
            ),
            MongoPaymentMappers.cashSessionToDocument(session),
            ReplaceOptions().upsert(false),
        )
    }

    private fun findMovements(organizationId: String, cashSessionId: String): List<CashMovement> =
        movements.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("cashSessionId", cashSessionId.trim()),
            )
        ).sort(Sorts.ascending("occurredAt"))
            .into(mutableListOf())
            .map(MongoPaymentMappers::cashMovementFromDocument)
}

private class MongoCashMovementRepository(
    private val collection: MongoCollection<Document>,
) : CashMovementRepository {
    override fun create(movement: CashMovement) {
        collection.insertOne(MongoPaymentMappers.cashMovementToDocument(movement))
    }

    override fun findByCashSession(organizationId: String, cashSessionId: String): List<CashMovement> =
        collection.find(
            Filters.and(
                Filters.eq(MongoDocumentFields.ORGANIZATION_ID, organizationId.trim()),
                Filters.eq("cashSessionId", cashSessionId.trim()),
            )
        ).sort(Sorts.ascending("occurredAt"))
            .into(mutableListOf())
            .map(MongoPaymentMappers::cashMovementFromDocument)
}

private class MongoPaymentSettlementRepository(
    private val client: MongoClient,
    private val payments: MongoCollection<Document>,
    private val receivables: MongoCollection<Document>,
    private val cashSessions: MongoCollection<Document>,
    private val cashMovements: MongoCollection<Document>,
    private val sales: MongoCollection<Document>,
) : PaymentSettlementRepository {
    override fun persistPaymentSettlement(settlement: PaymentSettlement) {
        client.startSession().use { session ->
            session.startTransaction()
            try {
                val cashSessionId = settlement.cashSession?.id
                payments.insertOne(
                    session,
                    MongoPaymentMappers.paymentToDocument(
                        payment = settlement.payment,
                        branchId = settlement.sale.branchId,
                        cashSessionId = cashSessionId,
                        customerId = settlement.sale.customerId,
                    )
                )
                settlement.cashMovement?.let { movement ->
                    cashMovements.insertOne(session, MongoPaymentMappers.cashMovementToDocument(movement))
                }
                settlement.cashSession?.let { cashSession ->
                    cashSessions.replaceOne(
                        session,
                        Filters.and(
                            Filters.eq(MongoDocumentFields.ID, cashSession.id),
                            Filters.eq(MongoDocumentFields.ORGANIZATION_ID, cashSession.organizationId),
                        ),
                        MongoPaymentMappers.cashSessionToDocument(cashSession),
                        ReplaceOptions().upsert(false),
                    )
                }
                settlement.receivable?.let { receivable ->
                    receivables.insertOne(session, MongoPaymentMappers.receivableToDocument(receivable))
                }

                val saleDocument = MongoSalesMappers.saleToDocument(settlement.sale)
                    .append("paymentRefs", settlement.sale.payments.map { it.id })
                    .append("updatedAt", MongoInstantMapper.toDate(settlement.sale.updatedAt))
                sales.replaceOne(
                    session,
                    Filters.and(
                        Filters.eq(MongoDocumentFields.ID, settlement.sale.id),
                        Filters.eq(MongoDocumentFields.ORGANIZATION_ID, settlement.sale.organizationId),
                    ),
                    saleDocument,
                    ReplaceOptions().upsert(false),
                )
                session.commitTransaction()
            } catch (error: Throwable) {
                session.abortTransaction()
                throw error
            }
        }
    }
}
