package com.hermes.infrastructure.mongo.admin.business

import com.hermes.application.admin.business.AdminActivityCreateDraft
import com.hermes.application.admin.business.AdminActivityMutationRepository
import com.hermes.application.admin.business.AdminActivityStatusPatch
import com.hermes.application.admin.business.AdminActivityUpdatePatch
import com.hermes.application.admin.business.AdminBusinessActivitySummary
import com.hermes.application.admin.business.AdminBusinessBranchSummary
import com.hermes.application.admin.business.AdminBusinessEmissionPointSummary
import com.hermes.application.admin.business.AdminBusinessMutationRepository
import com.hermes.application.admin.business.AdminBusinessProfile
import com.hermes.application.admin.business.AdminBusinessRepository
import com.hermes.application.admin.business.AdminBusinessUpdatePatch
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.electronicinvoicing.ElectronicInvoicingMongoCollectionNames
import com.mongodb.client.FindIterable
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Updates.set
import org.bson.Document
import java.util.Date

class MongoAdminBusinessRepository(
    database: MongoDatabase,
) : AdminBusinessRepository, AdminBusinessMutationRepository, AdminActivityMutationRepository {
    private val organizations: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ORGANIZATIONS)
    private val activities: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ORGANIZATION_ACTIVITIES)
    private val branches: MongoCollection<Document> = database.getCollection(MongoCollectionNames.BRANCHES)
    private val emissionPoints: MongoCollection<Document> = database.getCollection(MongoCollectionNames.EMISSION_POINTS)
    private val taxSettings: MongoCollection<Document> = database.getCollection(MongoCollectionNames.ORGANIZATION_TAX_SETTINGS)
    private val sriSettings: MongoCollection<Document> = database.getCollection(ElectronicInvoicingMongoCollectionNames.ORGANIZATION_SRI_SETTINGS)
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

private fun <T> FindIterable<T>.firstOrNull(): T? = first()
