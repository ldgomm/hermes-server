package com.hermes.application.sales

import com.hermes.application.catalog.CatalogTemplateSearchQuery
import com.hermes.application.catalog.OrganizationCatalogItemRepository
import com.hermes.application.catalog.OrganizationCatalogSearchQuery
import com.hermes.application.catalog.PlatformCatalogTemplateRepository
import com.hermes.application.tax.OrganizationTaxSettingsRepository
import com.hermes.application.tax.TaxProfileRepository
import com.hermes.application.tax.TaxSaleValidationUseCase
import com.hermes.domain.catalog.CatalogItemStatus
import com.hermes.domain.catalog.CatalogItemType
import com.hermes.domain.catalog.OrganizationCatalogItem
import com.hermes.domain.catalog.PlatformCatalogTemplate
import com.hermes.domain.catalog.PublicDiscoveryStatus
import com.hermes.domain.money.Money
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.quantity.Quantity
import com.hermes.domain.reservation.Reservation
import com.hermes.domain.reservation.ReservationStatus
import com.hermes.domain.sale.CatalogItemSnapshot
import com.hermes.domain.sale.CustomerSnapshot
import com.hermes.domain.sale.Sale
import com.hermes.domain.sale.SaleItem
import com.hermes.domain.sale.SaleItemTax
import com.hermes.domain.sale.SaleOperationalStatus
import com.hermes.domain.sale.SaleType
import com.hermes.domain.sale.SaleWorkflowMode
import com.hermes.domain.sale.TaxProfileSnapshotForSale
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.tax.OrganizationTaxSettings
import com.hermes.domain.tax.OrganizationTaxSettingsStatus
import com.hermes.domain.tax.TaxFixtures
import com.hermes.domain.tax.TaxProfile
import com.hermes.domain.tax.TaxRegimeCode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal val SalesTestNow: Instant = Instant.parse("2026-05-18T10:00:00Z")
internal val SalesTestClock: Clock = Clock.fixed(SalesTestNow, ZoneOffset.UTC)

internal fun salesPermissions(): Set<String> = setOf(
    PermissionCatalog.SALES_VIEW,
    PermissionCatalog.SALES_CREATE,
    PermissionCatalog.SALES_CONFIRM,
    PermissionCatalog.SALES_CANCEL,
    PermissionCatalog.SALES_CLOSE,
    PermissionCatalog.SALES_ITEMS_CHANGE_STATUS,
)

internal class RecordingSalesAuditLogger : SalesAuditLogger {
    val events = mutableListOf<SalesAuditEvent>()

    override fun log(event: SalesAuditEvent) {
        events += event
    }
}

internal class DeterministicSalesIdGenerator : SalesIdGenerator {
    private val counters = linkedMapOf<String, Int>()

    override fun newId(prefix: String): String {
        val cleanPrefix = prefix.trim().lowercase()
        val next = counters.getOrDefault(cleanPrefix, 0) + 1
        counters[cleanPrefix] = next
        return "${cleanPrefix}_${next}"
    }
}

internal class InMemoryOperationalSaleRepository : OperationalSaleRepository {
    val sales = linkedMapOf<String, Sale>()

    override fun create(sale: Sale) {
        if (sales.containsKey(sale.id)) throw DomainRuleViolation("Sale already exists.")
        sales[sale.id] = sale
    }

    override fun update(sale: Sale) {
        if (!sales.containsKey(sale.id)) throw DomainRuleViolation("Sale does not exist.")
        sales[sale.id] = sale
    }

    override fun findById(organizationId: String, saleId: String): Sale? =
        sales[saleId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun search(query: SaleSearchQuery): List<Sale> = sales.values
        .asSequence()
        .filter { it.organizationId == query.organizationId.trim() }
        .filter { query.statuses.isEmpty() || it.operationalStatus in query.statuses }
        .filter { query.customerId == null || it.customerId == query.customerId }
        .filter { query.activityId == null || it.activityId == query.activityId }
        .filter { query.from == null || !it.createdAt.isBefore(query.from) }
        .filter { query.to == null || !it.createdAt.isAfter(query.to) }
        .sortedByDescending { it.createdAt }
        .take(query.limit.coerceIn(1, 500))
        .toList()
}

internal class InMemoryOperationalReservationRepository : OperationalReservationRepository {
    val reservations = linkedMapOf<String, Reservation>()

    override fun create(reservation: Reservation) {
        if (reservations.containsKey(reservation.id)) throw DomainRuleViolation("Reservation already exists.")
        reservations[reservation.id] = reservation
    }

    override fun update(reservation: Reservation) {
        if (!reservations.containsKey(reservation.id)) throw DomainRuleViolation("Reservation does not exist.")
        reservations[reservation.id] = reservation
    }

