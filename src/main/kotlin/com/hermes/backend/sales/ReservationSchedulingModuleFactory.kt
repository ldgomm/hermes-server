package com.hermes.backend.sales

import com.hermes.application.sales.ChangeReservationStatusUseCase
import com.hermes.application.sales.CheckReservationAvailabilityUseCase
import com.hermes.application.sales.RescheduleReservationUseCase
import com.hermes.application.sales.ReservationSchedulingGuard
import com.hermes.application.sales.ReservationSchedulingRules
import com.hermes.infrastructure.mongo.sales.MongoSalesAuditLogger
import com.hermes.infrastructure.mongo.sales.MongoSalesStore
import com.mongodb.client.MongoDatabase
import java.time.Clock

object ReservationSchedulingModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
        rules: ReservationSchedulingRules = ReservationSchedulingRules(),
    ): ReservationSchedulingModule {
        val salesStore = MongoSalesStore(database)
        val auditLogger = MongoSalesAuditLogger(database)
        val schedulingGuard = ReservationSchedulingGuard(
            reservationRepository = salesStore.reservationRepository,
            rules = rules,
        )

        return ReservationSchedulingModule(
            checkReservationAvailabilityUseCase = CheckReservationAvailabilityUseCase(
                schedulingGuard = schedulingGuard,
                clock = clock,
            ),
            changeReservationStatusUseCase = ChangeReservationStatusUseCase(
                reservationRepository = salesStore.reservationRepository,
                auditLogger = auditLogger,
                clock = clock,
            ),
            rescheduleReservationUseCase = RescheduleReservationUseCase(
                reservationRepository = salesStore.reservationRepository,
                schedulingGuard = schedulingGuard,
                auditLogger = auditLogger,
                clock = clock,
            ),
        )
    }
}
