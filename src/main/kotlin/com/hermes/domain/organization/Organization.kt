package com.hermes.domain.organization

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

data class Organization(
    val id: String,
    val countryCode: String,
    val taxId: String,
    val legalName: String,
    val commercialName: String,
    val status: OrganizationStatus,
    val ownerUserId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 1,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Organization id cannot be blank.")
        if (countryCode.isBlank()) throw DomainRuleViolation("Organization country code cannot be blank.")
        if (countryCode != countryCode.uppercase()) {
            throw DomainRuleViolation("Organization country code must be uppercase.")
        }
        if (taxId.isBlank()) throw DomainRuleViolation("Organization tax id cannot be blank.")
        if (legalName.isBlank()) throw DomainRuleViolation("Organization legal name cannot be blank.")
        if (commercialName.isBlank()) throw DomainRuleViolation("Organization commercial name cannot be blank.")
        if (ownerUserId.isBlank()) throw DomainRuleViolation("Organization owner user id cannot be blank.")
        if (version < 1) throw DomainRuleViolation("Organization version must be greater than zero.")
    }

    fun assertCanOperate() {
        when (status) {
            OrganizationStatus.ACTIVE -> Unit
            OrganizationStatus.DRAFT -> throw DomainRuleViolation("Draft organization cannot operate.")
            OrganizationStatus.SUSPENDED -> throw DomainRuleViolation("Suspended organization cannot operate.")
            OrganizationStatus.BLOCKED -> throw DomainRuleViolation("Blocked organization cannot operate.")
            OrganizationStatus.ARCHIVED -> throw DomainRuleViolation("Archived organization cannot operate.")
        }
    }

    companion object {
        fun create(
            id: String,
            countryCode: String,
            taxId: String,
            legalName: String,
            commercialName: String,
            ownerUserId: String,
            now: Instant,
            status: OrganizationStatus = OrganizationStatus.ACTIVE,
        ): Organization = Organization(
            id = id,
            countryCode = countryCode.trim().uppercase(),
            taxId = taxId.trim(),
            legalName = legalName.trim(),
            commercialName = commercialName.trim(),
            status = status,
            ownerUserId = ownerUserId,
            createdAt = now,
            updatedAt = now,
        )
    }
}
