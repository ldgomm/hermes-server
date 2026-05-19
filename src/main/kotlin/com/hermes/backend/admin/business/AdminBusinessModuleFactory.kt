package com.hermes.backend.admin.business

import com.hermes.application.admin.business.ChangeAdminActivityStatusUseCase
import com.hermes.application.admin.business.ChangeAdminBranchStatusUseCase
import com.hermes.application.admin.business.ChangeAdminEmissionPointStatusUseCase
import com.hermes.application.admin.business.CreateAdminActivityUseCase
import com.hermes.application.admin.business.CreateAdminBranchUseCase
import com.hermes.application.admin.business.CreateAdminEmissionPointUseCase
import com.hermes.application.admin.business.GetAdminActivityUseCase
import com.hermes.application.admin.business.GetAdminBranchUseCase
import com.hermes.application.admin.business.GetAdminBusinessFoundationOverviewUseCase
import com.hermes.application.admin.business.GetAdminBusinessReadinessUseCase
import com.hermes.application.admin.business.GetAdminBusinessUseCase
import com.hermes.application.admin.business.GetAdminEmissionPointUseCase
import com.hermes.application.admin.business.ListAdminActivitiesUseCase
import com.hermes.application.admin.business.ListAdminBranchesUseCase
import com.hermes.application.admin.business.ListAdminEmissionPointsUseCase
import com.hermes.application.admin.business.UpdateAdminActivityUseCase
import com.hermes.application.admin.business.UpdateAdminBranchUseCase
import com.hermes.application.admin.business.UpdateAdminBusinessUseCase
import com.hermes.application.admin.business.UpdateAdminEmissionPointUseCase
import com.hermes.application.admin.business.UuidAdminBusinessIdGenerator
import com.hermes.infrastructure.mongo.admin.business.MongoAdminBusinessRepository
import com.mongodb.client.MongoDatabase
import java.time.Clock

object AdminBusinessModuleFactory {
    fun fromMongo(
        database: MongoDatabase,
        clock: Clock = Clock.systemUTC(),
    ): AdminBusinessModule {
        val repository = MongoAdminBusinessRepository(database)
        val idGenerator = UuidAdminBusinessIdGenerator()
        return AdminBusinessModule(
            getBusinessUseCase = GetAdminBusinessUseCase(repository),
            getReadinessUseCase = GetAdminBusinessReadinessUseCase(repository, clock),
            listActivitiesUseCase = ListAdminActivitiesUseCase(repository),
            listBranchesUseCase = ListAdminBranchesUseCase(repository),
            listEmissionPointsUseCase = ListAdminEmissionPointsUseCase(repository),
            getFoundationOverviewUseCase = GetAdminBusinessFoundationOverviewUseCase(repository, clock),
            updateBusinessUseCase = UpdateAdminBusinessUseCase(
                readRepository = repository,
                mutationRepository = repository,
                clock = clock,
            ),
            getActivityUseCase = GetAdminActivityUseCase(repository),
            createActivityUseCase = CreateAdminActivityUseCase(
                repository = repository,
                idGenerator = idGenerator,
                clock = clock,
            ),
            updateActivityUseCase = UpdateAdminActivityUseCase(
                repository = repository,
                clock = clock,
            ),
            changeActivityStatusUseCase = ChangeAdminActivityStatusUseCase(
                repository = repository,
                clock = clock,
            ),
            getBranchUseCase = GetAdminBranchUseCase(repository),
            createBranchUseCase = CreateAdminBranchUseCase(
                repository = repository,
                idGenerator = idGenerator,
                clock = clock,
            ),
            updateBranchUseCase = UpdateAdminBranchUseCase(
                repository = repository,
                clock = clock,
            ),
            changeBranchStatusUseCase = ChangeAdminBranchStatusUseCase(
                repository = repository,
                clock = clock,
            ),
            getEmissionPointUseCase = GetAdminEmissionPointUseCase(repository),
            createEmissionPointUseCase = CreateAdminEmissionPointUseCase(
                repository = repository,
                idGenerator = idGenerator,
                clock = clock,
            ),
            updateEmissionPointUseCase = UpdateAdminEmissionPointUseCase(
                repository = repository,
                clock = clock,
            ),
            changeEmissionPointStatusUseCase = ChangeAdminEmissionPointStatusUseCase(
                repository = repository,
                clock = clock,
            ),
        )
    }
}