    override fun findById(organizationId: String, reservationId: String): Reservation? =
        reservations[reservationId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun search(query: ReservationSearchQuery): List<Reservation> = reservations.values
        .asSequence()
        .filter { it.organizationId == query.organizationId.trim() }
        .filter { query.statuses.isEmpty() || it.status in query.statuses }
        .filter { query.customerId == null || it.customerId == query.customerId }
        .filter { query.activityId == null || it.activityId == query.activityId }
        .filter { query.from == null || !it.startAt.isBefore(query.from) }
        .filter { query.to == null || !it.startAt.isAfter(query.to) }
        .sortedBy { it.startAt }
        .take(query.limit.coerceIn(1, 500))
        .toList()
}

internal class InMemoryOrganizationCatalogItemRepository : OrganizationCatalogItemRepository {
    val items = linkedMapOf<String, OrganizationCatalogItem>()

    override fun create(item: OrganizationCatalogItem) {
        items[item.id] = item
    }

    override fun update(item: OrganizationCatalogItem) {
        items[item.id] = item
    }

    override fun findById(organizationId: String, catalogItemId: String): OrganizationCatalogItem? =
        items[catalogItemId.trim()]?.takeIf { it.organizationId == organizationId.trim() }

    override fun existsByTemplateId(organizationId: String, templateId: String): Boolean =
        items.values.any { it.organizationId == organizationId.trim() && it.templateId == templateId.trim() }

    override fun search(query: OrganizationCatalogSearchQuery): List<OrganizationCatalogItem> = items.values
        .asSequence()
        .filter { it.organizationId == query.organizationId.trim() }
        .filter { query.statuses.isEmpty() || it.status in query.statuses }
        .filter { query.type == null || it.type == query.type }
        .filter { query.identifier == null || it.identifiers.any { identifier -> identifier.normalizedValue == query.identifier } }
        .filter { query.query == null || it.searchableText.contains(query.query.trim(), ignoreCase = true) }
        .take(query.limit.coerceIn(1, 500))
        .toList()
}

internal class InMemoryTaxProfileRepositoryForSales : TaxProfileRepository {
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

