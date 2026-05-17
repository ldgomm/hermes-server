package com.hermes.backend.tax

import com.hermes.application.tax.EmptyTaxAuditQueryRepository
import com.hermes.application.tax.TaxCalculatePreviewUseCase
import com.hermes.application.tax.TaxCreateProfileUseCase
import com.hermes.application.tax.TaxCreateRateUseCase
import com.hermes.application.tax.TaxDocumentEmissionValidationUseCase
import com.hermes.application.tax.TaxGetOrganizationSettingsUseCase
import com.hermes.application.tax.TaxGetProfileUseCase
import com.hermes.application.tax.TaxGetRateUseCase
import com.hermes.application.tax.TaxListActiveProfilesUseCase
import com.hermes.application.tax.TaxListActiveRatesUseCase
import com.hermes.application.tax.TaxListAuditEventsUseCase
import com.hermes.application.tax.TaxSaleValidationUseCase
import com.hermes.application.tax.TaxUpdateOrganizationSettingsUseCase
import com.hermes.application.tax.TaxUpdateProfileUseCase
import com.hermes.application.tax.TaxUpdateRateUseCase

data class TaxModule(
    val listActiveRatesUseCase: TaxListActiveRatesUseCase,
    val getRateUseCase: TaxGetRateUseCase,
    val createRateUseCase: TaxCreateRateUseCase,
    val updateRateUseCase: TaxUpdateRateUseCase,

    val listActiveProfilesUseCase: TaxListActiveProfilesUseCase,
    val getProfileUseCase: TaxGetProfileUseCase,
    val createProfileUseCase: TaxCreateProfileUseCase,
    val updateProfileUseCase: TaxUpdateProfileUseCase,

    val getOrganizationSettingsUseCase: TaxGetOrganizationSettingsUseCase,
    val updateOrganizationSettingsUseCase: TaxUpdateOrganizationSettingsUseCase,

    val calculatePreviewUseCase: TaxCalculatePreviewUseCase,

    val listAuditEventsUseCase: TaxListAuditEventsUseCase = TaxListAuditEventsUseCase(EmptyTaxAuditQueryRepository),
    val validateSaleUseCase: TaxSaleValidationUseCase? = null,
    val validateDocumentEmissionUseCase: TaxDocumentEmissionValidationUseCase? = null,
)
