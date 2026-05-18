package com.hermes.backend.payments

import com.hermes.application.payments.CloseCashSessionUseCase
import com.hermes.application.payments.CreateReceivableForSaleUseCase
import com.hermes.application.payments.OpenCashSessionUseCase
import com.hermes.application.payments.RegisterCashMovementUseCase
import com.hermes.application.payments.RegisterPaymentUseCase
import com.hermes.application.payments.RegisterReceivableCollectionUseCase
import com.hermes.application.payments.UuidPaymentsIdGenerator
import com.hermes.infrastructure.mongo.payments.MongoPaymentAuditLogger
import com.hermes.infrastructure.mongo.payments.MongoPaymentsStore
import com.hermes.infrastructure.mongo.sales.MongoSalesStore
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import java.time.Clock

object PaymentsModuleFactory {
    fun fromMongo(
        client: MongoClient,
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): PaymentsModule {
        val paymentsStore = MongoPaymentsStore(client = client, database = database)
        val salesStore = MongoSalesStore(database)
        val auditLogger = MongoPaymentAuditLogger(database)
        val idGenerator = UuidPaymentsIdGenerator()

        val settlementRepository = paymentsStore.settlementRepository

        return PaymentsModule(
            registerPaymentUseCase = RegisterPaymentUseCase(
                saleRepository = salesStore.saleRepository,
                paymentRepository = paymentsStore.paymentRepository,
                cashSessionRepository = paymentsStore.cashSessionRepository,
                cashMovementRepository = paymentsStore.cashMovementRepository,
                receivableRepository = paymentsStore.receivableRepository,
                settlementRepository = settlementRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            openCashSessionUseCase = OpenCashSessionUseCase(
                cashSessionRepository = paymentsStore.cashSessionRepository,
                cashMovementRepository = paymentsStore.cashMovementRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            registerCashMovementUseCase = RegisterCashMovementUseCase(
                cashSessionRepository = paymentsStore.cashSessionRepository,
                cashMovementRepository = paymentsStore.cashMovementRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            closeCashSessionUseCase = CloseCashSessionUseCase(
                cashSessionRepository = paymentsStore.cashSessionRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            createReceivableForSaleUseCase = CreateReceivableForSaleUseCase(
                saleRepository = salesStore.saleRepository,
                receivableRepository = paymentsStore.receivableRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
            registerReceivableCollectionUseCase = RegisterReceivableCollectionUseCase(
                saleRepository = salesStore.saleRepository,
                receivableRepository = paymentsStore.receivableRepository,
                cashSessionRepository = paymentsStore.cashSessionRepository,
                cashMovementRepository = paymentsStore.cashMovementRepository,
                settlementRepository = settlementRepository,
                idGenerator = idGenerator,
                auditLogger = auditLogger,
                clock = clock,
            ),
        )
    }
}
