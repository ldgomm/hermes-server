package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.migration.HermesMongoMigrations
import com.hermes.infrastructure.mongo.migration.MongoMigrationRunner
import com.hermes.infrastructure.mongo.testing.MongoIntegrationTestSupport
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Phase42CoreRepositoriesTest {
    @Test
    fun `core repositories insert and query phase 4 documents`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_2_repositories_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            val fixture = Phase42Fixture(database)
            fixture.insertAll()

            assertEquals(
                "Altos del Murco",
                fixture.organizations.findByTaxId("EC", fixture.taxId)?.getString("commercialName")
            )
            assertEquals(
                "Restaurante",
                fixture.activities.findByCode(fixture.organizationId, "restaurant")?.getString("name")
            )
            assertEquals(fixture.branchId, fixture.branches.findMainBranch(fixture.organizationId)?.getString("_id"))
            assertEquals(
                fixture.emissionPointId,
                fixture.emissionPoints.findByCodes(fixture.organizationId, "001", "001")?.getString("_id")
            )
            assertEquals(fixture.userId, fixture.users.findByEmail("OWNER@EXAMPLE.COM")?.getString("_id"))
            assertEquals(
                fixture.membershipId,
                fixture.memberships.findByOrganizationAndUser(fixture.organizationId, fixture.userId)?.getString("_id")
            )
            assertEquals(
                fixture.roleId,
                fixture.roles.findOrganizationRoleByCode(fixture.organizationId, "owner")?.getString("_id")
            )
            assertEquals("sales", fixture.permissions.findByCode("sales.create")?.getString("module"))

            assertEquals(
                fixture.familyId,
                fixture.platformCatalog.findFamilyByGlobalFamilyId("fam_global_cuy")?.getString("_id")
            )
            assertEquals(
                fixture.templateId,
                fixture.platformCatalog.findTemplateByGlobalCatalogId("global_cuy_entero")?.getString("_id")
            )
            assertEquals(1, fixture.platformCatalog.findTemplatesByIdentifier("7860000000001").size)

            assertEquals(
                fixture.catalogItemId,
                fixture.organizationCatalog.findByLocalSku(fixture.organizationId, "CUY-ENTERO")?.getString("_id")
            )
            assertEquals(1, fixture.organizationCatalog.findByIdentifier(fixture.organizationId, "7860000000001").size)
            assertEquals(
                1,
                fixture.organizationCatalog.findActiveByActivity(fixture.organizationId, fixture.activityId).size
            )

            assertEquals(
                fixture.catalogRequestId,
                fixture.catalogRequests.findPendingForReview().first().getString("_id")
            )

            assertEquals(fixture.taxRateId, fixture.taxRates.findBySriCodes("EC", "2", "4")?.getString("_id"))
            assertTrue(fixture.taxRates.findActiveByTaxName("EC", "IVA").isNotEmpty())
            assertEquals(
                fixture.taxProfileId,
                fixture.taxProfiles.findGlobalByCode("iva_current_full")?.getString("_id")
            )
            assertEquals(
                fixture.taxProfileId,
                fixture.taxProfiles.findByCodeWithOrganizationOverride(fixture.organizationId, "iva_current_full")
                    ?.getString("_id")
            )
            assertEquals(
                fixture.taxSettingsId,
                fixture.organizationTaxSettings.findActiveByOrganization(fixture.organizationId)?.getString("_id")
            )
        }
    }

    @Test
    fun `base document repository keeps duplicate key errors meaningful`() {
        MongoIntegrationTestSupport.assumeMongoAvailable()

        MongoIntegrationTestSupport.client().use { client ->
            val database = client.getDatabase(MongoIntegrationTestSupport.databaseName("phase_4_2_duplicate_test"))
            MongoMigrationRunner(database).migrate(HermesMongoMigrations.all)

            val fixture = Phase42Fixture(database)
            val organization = fixture.organizationDocument()
            fixture.organizations.insert(organization)

            val duplicateError = assertNotNull(
                runCatching { fixture.organizations.insert(organization) }.exceptionOrNull(),
            )
            assertTrue(duplicateError.message.orEmpty().contains("already exists"))
        }
    }
}

