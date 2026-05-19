package com.hermes.backend.admin.business

import com.hermes.application.admin.business.ChangeAdminActivityStatusUseCase
import com.hermes.application.admin.business.ChangeAdminBranchStatusUseCase
import com.hermes.application.admin.business.CreateAdminActivityUseCase
import com.hermes.application.admin.business.CreateAdminBranchUseCase
import com.hermes.application.admin.business.GetAdminActivityUseCase
import com.hermes.application.admin.business.GetAdminBranchUseCase
import com.hermes.application.admin.business.GetAdminBusinessReadinessUseCase
import com.hermes.application.admin.business.GetAdminBusinessUseCase
import com.hermes.application.admin.business.ListAdminActivitiesUseCase
import com.hermes.application.admin.business.ListAdminBranchesUseCase
import com.hermes.application.admin.business.ListAdminEmissionPointsUseCase
import com.hermes.application.admin.business.UpdateAdminActivityUseCase
import com.hermes.application.admin.business.UpdateAdminBranchUseCase
import com.hermes.application.admin.business.UpdateAdminBusinessUseCase

/**
 * Nullable mutation use cases keep previous 13A tests/fakes backwards compatible
 * while this phase is applied incrementally. Production factory configures all of them.
 */
data class AdminBusinessModule(
    val getBusinessUseCase: GetAdminBusinessUseCase,
    val getReadinessUseCase: GetAdminBusinessReadinessUseCase,
    val listActivitiesUseCase: ListAdminActivitiesUseCase,
    val listBranchesUseCase: ListAdminBranchesUseCase,
    val listEmissionPointsUseCase: ListAdminEmissionPointsUseCase,
    val updateBusinessUseCase: UpdateAdminBusinessUseCase? = null,
    val getActivityUseCase: GetAdminActivityUseCase? = null,
    val createActivityUseCase: CreateAdminActivityUseCase? = null,
    val updateActivityUseCase: UpdateAdminActivityUseCase? = null,
    val changeActivityStatusUseCase: ChangeAdminActivityStatusUseCase? = null,
    val getBranchUseCase: GetAdminBranchUseCase? = null,
    val createBranchUseCase: CreateAdminBranchUseCase? = null,
    val updateBranchUseCase: UpdateAdminBranchUseCase? = null,
    val changeBranchStatusUseCase: ChangeAdminBranchStatusUseCase? = null,
)
