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
) : AdminBusinessRepository, AdminBusinessMutationRepository {
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
