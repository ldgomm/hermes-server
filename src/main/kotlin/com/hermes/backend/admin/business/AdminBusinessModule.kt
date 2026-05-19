package com.hermes.backend.admin.business

import com.hermes.application.admin.business.ChangeAdminActivityStatusUseCase
import com.hermes.application.admin.business.CreateAdminActivityUseCase
import com.hermes.application.admin.business.GetAdminActivityUseCase
import com.hermes.application.admin.business.GetAdminBusinessReadinessUseCase
import com.hermes.application.admin.business.GetAdminBusinessUseCase
import com.hermes.application.admin.business.ListAdminActivitiesUseCase
import com.hermes.application.admin.business.ListAdminBranchesUseCase
import com.hermes.application.admin.business.ListAdminEmissionPointsUseCase
import com.hermes.application.admin.business.UpdateAdminActivityUseCase
import com.hermes.application.admin.business.UpdateAdminBusinessUseCase

/**
 * Nullable activity use cases keep previous 13A.1/13A.2 tests/fakes backwards compatible
 * while this block is applied incrementally. Production factory configures all of them.
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
)
