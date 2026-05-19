package com.hermes.backend.admin.business

import com.hermes.application.admin.business.GetAdminBusinessReadinessUseCase
import com.hermes.application.admin.business.GetAdminBusinessUseCase
import com.hermes.application.admin.business.ListAdminActivitiesUseCase
import com.hermes.application.admin.business.ListAdminBranchesUseCase
import com.hermes.application.admin.business.ListAdminEmissionPointsUseCase
import com.hermes.infrastructure.mongo.admin.business.MongoAdminBusinessRepository
import com.mongodb.client.MongoDatabase
import java.time.Clock

object AdminBusinessModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): AdminBusinessModule {
        val repository = MongoAdminBusinessRepository(database)
        return AdminBusinessModule(
            getBusinessUseCase = GetAdminBusinessUseCase(repository),
            getReadinessUseCase = GetAdminBusinessReadinessUseCase(repository, clock),
            listActivitiesUseCase = ListAdminActivitiesUseCase(repository),
            listBranchesUseCase = ListAdminBranchesUseCase(repository),
            listEmissionPointsUseCase = ListAdminEmissionPointsUseCase(repository),
        )
    }
}
