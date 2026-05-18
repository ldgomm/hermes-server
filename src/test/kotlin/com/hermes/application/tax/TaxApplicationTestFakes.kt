package com.hermes.application.tax

import com.hermes.domain.tax.*

class InMemoryTaxRateRepository : TaxRateRepository {
    private val rates = linkedMapOf<String, TaxRate>()

    override fun create(rate: TaxRate) {
        rates[rate.id] = rate
    }

    override fun update(rate: TaxRate) {
        rates[rate.id] = rate
    }

    override fun findById(id: String): TaxRate? = rates[id.trim()]

    override fun findByCode(code: String): TaxRate? =
        rates.values.firstOrNull { it.code == code.trim().lowercase() }

    override fun findActive(): List<TaxRate> =
        rates.values.filter { it.status == TaxRateStatus.ACTIVE }
}

class InMemoryTaxProfileRepository : TaxProfileRepository {
    private val profiles = linkedMapOf<String, TaxProfile>()

    override fun create(profile: TaxProfile) {
        profiles[profile.id] = profile
    }

    override fun update(profile: TaxProfile) {
        profiles[profile.id] = profile
    }

    override fun findById(id: String): TaxProfile? = profiles[id.trim()]

    override fun findByCode(code: String): TaxProfile? =
        profiles.values.firstOrNull { it.code == code.trim().lowercase() }

    override fun findActive(): List<TaxProfile> =
        profiles.values.filter { it.status == TaxProfileStatus.ACTIVE }
}

class InMemoryOrganizationTaxSettingsRepository : OrganizationTaxSettingsRepository {
    private val settings = linkedMapOf<String, OrganizationTaxSettings>()

    override fun create(settings: OrganizationTaxSettings) {
        this.settings[settings.organizationId] = settings
    }

    override fun update(settings: OrganizationTaxSettings) {
        this.settings[settings.organizationId] = settings
    }

    override fun findByOrganizationId(organizationId: String): OrganizationTaxSettings? =
        settings[organizationId.trim()]
}

class RecordingTaxAuditStore : TaxAuditLogger, TaxAuditQueryRepository {
    val events: MutableList<TaxAuditRecord> = mutableListOf()

    override fun log(event: TaxAuditEvent) {
        events += TaxAuditRecord(
            id = "audit_${events.size + 1}",
            action = event.action,
            actorUserId = event.actorUserId,
            organizationId = event.organizationId ?: "org_platform",
            targetId = event.targetId,
            before = event.before,
            after = event.after,
            reason = event.reason,
            createdAt = event.createdAt,
        )
    }

    override fun search(query: TaxAuditQuery): List<TaxAuditRecord> =
        events
            .asSequence()
            .filter { it.organizationId == query.organizationId }
            .filter { query.actions.isEmpty() || it.action in query.actions }
            .filter { query.actorUserId == null || it.actorUserId == query.actorUserId }
            .filter { query.targetId == null || it.targetId == query.targetId }
            .filter { query.from == null || !it.createdAt.isBefore(query.from) }
            .filter { query.to == null || !it.createdAt.isAfter(query.to) }
            .sortedByDescending { it.createdAt }
            .take(query.limit)
            .toList()
}

class PredictableTaxIdGenerator : TaxIdGenerator {
    private var counter: Int = 0

    override fun newId(prefix: String): String {
        counter += 1
        return "${prefix.trim().lowercase()}_$counter"
    }
}
