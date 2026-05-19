package com.hermes.backend.admin.business

import com.hermes.application.admin.business.GetAdminBusinessReadinessUseCase
import com.hermes.application.admin.business.GetAdminBusinessUseCase
import com.hermes.application.admin.business.ListAdminActivitiesUseCase
import com.hermes.application.admin.business.ListAdminBranchesUseCase
import com.hermes.application.admin.business.ListAdminEmissionPointsUseCase

data class AdminBusinessModule(
    val getBusinessUseCase: GetAdminBusinessUseCase,
    val getReadinessUseCase: GetAdminBusinessReadinessUseCase,
    val listActivitiesUseCase: ListAdminActivitiesUseCase,
    val listBranchesUseCase: ListAdminBranchesUseCase,
    val listEmissionPointsUseCase: ListAdminEmissionPointsUseCase,
)