private class Phase42Fixture(
    database: MongoDatabase,
) {
    val organizations = OrganizationRepository(database)
    val activities = BusinessActivityRepository(database)
    val branches = BranchRepository(database)
    val emissionPoints = EmissionPointRepository(database)
    val users = UserRepository(database)
    val memberships = MembershipRepository(database)
    val roles = RoleRepository(database)
    val permissions = PermissionRepository(database)
    val platformCatalog = PlatformCatalogRepository(database)
    val organizationCatalog = OrganizationCatalogRepository(database)
    val catalogRequests = CatalogRequestRepository(database)
    val taxRates = TaxRateRepository(database)
    val taxProfiles = TaxProfileRepository(database)
    val organizationTaxSettings = OrganizationTaxSettingsRepository(database)

    val organizationId = "org_phase42test"
    val branchId = "br_phase42test"
    val emissionPointId = "emi_phase42test"
    val activityId = "act_phase42test"
    val userId = "usr_phase42test"
    val membershipId = "mem_phase42test"
    val roleId = "role_phase42test_owner"
    val permissionId = "perm_phase42test_sales_create"
    val familyId = "fam_phase42test_cuy"
    val templateId = "tpl_phase42test_cuy_entero"
    val catalogItemId = "item_phase42test_cuy_entero"
    val catalogRequestId = "catreq_phase42test_locro"
    val taxRateId = "taxr_phase42test_iva_15"
    val taxProfileId = "taxp_phase42test_iva_full"
    val taxSettingsId = "taxset_phase42test"
    val taxId = "0503638371001"

    fun insertAll() {
        organizations.insert(organizationDocument())
        activities.insert(activityDocument())
        branches.insert(branchDocument())
        emissionPoints.insert(emissionPointDocument())
        users.insert(userDocument())
        permissions.insert(permissionDocument())
        roles.insert(roleDocument())
        memberships.insert(membershipDocument())
        platformCatalog.insertFamily(platformCatalogFamilyDocument())
        platformCatalog.insertTemplate(platformCatalogTemplateDocument())
        taxRates.insert(taxRateDocument())
        taxProfiles.insert(taxProfileDocument())
        organizationTaxSettings.insert(organizationTaxSettingsDocument())
        organizationCatalog.insert(organizationCatalogItemDocument())
        catalogRequests.insert(catalogRequestDocument())
    }

    fun organizationDocument(): Document = root(organizationId)
        .append("legalName", "ALTOS DEL MURCO")
        .append("commercialName", "Altos del Murco")
        .append("taxId", taxId)
        .append("taxIdType", "ruc")
        .append("countryCode", "EC")
        .append("timezone", "America/Guayaquil")
        .append("defaultCurrency", "USD")
        .append("taxRegime", "rimpe_entrepreneur")
        .append("businessModel", "multi_activity")
        .append("primaryBusinessType", "restaurant")
        .append("status", "active")
        .append("contact", Document("email", "owner@example.com"))
        .append("branding", Document())

    private fun activityDocument(): Document = root(activityId, organizationId)
        .append("code", "restaurant")
        .append("name", "Restaurante")
        .append("description", "Actividad de restaurante")
        .append("activityType", "restaurant")
        .append("workflowMode", "order")
        .append("status", "active")
        .append("requiresScheduling", false)
        .append("tracksInventory", true)
        .append("allowsReceivables", true)
        .append("sortOrder", 1)
        .append("publicDiscovery", Document("status", "private"))
        .append("assistedCommerce", Document("enabled", false))

    private fun branchDocument(): Document = root(branchId, organizationId)
        .append("name", "Matriz")
        .append("code", "001")
        .append("type", "main")
        .append("status", "active")
        .append(
            "location",
            Document("coordinates", Document("type", "Point").append("coordinates", listOf(-78.55, -0.40)))
        )
        .append("contact", Document())
        .append("businessHoursId", null)
        .append("publicDiscovery", Document("allowBranchDiscovery", false))

    private fun emissionPointDocument(): Document = root(emissionPointId, organizationId)
        .append("branchId", branchId)
        .append("establishmentCode", "001")
        .append("emissionPointCode", "001")
        .append("displayName", "Matriz Caja 1")
        .append("status", "active")
        .append("documentSequences", Document("electronic_invoice", Document("current", 1).append("padding", 9)))

    private fun userDocument(): Document = root(userId)
        .append("email", "owner@example.com")
        .append("phone", null)
        .append("displayName", "Owner")
        .append("status", "active")
        .append("auth", Document("mustChangePassword", false))
        .append("profile", Document("preferredLanguage", "es-EC"))

    private fun permissionDocument(): Document = root(permissionId)
        .append("code", "sales.create")
        .append("name", "Crear ventas")
        .append("module", "sales")
        .append("status", "active")
        .append("description", "Permite crear ventas")

    private fun roleDocument(): Document = root(roleId)
        .append("organizationId", organizationId)
        .append("code", "owner")
        .append("name", "Dueño")
        .append("scope", "organization")
        .append("status", "active")
        .append("permissions", listOf("sales.create"))

    private fun membershipDocument(): Document = root(membershipId, organizationId)
        .append("userId", userId)
        .append("roleId", roleId)
        .append("status", "active")
        .append("effectivePermissions", listOf("sales.create"))
        .append("invitedBy", null)
        .append("joinedAt", Date.from(Instant.now()))

    private fun platformCatalogFamilyDocument(): Document = root(familyId)
        .append("globalFamilyId", "fam_global_cuy")
        .append("canonicalName", "Cuy")
        .append("normalizedName", "cuy")
        .append("categoryCode", "food.main")
        .append("itemType", "product")
        .append("status", "active")
        .append("searchKeywords", listOf("cuy"))
        .append("semanticTags", listOf("restaurant"))

    private fun platformCatalogTemplateDocument(): Document = root(templateId)
        .append("globalCatalogId", "global_cuy_entero")
        .append("productFamilyId", familyId)
        .append("canonicalName", "Cuy entero")
        .append("normalizedName", "cuy entero")
        .append("brand", null)
        .append("categoryCode", "food.main")
        .append("itemType", "product")
        .append("status", "published")
        .append("identifiers", listOf(identifier("barcode", "7860000000001")))
        .append("variantAttributes", Document("portion", "entero"))
        .append("attributes", Document("category", "plato fuerte"))
        .append("media", emptyList<Document>())
        .append("searchKeywords", listOf("cuy", "entero"))
        .append("semanticTags", listOf("restaurant"))

    private fun taxRateDocument(): Document = root(taxRateId)
        .append("countryCode", "EC")
        .append("taxName", "IVA")
        .append("ratePercent", Decimal128(BigDecimal("15.0000")))
        .append("sriTaxCode", "2")
        .append("sriRateCode", "4")
        .append("status", "active")
        .append("effectiveFrom", Date.from(Instant.parse("2024-04-01T00:00:00Z")))
        .append("effectiveTo", null)
        .append("legalReference", "Configuración de prueba")
        .append("source", "migration_seed")

    private fun taxProfileDocument(): Document = root(taxProfileId)
        .append("code", "iva_current_full")
        .append("name", "IVA tarifa vigente")
        .append("taxName", "IVA")
        .append("taxRateId", taxRateId)
        .append("ratePercent", Decimal128(BigDecimal("15.0000")))
        .append("sriTaxCode", "2")
        .append("sriRateCode", "4")
        .append("profileType", "iva_current_full")
        .append("status", "active")
        .append("effectiveFrom", Date.from(Instant.parse("2024-04-01T00:00:00Z")))
        .append("effectiveTo", null)
        .append("snapshotPolicy", "required_on_sale")

    private fun organizationTaxSettingsDocument(): Document = root(taxSettingsId, organizationId)
        .append("taxRegime", "rimpe_entrepreneur")
        .append("taxMode", "mixed_by_item")
        .append("status", "active")
        .append("activeFrom", Date.from(Instant.parse("2026-01-01T00:00:00Z")))
        .append("activeTo", null)
        .append("defaultTaxProfileId", taxProfileId)
        .append("requiresAccounting", false)
        .append("source", "migration")

    private fun organizationCatalogItemDocument(): Document = root(catalogItemId, organizationId)
        .append("globalCatalogId", "global_cuy_entero")
        .append("templateId", templateId)
        .append("productFamilyId", familyId)
        .append("branchId", branchId)
        .append("activityId", activityId)
        .append("localName", "Cuy entero")
        .append("normalizedName", "cuy entero")
        .append("description", "Cuy entero asado")
        .append("itemType", "product")
        .append("status", "active")
        .append("localSku", "CUY-ENTERO")
        .append("identifiers", listOf(identifier("barcode", "7860000000001")))
        .append("price", money("24.00"))
        .append("taxProfileId", taxProfileId)
        .append("attributes", Document("category", "plato fuerte"))
        .append("variantAttributes", Document("portion", "entero"))
        .append("media", emptyList<Document>())
        .append("searchableText", "cuy entero plato fuerte restaurante")
        .append("publicDiscovery", Document("status", "private"))
        .append("inventoryPolicy", Document("tracksStock", true))

    private fun catalogRequestDocument(): Document = root(catalogRequestId, organizationId)
        .append("requestedName", "Yahuarlocro")
        .append("normalizedName", "yahuarlocro")
        .append("itemType", "product")
        .append("activityId", activityId)
        .append("status", "submitted")
        .append("requestedBy", userId)
        .append("reviewedBy", null)
        .append("reviewedAt", null)
        .append("resultTemplateId", null)
        .append("notes", "Solicitud de prueba")
        .append("payload", Document("source", "test"))

    private fun root(id: String, organizationId: String? = null): Document {
        val now = Date.from(Instant.now())
        val document = Document("_id", id)
            .append("createdAt", now)
            .append("createdBy", "usr_test")
            .append("updatedAt", now)
            .append("updatedBy", "usr_test")
            .append("version", 1)
            .append("schemaVersion", 1)

        if (organizationId != null) {
            document.append("organizationId", organizationId)
        }

        return document
    }

    private fun money(amount: String): Document = Document("amount", Decimal128(BigDecimal(amount)))
        .append("currency", "USD")

    private fun identifier(
        type: String,
        normalizedValue: String,
    ): Document = Document("type", type)
        .append("value", normalizedValue)
        .append("normalizedValue", normalizedValue)
        .append("scope", "global")
        .append("status", "active")
        .append("source", "test")
        .append("isPrimary", true)
}
