package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.OrganizationSriSettings

interface OrganizationSriSettingsRepository {
    fun findByOrganizationId(organizationId: String): OrganizationSriSettings?
    fun save(settings: OrganizationSriSettings): OrganizationSriSettings
}