    override fun findActive(): List<TaxProfile> = profiles.values.filter { it.status.name == "ACTIVE" }
}

internal class InMemoryOrganizationTaxSettingsRepositoryForSales : OrganizationTaxSettingsRepository {
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

internal data class SalesUseCaseFixture(
    val saleRepository: InMemoryOperationalSaleRepository,
    val reservationRepository: InMemoryOperationalReservationRepository,
    val catalogRepository: InMemoryOrganizationCatalogItemRepository,
    val profileRepository: InMemoryTaxProfileRepositoryForSales,
    val settingsRepository: InMemoryOrganizationTaxSettingsRepositoryForSales,
    val idGenerator: DeterministicSalesIdGenerator,
    val auditLogger: RecordingSalesAuditLogger,
    val createQuickSaleUseCase: CreateQuickSaleUseCase,
    val addSaleItemUseCase: AddSaleItemUseCase,
    val getSaleUseCase: GetSaleUseCase,
    val searchSalesUseCase: SearchSalesUseCase,
    val changeSaleStatusUseCase: ChangeSaleStatusUseCase,
    val changeSaleItemStatusUseCase: ChangeSaleItemStatusUseCase,
    val cancelSaleUseCase: CancelSaleUseCase,
    val closeSaleUseCase: CloseSaleUseCase,
    val createReservationUseCase: CreateReservationUseCase,
    val searchReservationsUseCase: SearchReservationsUseCase,
)

internal fun salesFixture(): SalesUseCaseFixture {
    val saleRepository = InMemoryOperationalSaleRepository()
    val reservationRepository = InMemoryOperationalReservationRepository()
    val catalogRepository = InMemoryOrganizationCatalogItemRepository()
    val profileRepository = InMemoryTaxProfileRepositoryForSales()
    val settingsRepository = InMemoryOrganizationTaxSettingsRepositoryForSales()
    val idGenerator = DeterministicSalesIdGenerator()
    val auditLogger = RecordingSalesAuditLogger()

    TaxFixtures.allProfiles.forEach(profileRepository::create)
    settingsRepository.create(activeTaxSettings())
    catalogRepository.create(activeCatalogItem())

    val taxSaleValidationUseCase = TaxSaleValidationUseCase(
        profileRepository = profileRepository,
        settingsRepository = settingsRepository,
        clock = SalesTestClock,
    )
    val itemPreparationService = SaleItemPreparationService(
        catalogRepository = catalogRepository,
        taxProfileRepository = profileRepository,
        settingsRepository = settingsRepository,
        taxSaleValidationUseCase = taxSaleValidationUseCase,
        idGenerator = idGenerator,
    )
    val createQuickSaleUseCase = CreateQuickSaleUseCase(
        saleRepository = saleRepository,
        saleItemPreparationService = itemPreparationService,
        idGenerator = idGenerator,
        auditLogger = auditLogger,
        clock = SalesTestClock,
    )
    val changeSaleStatusUseCase = ChangeSaleStatusUseCase(
        saleRepository = saleRepository,
        auditLogger = auditLogger,
        clock = SalesTestClock,
    )

    return SalesUseCaseFixture(
        saleRepository = saleRepository,
        reservationRepository = reservationRepository,
        catalogRepository = catalogRepository,
        profileRepository = profileRepository,
        settingsRepository = settingsRepository,
        idGenerator = idGenerator,
        auditLogger = auditLogger,
        createQuickSaleUseCase = createQuickSaleUseCase,
        addSaleItemUseCase = AddSaleItemUseCase(
            saleRepository = saleRepository,
            saleItemPreparationService = itemPreparationService,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
        getSaleUseCase = GetSaleUseCase(
            saleRepository = saleRepository,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
        searchSalesUseCase = SearchSalesUseCase(
            saleRepository = saleRepository,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
        changeSaleStatusUseCase = changeSaleStatusUseCase,
        changeSaleItemStatusUseCase = ChangeSaleItemStatusUseCase(
            saleRepository = saleRepository,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
        cancelSaleUseCase = CancelSaleUseCase(
            saleRepository = saleRepository,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
        closeSaleUseCase = CloseSaleUseCase(changeSaleStatusUseCase),
        createReservationUseCase = CreateReservationUseCase(
            reservationRepository = reservationRepository,
            createQuickSaleUseCase = createQuickSaleUseCase,
            idGenerator = idGenerator,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
        searchReservationsUseCase = SearchReservationsUseCase(
            reservationRepository = reservationRepository,
            auditLogger = auditLogger,
            clock = SalesTestClock,
        ),
    )
}

internal fun activeTaxSettings(): OrganizationTaxSettings = OrganizationTaxSettings(
    id = "taxset_org_1",
    organizationId = "org_1",
    regime = TaxRegimeCode.RIMPE_ENTREPRENEUR,
    defaultTaxProfileCode = "iva_current_full",
    enabledTaxProfileCodes = setOf(
        "iva_current_full",
        "iva_0",
        "exempt_iva",
        "not_subject_to_iva",
        "no_tax_internal",
    ),
    allowTaxInclusivePrices = true,
    allowManualLineDiscounts = true,
    requireTaxProfileForCatalogItems = true,
    status = OrganizationTaxSettingsStatus.ACTIVE,
    createdAt = SalesTestNow,
    updatedAt = SalesTestNow,
    createdBy = "usr_owner",
    updatedBy = "usr_owner",
)

internal fun activeCatalogItem(
    id: String = "cat_1",
    name: String = "Café americano",
    price: String = "10.00",
    status: CatalogItemStatus = CatalogItemStatus.ACTIVE,
    activityId: String = "act_restaurant",
): OrganizationCatalogItem = OrganizationCatalogItem(
    id = id,
    organizationId = "org_1",
    branchId = "br_1",
    activityId = activityId,
    templateId = "tpl_$id",
    globalCatalogId = "global_$id",
    localName = name,
    searchableText = name.lowercase(),
    type = CatalogItemType.PRODUCT,
    status = status,
    localPrice = Money.of(price),
    taxProfileId = "taxp_iva13",
    publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
)

internal fun saleLine(
    catalogItemId: String = "cat_1",
    quantity: Int = 1,
    unitPrice: Money? = null,
): CreateSaleItemCommandLine = CreateSaleItemCommandLine(
    catalogItemId = catalogItemId,
    quantity = Quantity.units(quantity),
    unitPrice = unitPrice,
)

internal fun saleCommand(
    permissions: Set<String> = salesPermissions(),
    autoConfirm: Boolean = true,
    items: List<CreateSaleItemCommandLine> = listOf(saleLine(quantity = 2)),
): CreateQuickSaleCommand = CreateQuickSaleCommand(
    organizationId = "org_1",
    branchId = "br_1",
    activityId = "act_restaurant",
    actorUserId = "usr_1",
    actorEffectivePermissions = permissions,
    customerSnapshot = CustomerSnapshot.finalConsumer(),
    occurredAt = SalesTestNow,
    autoConfirm = autoConfirm,
    items = items,
)

internal fun scheduledReservationCommand(
    linkedSaleItem: CreateSaleItemCommandLine? = null,
    permissions: Set<String> = salesPermissions(),
): CreateReservationCommand = CreateReservationCommand(
    organizationId = "org_1",
    branchId = "br_1",
    activityId = "act_tourism",
    actorUserId = "usr_1",
    actorEffectivePermissions = permissions,
    customerId = "cust_1",
    customerSnapshot = CustomerSnapshot(
        customerId = "cust_1",
        displayName = "Ana Cliente",
        taxId = "1720000001",
        taxIdType = "cedula",
        email = "ana@example.com",
    ),
    resourceId = "quad_1",
    startAt = SalesTestNow.plusSeconds(3600),
    endAt = SalesTestNow.plusSeconds(7200),
    partySize = 2,
    notes = "Traer casco extra",
    linkedSaleItem = linkedSaleItem,
)

internal fun basicSaleItem(
    id: String = "sitem_1",
    catalogItemId: String = "cat_1",
    name: String = "Café americano",
    unitPrice: Money = Money.of("10.00"),
    quantity: Quantity = Quantity.units(1),
): SaleItem = SaleItem.create(
    id = id,
    catalogItemId = catalogItemId,
    name = name,
    unitPrice = unitPrice,
    quantity = quantity,
    discount = Money.zero(unitPrice.currency),
    catalogSnapshot = CatalogItemSnapshot(
        catalogItemId = catalogItemId,
        sourceTemplateId = "tpl_$catalogItemId",
        globalCatalogId = "global_$catalogItemId",
        productFamilyId = null,
        name = name,
        type = CatalogItemType.PRODUCT,
        taxProfileId = "taxp_iva13",
        unitCode = "unit",
    ),
    taxProfileSnapshot = TaxProfileSnapshotForSale(
        code = "iva_current_full",
        taxName = "IVA current full",
        rate = com.hermes.domain.percentage.Percentage.of("13.0000".toBigDecimal()),
        sriTaxCode = "2",
        sriRateCode = "4",
        treatment = com.hermes.domain.tax.TaxTreatment.IVA_FULL,
        legalBasis = "Test legal basis",
        effectiveFrom = LocalDate.parse("2026-01-01"),
        source = "SYSTEM_SEED",
    ),
    taxes = listOf(
        SaleItemTax(
            taxCode = "2",
            rateCode = "4",
            rate = com.hermes.domain.percentage.Percentage.of("13.0000".toBigDecimal()),
            taxableBase = Money.of("10.00"),
            amount = Money.of("1.30"),
        )
    ),
)

internal fun draftSale(
    id: String = "sale_1",
    item: SaleItem = basicSaleItem(),
    createdAt: Instant = SalesTestNow,
): Sale = Sale.createDraft(
    id = id,
    organizationId = "org_1",
    branchId = "br_1",
    activityId = "act_restaurant",
    saleType = SaleType.SALE,
    workflowMode = SaleWorkflowMode.QUICK_SALE,
    saleNumber = "SALE-001",
    customerSnapshot = CustomerSnapshot.finalConsumer(),
    createdAt = createdAt,
).addItem(item, createdAt)

internal fun confirmedSale(id: String = "sale_1", createdAt: Instant = SalesTestNow): Sale =
    draftSale(id = id, createdAt = createdAt).confirm(createdAt)

internal fun scheduledReservation(
    id: String = "res_1",
    status: ReservationStatus = ReservationStatus.SCHEDULED,
    activityId: String = "act_tourism",
): Reservation {
    val base = Reservation.schedule(
        id = id,
        organizationId = "org_1",
        branchId = "br_1",
        activityId = activityId,
        customerId = "cust_1",
        customerSnapshot = CustomerSnapshot.finalConsumer(),
        startAt = SalesTestNow.plusSeconds(3600),
        endAt = SalesTestNow.plusSeconds(7200),
        partySize = 2,
        createdAt = SalesTestNow,
    )
    return when (status) {
        ReservationStatus.SCHEDULED -> base
        ReservationStatus.CONFIRMED -> base.confirm(SalesTestNow)
        ReservationStatus.IN_PROGRESS -> base.confirm(SalesTestNow).start(SalesTestNow)
        ReservationStatus.COMPLETED -> base.confirm(SalesTestNow).start(SalesTestNow).complete(SalesTestNow)
        ReservationStatus.CANCELED -> base.cancel(SalesTestNow)
        ReservationStatus.RESCHEDULED -> base.reschedule(
            SalesTestNow.plusSeconds(10_800),
            SalesTestNow.plusSeconds(14_400),
            SalesTestNow,
        )
        ReservationStatus.DRAFT -> base.copy(status = ReservationStatus.DRAFT)
        ReservationStatus.NO_SHOW -> base.copy(status = ReservationStatus.NO_SHOW)
    }
}
