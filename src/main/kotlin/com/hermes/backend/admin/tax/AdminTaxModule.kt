package com.hermes.backend.admin.tax

import com.hermes.application.admin.tax.GetAdminTaxReadinessUseCase
import com.hermes.application.admin.tax.SearchAdminTaxProfilesUseCase
import com.hermes.application.admin.tax.SearchAdminTaxRatesUseCase
import com.hermes.application.catalog.AssignTaxProfileToCatalogItemUseCase
import com.hermes.application.tax.*

data class AdminTaxModule(
    val searchRatesUseCase: SearchAdminTaxRatesUseCase,
    val getRateUseCase: TaxGetRateUseCase,
    val createRateUseCase: TaxCreateRateUseCase,
    val updateRateUseCase: TaxUpdateRateUseCase,

    val searchProfilesUseCase: SearchAdminTaxProfilesUseCase,
    val getProfileUseCase: TaxGetProfileUseCase,
    val createProfileUseCase: TaxCreateProfileUseCase,
    val updateProfileUseCase: TaxUpdateProfileUseCase,

    val assignTaxProfileToCatalogItemUseCase: AssignTaxProfileToCatalogItemUseCase,
    val readinessUseCase: GetAdminTaxReadinessUseCase,
)
