package com.hermes.infrastructure.mongo.admin.business

import com.hermes.application.admin.business.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicInvoicingMongoCollectionNames
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates.*
import org.bson.Document
import java.util.*

class MongoAdminBusinessRepository(
    database: MongoDatabase,
) : AdminBusinessRepository,
    AdminBusinessMutationRepository,
    AdminActivityMutationRepository,
    AdminBranchMutationRepository,
    AdminEmissionPointMutationRepository {

    private val organizations: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ORGANIZATIONS)
    private val activities: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ORGANIZATION_ACTIVITIES)
    private val branches: MongoCollection<Document> = database.getCollection(MongoCollectionNames.BRANCHES)
    private val emissionPoints: MongoCollection<Document> = database.getCollection(MongoCollectionNames.EMISSION_POINTS)
    private val taxSettings: MongoCollection<Document> =
        database.getCollection(MongoCollectionNames.ORGANIZATION_TAX_SETTINGS)
    private val sriSettings: MongoCollection<Document> =
        database.getCollection(ElectronicInvoicingMongoCollectionNames.ORGANIZATION_SRI_SETTINGS)
    private val memberships: MongoCollection<Document> = database.getCollection(MongoCollectionNames.MEMBERSHIPS)
    private val roles: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ROLES)

    override fun findBusiness(organizationId: String): AdminBusinessProfile? = organizations
        .find(eq("_id", organizationId.trim()))
        .firstOrNull()
        ?.let(MongoAdminBusinessMappers::businessFromDocument)

    override fun listActivities(organizationId: String): List<AdminBusinessActivitySummary> = activities
        .find(and(eq("organizationId", organizationId.trim()), ne("status", "archived")))
        .sort(Sorts.ascending("sortOrder", "name"))
        .into(mutableListOf())
        .map(MongoAdminBusinessMappers::activityFromDocument)

    override fun listBranches(organizationId: String): List<AdminBusinessBranchSummary> = branches
        .find(and(eq("organizationId", organizationId.trim()), ne("status", "archived")))
        .sort(Sorts.ascending("type", "name"))
        .into(mutableListOf())
        .map(MongoAdminBusinessMappers::branchFromDocument)

    override fun listEmissionPoints(organizationId: String): List<AdminBusinessEmissionPointSummary> = emissionPoints
        .find(and(eq("organizationId", organizationId.trim()), ne("status", "archived")))
        .sort(Sorts.ascending("branchId", "establishmentCode", "emissionPointCode"))
        .into(mutableListOf())
        .map(MongoAdminBusinessMappers::emissionPointFromDocument)

    override fun hasTaxSettings(organizationId: String): Boolean =
        taxSettings.countDocuments(eq("organizationId", organizationId.trim())) > 0

    override fun hasSriSettings(organizationId: String): Boolean =
        sriSettings.countDocuments(eq("organizationId", organizationId.trim())) > 0

    override fun hasActiveOwnerOrAdminMembership(organizationId: String): Boolean {
        val activeMemberships = memberships
            .find(eq("organizationId", organizationId.trim()))
            .into(mutableListOf())
            .filter { it.getString("status").normalizedDbToken() == "active" }

        if (activeMemberships.isEmpty()) return false

        val roleIds = activeMemberships.flatMap { MongoAdminBusinessMappers.roleIdsFromMembership(it) }.toSet()
        if (roleIds.isEmpty()) return false

        return roles
            .find(com.mongodb.client.model.Filters.`in`("_id", roleIds))
            .into(mutableListOf())
            .any { role -> role.isOwnerOrAdminRole() }
    }

    override fun existsBusinessWithTaxId(
        countryCode: String,
        taxId: String,
        excludeOrganizationId: String,
    ): Boolean = organizations.countDocuments(
        and(
            eq("countryCode", countryCode.trim().uppercase()),
            eq("taxId", taxId.trim()),
            ne("_id", excludeOrganizationId.trim()),
        )
    ) > 0

    override fun updateBusiness(patch: AdminBusinessUpdatePatch): AdminBusinessProfile {
        val sets = buildList {
            patch.countryCode?.let { add(set("countryCode", it)) }
            patch.taxId?.let { add(set("taxId", it)) }
            patch.legalName?.let { add(set("legalName", it)) }
            patch.commercialName?.let { add(set("commercialName", it)) }
            patch.defaultCurrency?.let { add(set("defaultCurrency", it)) }
            patch.timezone?.let { add(set("timezone", it)) }
            add(set("updatedAt", Date.from(patch.updatedAt)))
            add(set("updatedBy", patch.updatedBy))
            add(inc("version", 1L))
        }

        val updated = organizations.findOneAndUpdate(
            eq("_id", patch.organizationId.trim()),
            combine(sets),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Organization does not exist.")

        return MongoAdminBusinessMappers.businessFromDocument(updated)
    }

    override fun findActivity(organizationId: String, activityId: String): AdminBusinessActivitySummary? = activities
        .find(and(eq("organizationId", organizationId.trim()), eq("_id", activityId.trim())))
        .firstOrNull()
        ?.let(MongoAdminBusinessMappers::activityFromDocument)

    override fun existsActivityCode(
        organizationId: String,
        code: String,
        excludeActivityId: String?,
    ): Boolean {
        val filters = buildList {
            add(eq("organizationId", organizationId.trim()))
            add(eq("code", code.trim()))
            if (!excludeActivityId.isNullOrBlank()) add(ne("_id", excludeActivityId.trim()))
        }
        return activities.countDocuments(and(filters)) > 0
    }

    override fun createActivity(draft: AdminActivityCreateDraft): AdminBusinessActivitySummary {
        val now = Date.from(draft.createdAt)
        val document = Document("_id", draft.id)
            .append("organizationId", draft.organizationId)
            .append("code", draft.code)
            .append("name", draft.name)
            .append("description", draft.description)
            .append("activityType", draft.activityType)
            .append("workflowMode", draft.workflowMode)
            .append("status", draft.status)
            .append("requiresScheduling", draft.requiresScheduling)
            .append("tracksInventory", draft.tracksInventory)
            .append("allowsReceivables", draft.allowsReceivables)
            .append("sortOrder", draft.sortOrder)
            .append("publicDiscovery", Document())
            .append("assistedCommerce", Document())
            .append("createdAt", now)
            .append("createdBy", draft.createdBy)
            .append("updatedAt", now)
            .append("updatedBy", draft.createdBy)
            .append("version", 1L)
            .append("schemaVersion", 1)

        activities.insertOne(document)
        return MongoAdminBusinessMappers.activityFromDocument(document)
    }

    override fun updateActivity(patch: AdminActivityUpdatePatch): AdminBusinessActivitySummary {
        val sets = buildList {
            patch.code?.let { add(set("code", it)) }
            patch.name?.let { add(set("name", it)) }
            if (patch.changeDescription) add(set("description", patch.description))
            patch.activityType?.let { add(set("activityType", it)) }
            patch.workflowMode?.let { add(set("workflowMode", it)) }
            patch.requiresScheduling?.let { add(set("requiresScheduling", it)) }
            patch.tracksInventory?.let { add(set("tracksInventory", it)) }
            patch.allowsReceivables?.let { add(set("allowsReceivables", it)) }
            patch.sortOrder?.let { add(set("sortOrder", it)) }
            add(set("updatedAt", Date.from(patch.updatedAt)))
            add(set("updatedBy", patch.updatedBy))
            add(inc("version", 1L))
        }

        val updated = activities.findOneAndUpdate(
            and(eq("organizationId", patch.organizationId.trim()), eq("_id", patch.activityId.trim())),
            combine(sets),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Business activity does not exist.")

        return MongoAdminBusinessMappers.activityFromDocument(updated)
    }

    override fun updateActivityStatus(patch: AdminActivityStatusPatch): AdminBusinessActivitySummary {
        val updated = activities.findOneAndUpdate(
            and(eq("organizationId", patch.organizationId.trim()), eq("_id", patch.activityId.trim())),
            combine(
                set("status", patch.status),
                set("updatedAt", Date.from(patch.updatedAt)),
                set("updatedBy", patch.updatedBy),
                inc("version", 1L),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Business activity does not exist.")

        return MongoAdminBusinessMappers.activityFromDocument(updated)
    }

    override fun findBranch(organizationId: String, branchId: String): AdminBusinessBranchSummary? = branches
        .find(and(eq("organizationId", organizationId.trim()), eq("_id", branchId.trim())))
        .firstOrNull()
        ?.let(MongoAdminBusinessMappers::branchFromDocument)

    override fun existsBranchCode(
        organizationId: String,
        code: String,
        excludeBranchId: String?,
    ): Boolean {
        val filters = buildList {
            add(eq("organizationId", organizationId.trim()))
            add(eq("code", code.trim()))
            if (!excludeBranchId.isNullOrBlank()) add(ne("_id", excludeBranchId.trim()))
        }
        return branches.countDocuments(and(filters)) > 0
    }

    override fun hasActiveMainBranch(organizationId: String, excludeBranchId: String?): Boolean {
        val filters = buildList {
            add(eq("organizationId", organizationId.trim()))
            add(eq("type", "main"))
            add(eq("status", "active"))
            if (!excludeBranchId.isNullOrBlank()) add(ne("_id", excludeBranchId.trim()))
        }
        return branches.countDocuments(and(filters)) > 0
    }

    override fun countActiveBranches(organizationId: String, excludeBranchId: String?): Int {
        val filters = buildList {
            add(eq("organizationId", organizationId.trim()))
            add(eq("status", "active"))
            if (!excludeBranchId.isNullOrBlank()) add(ne("_id", excludeBranchId.trim()))
        }
        return branches.countDocuments(and(filters)).toInt()
    }

    override fun hasActiveEmissionPoints(organizationId: String, branchId: String): Boolean =
        emissionPoints.countDocuments(
            and(
                eq("organizationId", organizationId.trim()),
                eq("branchId", branchId.trim()),
                eq("status", "active"),
            )
        ) > 0

    override fun createBranch(draft: AdminBranchCreateDraft): AdminBusinessBranchSummary {
        val now = Date.from(draft.createdAt)
        val document = Document("_id", draft.id)
            .append("organizationId", draft.organizationId)
            .append("code", draft.code)
            .append("name", draft.name)
            .append("type", draft.type)
            .append("status", draft.status)
            .append("location", draft.location.toLocationDocumentOrEmpty())
            .append("contact", Document())
            .append("businessHoursId", draft.businessHoursId)
            .append("publicDiscovery", Document("visible", false).append("status", "private"))
            .append("createdAt", now)
            .append("createdBy", draft.createdBy)
            .append("updatedAt", now)
            .append("updatedBy", draft.createdBy)
            .append("version", 1L)
            .append("schemaVersion", 2)

        branches.insertOne(document)
        return MongoAdminBusinessMappers.branchFromDocument(document)
    }

    override fun updateBranch(patch: AdminBranchUpdatePatch): AdminBusinessBranchSummary {
        val sets = buildList {
            patch.code?.let { add(set("code", it)) }
            patch.name?.let { add(set("name", it)) }
            patch.type?.let { add(set("type", it)) }
            if (patch.changeLocation) add(set("location", patch.location.toLocationDocumentOrEmpty()))
            if (patch.changeBusinessHoursId) add(set("businessHoursId", patch.businessHoursId))
            add(set("updatedAt", Date.from(patch.updatedAt)))
            add(set("updatedBy", patch.updatedBy))
            add(inc("version", 1L))
        }

        val updated = branches.findOneAndUpdate(
            and(eq("organizationId", patch.organizationId.trim()), eq("_id", patch.branchId.trim())),
            combine(sets),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Branch does not exist.")

        return MongoAdminBusinessMappers.branchFromDocument(updated)
    }

    override fun updateBranchStatus(patch: AdminBranchStatusPatch): AdminBusinessBranchSummary {
        val updated = branches.findOneAndUpdate(
            and(eq("organizationId", patch.organizationId.trim()), eq("_id", patch.branchId.trim())),
            combine(
                set("status", patch.status),
                set("updatedAt", Date.from(patch.updatedAt)),
                set("updatedBy", patch.updatedBy),
                inc("version", 1L),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Branch does not exist.")

        return MongoAdminBusinessMappers.branchFromDocument(updated)
    }


    override fun findEmissionPoint(
        organizationId: String,
        emissionPointId: String,
    ): AdminBusinessEmissionPointSummary? = emissionPoints
        .find(and(eq("organizationId", organizationId.trim()), eq("_id", emissionPointId.trim())))
        .firstOrNull()
        ?.let(MongoAdminBusinessMappers::emissionPointFromDocument)

    override fun existsEmissionPointCodes(
        organizationId: String,
        establishmentCode: String,
        emissionPointCode: String,
        excludeEmissionPointId: String?,
    ): Boolean {
        val filters = buildList {
            add(eq("organizationId", organizationId.trim()))
            add(eq("establishmentCode", establishmentCode.trim()))
            add(eq("emissionPointCode", emissionPointCode.trim()))
            if (!excludeEmissionPointId.isNullOrBlank()) add(ne("_id", excludeEmissionPointId.trim()))
        }
        return emissionPoints.countDocuments(and(filters)) > 0
    }

    override fun createEmissionPoint(draft: AdminEmissionPointCreateDraft): AdminBusinessEmissionPointSummary {
        val now = Date.from(draft.createdAt)
        val document = Document("_id", draft.id)
            .append("organizationId", draft.organizationId)
            .append("branchId", draft.branchId)
            .append("establishmentCode", draft.establishmentCode)
            .append("emissionPointCode", draft.emissionPointCode)
            .append("displayName", draft.displayName)
            .append("status", draft.status)
            .append("documentSequences", Document())
            .append("createdAt", now)
            .append("createdBy", draft.createdBy)
            .append("updatedAt", now)
            .append("updatedBy", draft.createdBy)
            .append("version", 1L)
            .append("schemaVersion", 1)

        emissionPoints.insertOne(document)
        return MongoAdminBusinessMappers.emissionPointFromDocument(document)
    }

    override fun updateEmissionPoint(patch: AdminEmissionPointUpdatePatch): AdminBusinessEmissionPointSummary {
        val sets = buildList {
            patch.branchId?.let { add(set("branchId", it)) }
            patch.establishmentCode?.let { add(set("establishmentCode", it)) }
            patch.emissionPointCode?.let { add(set("emissionPointCode", it)) }
            patch.displayName?.let { add(set("displayName", it)) }
            add(set("updatedAt", Date.from(patch.updatedAt)))
            add(set("updatedBy", patch.updatedBy))
            add(inc("version", 1L))
        }

        val updated = emissionPoints.findOneAndUpdate(
            and(eq("organizationId", patch.organizationId.trim()), eq("_id", patch.emissionPointId.trim())),
            combine(sets),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Emission point does not exist.")

        return MongoAdminBusinessMappers.emissionPointFromDocument(updated)
    }

    override fun updateEmissionPointStatus(patch: AdminEmissionPointStatusPatch): AdminBusinessEmissionPointSummary {
        val updated = emissionPoints.findOneAndUpdate(
            and(eq("organizationId", patch.organizationId.trim()), eq("_id", patch.emissionPointId.trim())),
            combine(
                set("status", patch.status),
                set("updatedAt", Date.from(patch.updatedAt)),
                set("updatedBy", patch.updatedBy),
                inc("version", 1L),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        ) ?: throw DomainRuleViolation("Emission point does not exist.")

        return MongoAdminBusinessMappers.emissionPointFromDocument(updated)
    }

    private fun Document.isOwnerOrAdminRole(): Boolean {
        val code = getString("code").normalizedDbToken()
        val permissions = getList("permissionKeys", String::class.java).orEmpty() +
                getList("permissions", String::class.java).orEmpty()

        return code in setOf(
            SystemRoleCode.ORGANIZATION_OWNER.code,
            SystemRoleCode.ORGANIZATION_ADMIN.code,
        ) || PermissionCatalog.ALL in permissions || PermissionCatalog.ORGANIZATION_UPDATE in permissions
    }
}

private fun AdminBranchLocation?.toLocationDocumentOrEmpty(): Document {
    if (this == null) return Document()
    val document = Document()
        .append("countryCode", countryCode)
        .append("province", province)
        .append("city", city)
        .append("sector", sector)
        .append("addressLine", addressLine)
        .append("privacyMode", privacyMode)

    if (latitude != null && longitude != null) {
        document.append(
            "coordinates",
            Document("type", "Point").append("coordinates", listOf(longitude, latitude)),
        )
    }
    return document
}

private fun <T> FindIterable<T>.firstOrNull(): T? = first()
