package com.hermes.application.tax

import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxRate

interface TaxRateRepository {
    fun create(rate: TaxRate)
    fun update(rate: TaxRate)
    fun findById(id: String): TaxRate?
    fun findByCode(code: String): TaxRate?
    fun findActive(): List<TaxRate>
}

interface TaxProfileRepository {
    fun create(profile: TaxProfile)
    fun update(profile: TaxProfile)
    fun findById(id: String): TaxProfile?
    fun findByCode(code: String): TaxProfile?
    fun findActive(): List<TaxProfile>
}

interface OrganizationTaxSettingsRepository {
    fun create(settings: OrganizationTaxSettings)
    fun update(settings: OrganizationTaxSettings)
    fun findByOrganizationId(organizationId: String): OrganizationTaxSettings?
}
