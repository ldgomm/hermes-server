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
