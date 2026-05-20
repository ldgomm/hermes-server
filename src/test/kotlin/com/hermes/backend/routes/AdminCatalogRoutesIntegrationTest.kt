package com.hermes.backend.routes

import com.hermes.application.admin.catalog.ChangeAdminCatalogLocalItemStatusUseCase
import com.hermes.application.admin.catalog.GetAdminCatalogRequestUseCase
import com.hermes.application.admin.catalog.SearchAdminCatalogMasterTemplatesUseCase
import com.hermes.application.auth.*
import com.hermes.application.catalog.*
import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.backend.admin.catalog.AdminCatalogModule
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.domain.catalog.*
import com.hermes.domain.money.Money
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.session.UserSession
import com.hermes.domain.tax.*
import com.hermes.domain.user.User
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminCatalogRoutesIntegrationTest {
    @Test
    fun `admin catalog routes expose master local and request flows`() = testApplication {
        val fixture = fixture()

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminCatalogRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    adminCatalogModule = fixture.adminCatalogModule,
                )
            }
        }

        val master = client.get("/api/v1/admin/catalog/master/templates?q=cola") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val masterBody = master.bodyAsText()
        assertEquals(HttpStatusCode.OK, master.status, masterBody)
        assertTrue(masterBody.contains("Coca Cola 500ml"), masterBody)

        val detail = client.get("/api/v1/admin/catalog/master/templates/tpl_1") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val detailBody = detail.bodyAsText()
        assertEquals(HttpStatusCode.OK, detail.status, detailBody)
        assertTrue(detailBody.contains("coca_500"), detailBody)

        val local = client.get("/api/v1/admin/catalog/local/items") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val localBody = local.bodyAsText()
        assertEquals(HttpStatusCode.OK, local.status, localBody)
        assertTrue(localBody.contains("Cola local"), localBody)

        val activate = client.post("/api/v1/admin/catalog/local/items/ocat_1/activate") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody("""{"reason":"Disponible otra vez"}""")
        }
        val activateBody = activate.bodyAsText()
        assertEquals(HttpStatusCode.OK, activate.status, activateBody)
        assertTrue(activateBody.contains("\"status\":\"ACTIVE\""), activateBody)

        val createdRequest = client.post("/api/v1/admin/catalog/requests") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "requestedName":"Mote con chicharrón",
                  "requestedType":"PRODUCT",
                  "description":"Nuevo plato para catálogo local"
                }
                """.trimIndent(),
            )
        }
        val createdRequestBody = createdRequest.bodyAsText()
        assertEquals(HttpStatusCode.Created, createdRequest.status, createdRequestBody)
        assertTrue(createdRequestBody.contains("Mote con chicharrón"), createdRequestBody)

        val requests = client.get("/api/v1/admin/catalog/requests") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }
        val requestsBody = requests.bodyAsText()
        assertEquals(HttpStatusCode.OK, requests.status, requestsBody)
        assertTrue(requestsBody.contains("creq_"), requestsBody)
    }

    @Test
    fun `admin catalog rejects missing catalog permission`() = testApplication {
        val fixture = fixture(permissions = setOf(PermissionCatalog.SALES_VIEW))

        application {
            configureSerialization()
            configureStatusPages()
            routing {
                adminCatalogRoutes(
                    authenticateRequestUseCase = fixture.authenticateRequestUseCase,
                    activeOrganizationResolverUseCase = fixture.activeOrganizationResolverUseCase,
                    effectivePermissionResolverUseCase = fixture.effectivePermissionResolverUseCase,
                    adminCatalogModule = fixture.adminCatalogModule,
                )
            }
        }

        val response = client.get("/api/v1/admin/catalog/local/items") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header("X-Organization-Id", "org_1")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    private fun fixture(
        permissions: Set<String> = setOf(PermissionCatalog.ALL),
    ): Fixture {
        val now = Instant.parse("2026-05-20T00:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val authRepository = FakeAuthContextRepository()
        val user = User.createOwner(
            id = "usr_1",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )
        val organization = Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1790000000001",
            legalName = "Hermes Demo S.A.",
            commercialName = "Hermes Demo",
            ownerUserId = user.id,
            now = now,
        )
        val role = RoleDefinition(
            id = "role_admin_catalog_test",
            code = "admin_catalog_test",
            organizationId = organization.id,
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Admin catalog test",
            description = "Custom test role for Admin Catalog routes",
            permissionKeys = permissions,
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        )
        val membership = OrganizationMembership.owner(
            id = "mem_1",
            organizationId = organization.id,
            userId = user.id,
            ownerRoleId = role.id,
            now = now,
        )
        val session = UserSession.create(
            id = "ses_1",
            userId = user.id,
            now = now,
            expiresAt = now.plusSeconds(3600),
        )
        authRepository.users[user.id] = user
        authRepository.organizations[organization.id] = organization
        authRepository.roles[role.id] = role
        authRepository.memberships[membership.id] = membership
        authRepository.sessions[session.id] = session

        val jwt = HmacJwtTokenService(
            secret = "test-jwt-secret-for-hermes-admin-catalog-32chars-minimum",
            accessTokenTtlSeconds = 3600,
        )
        val authenticate = AuthenticateRequestUseCase(authRepository, jwt, clock)
        val activeOrganization = ActiveOrganizationResolverUseCase(authRepository)
        val effectivePermissions = EffectivePermissionResolverUseCase(authRepository)
        val accessToken = jwt.issueAccessToken(userId = user.id, sessionId = session.id, issuedAt = now).token

        val state = RouteAdminCatalogState(now)
        val templateRepository = RouteTemplateRepository(state)
        val itemRepository = RouteOrganizationItemRepository(state)
        val categoryRepository = RouteCategoryRepository(state)
        val familyRepository = RouteFamilyRepository(state)
        val requestRepository = RouteCatalogRequestRepository(state)
        val identifierConflictChecker = RouteIdentifierConflictChecker(state)
        val priceHistoryRepository = RoutePriceHistoryRepository()
        val taxAssignmentRepository = RouteCatalogTaxProfileAssignmentRepository(state)
        val taxProfileRepository = RouteInMemoryTaxProfileRepository(now)
        val taxSettingsRepository = RouteInMemoryOrganizationTaxSettingsRepository(now)
        val idGenerator = RouteIncrementalCatalogIdGenerator()

        return Fixture(
            authenticateRequestUseCase = authenticate,
            activeOrganizationResolverUseCase = activeOrganization,
            effectivePermissionResolverUseCase = effectivePermissions,
            adminCatalogModule = AdminCatalogModule(
                searchMasterTemplatesUseCase = SearchAdminCatalogMasterTemplatesUseCase(templateRepository),
                getTemplateUseCase = CatalogGetTemplateUseCase(templateRepository),
                createPlatformTemplateUseCase = CatalogCreatePlatformTemplateUseCase(templateRepository, idGenerator),
                searchCategoriesUseCase = CatalogSearchCategoriesUseCase(categoryRepository),
                createCategoryUseCase = CatalogCreateCategoryUseCase(categoryRepository, idGenerator),
                searchFamiliesUseCase = CatalogSearchFamiliesUseCase(familyRepository),
                createFamilyUseCase = CatalogCreateFamilyUseCase(familyRepository, categoryRepository, idGenerator),
                searchOrganizationItemsUseCase = CatalogSearchOrganizationItemsUseCase(itemRepository),
                getOrganizationItemUseCase = CatalogGetOrganizationItemUseCase(itemRepository),
                copyTemplateToOrganizationUseCase = CatalogCopyTemplateToOrganizationUseCase(
                    templateRepository = templateRepository,
                    itemRepository = itemRepository,
                    profileRepository = taxProfileRepository,
                    settingsRepository = taxSettingsRepository,
                    idGenerator = idGenerator,
                ),
                updateLocalItemUseCase = CatalogUpdateLocalItemUseCase(
                    itemRepository = itemRepository,
                    profileRepository = taxProfileRepository,
                    settingsRepository = taxSettingsRepository,
                    priceHistoryRepository = priceHistoryRepository,
                    identifierConflictChecker = identifierConflictChecker,
                    idGenerator = idGenerator,
                ),
                changeLocalItemStatusUseCase = ChangeAdminCatalogLocalItemStatusUseCase(itemRepository),
                removeLocalItemUseCase = CatalogRemoveLocalItemUseCase(itemRepository),
                requestNewItemUseCase = CatalogRequestNewItemUseCase(requestRepository, idGenerator),
                listOrganizationRequestsUseCase = CatalogListOrganizationRequestsUseCase(requestRepository),
                listAdminRequestsUseCase = CatalogListAdminRequestsUseCase(requestRepository),
                getRequestUseCase = GetAdminCatalogRequestUseCase(requestRepository),
                approveRequestAsTemplateUseCase = CatalogApproveRequestAsTemplateUseCase(
                    requestRepository, templateRepository, idGenerator
                ),
                rejectRequestUseCase = CatalogRejectRequestUseCase(requestRepository),
                linkRequestToExistingTemplateUseCase = CatalogLinkRequestToExistingTemplateUseCase(
                    requestRepository, templateRepository
                ),
                requestMoreInfoUseCase = CatalogRequestMoreInfoUseCase(requestRepository),
            ),
            accessToken = accessToken,
        )
    }

    private data class Fixture(
        val authenticateRequestUseCase: AuthenticateRequestUseCase,
        val activeOrganizationResolverUseCase: ActiveOrganizationResolverUseCase,
        val effectivePermissionResolverUseCase: EffectivePermissionResolverUseCase,
        val adminCatalogModule: AdminCatalogModule,
        val accessToken: String,
    )
}

private class RouteIncrementalCatalogIdGenerator : CatalogIdGenerator {
    private var counter: Int = 0
    override fun newId(prefix: String): String {
        counter += 1
        return "${prefix}_$counter"
    }
}


private class RouteAdminCatalogState(now: Instant) {
    val templates: MutableMap<String, PlatformCatalogTemplate> = mutableMapOf(
        "tpl_1" to PlatformCatalogTemplate(
            id = "tpl_1",
            globalCatalogId = "coca_500",
            canonicalName = "Coca Cola 500ml",
            normalizedName = "coca cola 500ml",
            type = CatalogItemType.PRODUCT,
            status = CatalogTemplateStatus.ACTIVE,
            identifiers = listOf(identifier("7861001234567")),
        ),
    )
    val items: MutableMap<String, OrganizationCatalogItem> = mutableMapOf(
        "ocat_1" to OrganizationCatalogItem(
            id = "ocat_1",
            organizationId = "org_1",
            branchId = null,
            activityId = "act_restaurant",
            templateId = "tpl_1",
            globalCatalogId = "coca_500",
            localName = "Cola local",
            searchableText = "cola local coca_500 BEB001",
            type = CatalogItemType.PRODUCT,
            status = CatalogItemStatus.PAUSED,
            localPrice = Money.of("1.25"),
            taxProfileId = "taxp_iva_15",
            publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
            identifiers = listOf(identifier("BEB-001", CatalogIdentifierType.SKU_LOCAL)),
        ),
    )
    val categories: MutableMap<String, CatalogCategory> = mutableMapOf(
        "cat_food" to CatalogCategory(
            id = "cat_food",
            parentId = null,
            code = "food",
            name = "Comida",
            normalizedName = "comida",
            status = CatalogCategoryStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
        ),
    )
    val families: MutableMap<String, PlatformCatalogFamily> = mutableMapOf()
    val requests: MutableMap<String, CatalogItemRequest> = mutableMapOf()
}

private class RouteTemplateRepository(
    private val state: RouteAdminCatalogState,
) : PlatformCatalogTemplateRepository {
    override fun create(template: PlatformCatalogTemplate) {
        state.templates[template.id] = template
    }

    override fun update(template: PlatformCatalogTemplate) {
        state.templates[template.id] = template
    }

    override fun findById(id: String): PlatformCatalogTemplate? = state.templates[id]
    override fun existsByGlobalCatalogId(globalCatalogId: String): Boolean =
        state.templates.values.any { it.globalCatalogId == globalCatalogId }

    override fun search(query: CatalogTemplateSearchQuery): List<PlatformCatalogTemplate> =
        state.templates.values.filter { !query.onlyActive || it.status == CatalogTemplateStatus.ACTIVE }
            .filter { query.type == null || it.type == query.type }.filter {
                query.query.isNullOrBlank() || it.canonicalName.contains(
                    query.query, ignoreCase = true
                ) || it.globalCatalogId.contains(query.query, ignoreCase = true)
            }
            .filter { query.identifier.isNullOrBlank() || it.identifiers.any { identifier -> identifier.normalizedValue == query.identifier } }
            .sortedBy { it.canonicalName }.take(query.limit)
}

private class RouteOrganizationItemRepository(
    private val state: RouteAdminCatalogState,
) : OrganizationCatalogItemRepository {
    override fun create(item: OrganizationCatalogItem) {
        state.items[item.id] = item
    }

    override fun update(item: OrganizationCatalogItem) {
        state.items[item.id] = item
    }

    override fun findById(organizationId: String, catalogItemId: String): OrganizationCatalogItem? =
        state.items[catalogItemId]?.takeIf { it.organizationId == organizationId }

    override fun existsByTemplateId(organizationId: String, templateId: String): Boolean =
        state.items.values.any { it.organizationId == organizationId && it.templateId == templateId && it.status != CatalogItemStatus.REMOVED_FROM_ACCOUNT }

    override fun search(query: OrganizationCatalogSearchQuery): List<OrganizationCatalogItem> =
        state.items.values.filter { it.organizationId == query.organizationId }
            .filter { query.type == null || it.type == query.type }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }.filter {
                query.query.isNullOrBlank() || it.localName.contains(
                    query.query, ignoreCase = true
                ) || it.searchableText.contains(query.query, ignoreCase = true)
            }
            .filter { query.identifier.isNullOrBlank() || it.identifiers.any { identifier -> identifier.normalizedValue == query.identifier } }
            .sortedBy { it.localName }.take(query.limit)
}

private class RouteCategoryRepository(
    private val state: RouteAdminCatalogState,
) : CatalogCategoryRepository {
    override fun create(category: CatalogCategory) {
        state.categories[category.id] = category
    }

    override fun update(category: CatalogCategory) {
        state.categories[category.id] = category
    }

    override fun findById(id: String): CatalogCategory? = state.categories[id]
    override fun findByCode(code: String): CatalogCategory? = state.categories.values.firstOrNull { it.code == code }
    override fun existsByCode(code: String): Boolean = state.categories.values.any { it.code == code }
    override fun search(query: CatalogCategorySearchQuery): List<CatalogCategory> =
        state.categories.values.filter { query.parentId == null || it.parentId == query.parentId }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }.filter {
                query.query.isNullOrBlank() || it.name.contains(query.query, ignoreCase = true) || it.code.contains(
                    query.query, ignoreCase = true
                )
            }.sortedBy { it.sortOrder }.take(query.limit)
}

private class RouteFamilyRepository(
    private val state: RouteAdminCatalogState,
) : PlatformCatalogFamilyRepository {
    override fun create(family: PlatformCatalogFamily) {
        state.families[family.id] = family
    }

    override fun update(family: PlatformCatalogFamily) {
        state.families[family.id] = family
    }

    override fun findById(id: String): PlatformCatalogFamily? = state.families[id]
    override fun findByGlobalFamilyId(globalFamilyId: String): PlatformCatalogFamily? =
        state.families.values.firstOrNull { it.globalFamilyId == globalFamilyId }

    override fun existsByGlobalFamilyId(globalFamilyId: String): Boolean =
        state.families.values.any { it.globalFamilyId == globalFamilyId }

    override fun search(query: PlatformCatalogFamilySearchQuery): List<PlatformCatalogFamily> =
        state.families.values.filter { query.categoryId == null || it.categoryId == query.categoryId }
            .filter { query.type == null || it.type == query.type }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }.filter {
                query.query.isNullOrBlank() || it.canonicalName.contains(
                    query.query, ignoreCase = true
                ) || it.globalFamilyId.contains(query.query, ignoreCase = true)
            }.sortedBy { it.canonicalName }.take(query.limit)
}

private class RouteCatalogRequestRepository(
    private val state: RouteAdminCatalogState,
) : CatalogItemRequestRepository, CatalogItemRequestSearchRepository {
    override fun create(request: CatalogItemRequest) {
        state.requests[request.id] = request
    }

    override fun update(request: CatalogItemRequest) {
        state.requests[request.id] = request
    }

    override fun findById(requestId: String): CatalogItemRequest? = state.requests[requestId]
    override fun findPendingByOrganizationAndName(organizationId: String, requestedName: String): CatalogItemRequest? =
        state.requests.values.firstOrNull {
            it.organizationId == organizationId && it.requestedName.equals(
                requestedName,
                ignoreCase = true
            ) && it.status == CatalogItemRequestStatus.PENDING_REVIEW
        }

    override fun search(query: CatalogItemRequestSearchQuery): List<CatalogItemRequest> =
        state.requests.values.filter { query.organizationId == null || it.organizationId == query.organizationId }
            .filter { query.statuses.isEmpty() || it.status in query.statuses }
            .filter { query.requestedType == null || it.requestedType == query.requestedType }
            .filter { query.requestedByUserId == null || it.requestedByUserId == query.requestedByUserId }
            .filter { query.query.isNullOrBlank() || it.requestedName.contains(query.query, ignoreCase = true) }
            .sortedByDescending { it.createdAt }.take(query.limit)
}

private class RouteIdentifierConflictChecker(
    private val state: RouteAdminCatalogState,
) : CatalogIdentifierConflictChecker {
    override fun existsLocalIdentifier(
        organizationId: String,
        normalizedValue: String,
        excludeCatalogItemId: String?,
    ): Boolean = state.items.values.any {
        it.organizationId == organizationId && it.id != excludeCatalogItemId && it.identifiers.any { identifier -> identifier.normalizedValue == normalizedValue }
    }
}

private class RoutePriceHistoryRepository : CatalogPriceHistoryRepository {
    override fun create(history: com.hermes.domain.catalog.CatalogPriceHistory) = Unit
}

private class RouteCatalogTaxProfileAssignmentRepository(
    private val state: RouteAdminCatalogState,
) : OrganizationCatalogTaxProfileRepository {
    override fun assignTaxProfile(
        organizationId: String,
        catalogItemId: String,
        taxProfileId: String,
        updatedAt: Instant,
    ): CatalogTaxProfileAssignmentRecord {
        val current = state.items.getValue(catalogItemId)
        state.items[catalogItemId] = current.copy(taxProfileId = taxProfileId)
        return CatalogTaxProfileAssignmentRecord(
            organizationId = organizationId,
            catalogItemId = catalogItemId,
            previousTaxProfileId = current.taxProfileId,
            taxProfileId = taxProfileId,
            updatedAt = updatedAt,
        )
    }
}

private class RouteInMemoryTaxProfileRepository(now: Instant) : TaxProfileRepository {
    private val rate = TaxRate.of(
        id = "taxr_iva_15",
        code = "iva_15_rate",
        name = "IVA 15%",
        rate = "15.0000",
        sriTaxCode = "2",
        sriRateCode = "4",
        legalBasis = "Test seed",
        effectiveFrom = now.minusSeconds(3600),
        now = now,
    )
    private val profile = TaxProfile(
        id = "taxp_iva_15",
        code = "iva_15",
        name = "IVA 15%",
        treatment = TaxTreatment.IVA_FULL,
        status = TaxProfileStatus.ACTIVE,
        taxRate = rate,
        sriTaxCode = "2",
        sriRateCode = "4",
        legalBasis = "Test seed",
        effectiveFrom = now.minusSeconds(3600),
        source = TaxSource.SYSTEM_SEED,
        createdAt = now,
        updatedAt = now,
    )

    override fun create(profile: TaxProfile) = Unit
    override fun update(profile: TaxProfile) = Unit
    override fun findById(id: String): TaxProfile? = profile.takeIf { it.id == id }
    override fun findByCode(code: String): TaxProfile? = profile.takeIf { it.code == code }
    override fun findActive(): List<TaxProfile> = listOf(profile)
}

private class RouteInMemoryOrganizationTaxSettingsRepository(now: Instant) : OrganizationTaxSettingsRepository {
    private val settings = OrganizationTaxSettings(
        id = "tax_settings_org_1",
        organizationId = "org_1",
        regime = TaxRegimeCode.GENERAL,
        defaultTaxProfileCode = "iva_15",
        enabledTaxProfileCodes = setOf("iva_15"),
        allowTaxInclusivePrices = true,
        allowManualLineDiscounts = true,
        requireTaxProfileForCatalogItems = true,
        status = OrganizationTaxSettingsStatus.ACTIVE,
        createdAt = now,
        updatedAt = now,
        createdBy = "usr_1",
        updatedBy = "usr_1",
    )

    override fun create(settings: OrganizationTaxSettings) = Unit
    override fun update(settings: OrganizationTaxSettings) = Unit
    override fun findByOrganizationId(organizationId: String): OrganizationTaxSettings? =
        settings.takeIf { it.organizationId == organizationId }
}

private fun identifier(
    value: String,
    type: CatalogIdentifierType = CatalogIdentifierType.BARCODE,
): CatalogIdentifier = CatalogIdentifier.create(
    type = type,
    value = value,
    scope = CatalogIdentifierScope.ORGANIZATION,
    source = CatalogIdentifierSource.ORGANIZATION,
    status = CatalogIdentifierStatus.ACTIVE,
    isPrimary = true,
)
