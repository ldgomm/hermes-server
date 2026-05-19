package com.hermes.backend.admin.business

import com.hermes.application.admin.business.*

/**
 * Admin General Business Foundation module.
 *
 * Fase 13A.6 keeps the mutation use cases nullable for backwards-compatible
 * tests/fakes, but production factory configures all use cases including the
 * closure overview read model.
 */
data class AdminBusinessModule(
    val getBusinessUseCase: GetAdminBusinessUseCase,
    val getReadinessUseCase: GetAdminBusinessReadinessUseCase,
    val listActivitiesUseCase: ListAdminActivitiesUseCase,
    val listBranchesUseCase: ListAdminBranchesUseCase,
    val listEmissionPointsUseCase: ListAdminEmissionPointsUseCase,
    val getFoundationOverviewUseCase: GetAdminBusinessFoundationOverviewUseCase? = null,
    val updateBusinessUseCase: UpdateAdminBusinessUseCase? = null,
    val getActivityUseCase: GetAdminActivityUseCase? = null,
    val createActivityUseCase: CreateAdminActivityUseCase? = null,
    val updateActivityUseCase: UpdateAdminActivityUseCase? = null,
    val changeActivityStatusUseCase: ChangeAdminActivityStatusUseCase? = null,
    val getBranchUseCase: GetAdminBranchUseCase? = null,
    val createBranchUseCase: CreateAdminBranchUseCase? = null,
    val updateBranchUseCase: UpdateAdminBranchUseCase? = null,
    val changeBranchStatusUseCase: ChangeAdminBranchStatusUseCase? = null,
    val getEmissionPointUseCase: GetAdminEmissionPointUseCase? = null,
    val createEmissionPointUseCase: CreateAdminEmissionPointUseCase? = null,
    val updateEmissionPointUseCase: UpdateAdminEmissionPointUseCase? = null,
    val changeEmissionPointStatusUseCase: ChangeAdminEmissionPointStatusUseCase? = null,
)
