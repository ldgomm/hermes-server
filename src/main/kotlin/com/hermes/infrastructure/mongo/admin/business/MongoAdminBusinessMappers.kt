package com.hermes.infrastructure.mongo.admin.business

import com.hermes.application.admin.business.*
import org.bson.Document
import java.time.Instant
import java.util.*

internal object MongoAdminBusinessMappers {
    fun businessFromDocument(document: Document): AdminBusinessProfile = AdminBusinessProfile(
        id = document.requiredString("_id"),
        countryCode = document.getString("countryCode") ?: "EC",
        taxId = document.getString("taxId").orEmpty(),
        legalName = document.getString("legalName").orEmpty(),
        commercialName = document.getString("commercialName").orEmpty(),
        status = document.getString("status") ?: "unknown",
        ownerUserId = document.getString("ownerUserId").orEmpty(),
        defaultCurrency = document.getString("defaultCurrency"),
        timezone = document.getString("timezone"),
        createdAt = document.instantOrNull("createdAt"),
        updatedAt = document.instantOrNull("updatedAt"),
        version = document.longFlexible("version") ?: 1L,
    )

    fun activityFromDocument(document: Document): AdminBusinessActivitySummary = AdminBusinessActivitySummary(
        id = document.requiredString("_id"),
        organizationId = document.requiredString("organizationId"),
        code = document.getString("code"),
        name = document.getString("name").orEmpty(),
        description = document.getString("description"),
        activityType = document.getString("activityType") ?: document.getString("type") ?: "custom",
        workflowMode = document.getString("workflowMode") ?: firstWorkflowMode(document) ?: "quick_sale",
        status = document.getString("status") ?: "draft",
        requiresScheduling = document.getBoolean("requiresScheduling") ?: false,
        tracksInventory = document.getBoolean("tracksInventory") ?: document.getBoolean("affectsInventory") ?: false,
        allowsReceivables = document.getBoolean("allowsReceivables") ?: true,
        sortOrder = document.getInteger("sortOrder") ?: 0,
        createdAt = document.instantOrNull("createdAt"),
        updatedAt = document.instantOrNull("updatedAt"),
    )

    fun branchFromDocument(document: Document): AdminBusinessBranchSummary = AdminBusinessBranchSummary(
        id = document.requiredString("_id"),
        organizationId = document.requiredString("organizationId"),
        code = document.getString("code"),
        name = document.getString("name").orEmpty(),
        type = document.getString("type") ?: "branch",
        status = document.getString("status") ?: "inactive",
        location = locationFromDocument(document.get("location", Document::class.java)),
        businessHoursId = document.getString("businessHoursId"),
        createdAt = document.instantOrNull("createdAt"),
        updatedAt = document.instantOrNull("updatedAt"),
    )

    fun emissionPointFromDocument(document: Document): AdminBusinessEmissionPointSummary =
        AdminBusinessEmissionPointSummary(
            id = document.requiredString("_id"),
            organizationId = document.requiredString("organizationId"),
            branchId = document.getString("branchId").orEmpty(),
            establishmentCode = document.getString("establishmentCode").orEmpty(),
            emissionPointCode = document.getString("emissionPointCode").orEmpty(),
            displayName = document.getString("displayName").orEmpty(),
            status = document.getString("status") ?: "inactive",
            createdAt = document.instantOrNull("createdAt"),
            updatedAt = document.instantOrNull("updatedAt"),
        )

    fun roleIdsFromMembership(document: Document): Set<String> {
        val roleIds = document.getList("roleIds", String::class.java).orEmpty()
        val legacyRoleId = document.getString("roleId")
        return (roleIds + listOfNotNull(legacyRoleId)).filter { it.isNotBlank() }.toSet()
    }

    private fun firstWorkflowMode(document: Document): String? =
        document.getList("workflowModes", String::class.java).orEmpty().firstOrNull()

    private fun locationFromDocument(location: Document?): AdminBranchLocation? {
        if (location == null || location.isEmpty()) return null
        val coordinates = location.get("coordinates", Document::class.java)
        val values = coordinates?.getList("coordinates", Number::class.java).orEmpty()
        val longitude = values.getOrNull(0)?.toDouble()
        val latitude = values.getOrNull(1)?.toDouble()

        return AdminBranchLocation(
            countryCode = location.getString("countryCode"),
            province = location.getString("province"),
            city = location.getString("city"),
            sector = location.getString("sector"),
            addressLine = location.getString("addressLine"),
            latitude = latitude,
            longitude = longitude,
            privacyMode = location.getString("privacyMode"),
        )
    }
}

internal fun Document.requiredString(field: String): String = getString(field)
    ?: throw IllegalStateException("Mongo document ${getString("_id") ?: "<unknown>"} requires '$field'.")

internal fun Document.instantOrNull(field: String): Instant? = when (val raw = this[field]) {
    is Date -> raw.toInstant()
    is Instant -> raw
    is String -> runCatching { Instant.parse(raw) }.getOrNull()
    else -> null
}

internal fun Document.longFlexible(field: String): Long? = when (val raw = this[field]) {
    is Long -> raw
    is Int -> raw.toLong()
    is Number -> raw.toLong()
    is String -> raw.toLongOrNull()
    else -> null
}

internal fun String?.normalizedDbToken(): String = this?.trim()?.lowercase().orEmpty()
