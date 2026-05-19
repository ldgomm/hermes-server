package com.hermes.backend.admin.business

import com.hermes.application.admin.business.GetAdminBusinessReadinessUseCase
import com.hermes.application.admin.business.GetAdminBusinessUseCase
import com.hermes.application.admin.business.ListAdminActivitiesUseCase
import com.hermes.application.admin.business.ListAdminBranchesUseCase
import com.hermes.application.admin.business.ListAdminEmissionPointsUseCase
import com.hermes.application.admin.business.UpdateAdminBusinessUseCase

/**
 * updateBusinessUseCase is nullable only to keep 13A.1 tests/fakes backwards compatible
 * while applying this block incrementally. Production factory configures it.
 */
data class AdminBusinessModule(
    val getBusinessUseCase: GetAdminBusinessUseCase,
    val getReadinessUseCase: GetAdminBusinessReadinessUseCase,
    val listActivitiesUseCase: ListAdminActivitiesUseCase,
    val listBranchesUseCase: ListAdminBranchesUseCase,
    val listEmissionPointsUseCase: ListAdminEmissionPointsUseCase,
    val updateBusinessUseCase: UpdateAdminBusinessUseCase? = null,
)
