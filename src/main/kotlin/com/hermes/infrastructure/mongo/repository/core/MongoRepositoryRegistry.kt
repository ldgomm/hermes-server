package com.hermes.infrastructure.mongo.repository.core

import com.mongodb.client.MongoDatabase

class MongoRepositoryRegistry(
    database: MongoDatabase,
) {
    val organizations = OrganizationRepository(database)
    val businessActivities = BusinessActivityRepository(database)
    val branches = BranchRepository(database)
    val emissionPoints = EmissionPointRepository(database)
    val users = UserRepository(database)
    val memberships = MembershipRepository(database)
    val roles = RoleRepository(database)
    val permissions = PermissionRepository(database)
    val credentialEvents = CredentialEventRepository(database)
    val userSessions = UserSessionRepository(database)
    val platformCategories = PlatformCategoryRepository(database)
    val platformCatalogFamilies = PlatformCatalogFamilyRepository(database)
    val platformCatalogTemplates = PlatformCatalogTemplateRepository(database)
    val platformCatalog = PlatformCatalogRepository(database)
    val organizationCatalog = OrganizationCatalogRepository(database)
    val catalogRequests = CatalogRequestRepository(database)
    val catalogIdentifierRegistry = CatalogIdentifierRegistryRepository(database)
    val catalogIdentityConflicts = CatalogIdentityConflictRepository(database)
    val catalogPriceHistory = CatalogPriceHistoryRepository(database)
    val catalogAttributeDefinitions = CatalogAttributeDefinitionRepository(database)
    val catalogMediaAssets = CatalogMediaAssetRepository(database)
    val units = UnitRepository(database)
    val unitConversions = UnitConversionRepository(database)
    val taxRates = TaxRateRepository(database)
    val taxProfiles = TaxProfileRepository(database)
    val organizationTaxSettings = OrganizationTaxSettingsRepository(database)
    val customers = CustomerRepository(database)
    val sales = SaleRepository(database)
    val serviceOrders = ServiceOrderRepository(database)
    val reservations = ReservationRepository(database)
    val reservationSlots = ReservationSlotRepository(database)
    val capacityResources = CapacityResourceRepository(database)
    val payments = PaymentRepository(database)
    val receivables = ReceivableRepository(database)
    val cashSessions = CashSessionRepository(database)
    val cashMovements = CashMovementRepository(database)
    val commercialDocuments = CommercialDocumentRepository(database)
    val electronicDocumentPayloads = ElectronicDocumentPayloadRepository(database)
    val sriSubmissions = SriSubmissionRepository(database)
    val electronicSignatures = ElectronicSignatureRepository(database)
    val electronicSignatureEvents = ElectronicSignatureEventRepository(database)
    val stockBalances = StockBalanceRepository(database)
    val stockMovements = StockMovementRepository(database)
    val stockReservations = StockReservationRepository(database)
    val auditLogs = AuditLogRepository(database)
    val domainEvents = DomainEventRepository(database)
    val outboxEvents = OutboxEventRepository(database)
    val organizationSettings = OrganizationSettingsRepository(database)
    val featureFlags = FeatureFlagRepository(database)
    val counters = CounterRepository(database)
    val businessHours = BusinessHoursRepository(database)
    val specialHours = SpecialHoursRepository(database)
    val temporaryClosures = TemporaryClosureRepository(database)
    val serviceAreas = ServiceAreaRepository(database)
    val catalogImportJobs = CatalogImportJobRepository(database)
    val returnPolicies = ReturnPolicyRepository(database)
    val warrantyPolicies = WarrantyPolicyRepository(database)

    val documentRepositories: List<DocumentMongoRepository> = listOf(
        organizations,
        businessActivities,
        branches,
        emissionPoints,
        users,
        memberships,
        roles,
        permissions,
        credentialEvents,
        userSessions,
        platformCategories,
        platformCatalogFamilies,
        platformCatalogTemplates,
        organizationCatalog,
        catalogRequests,
        catalogIdentifierRegistry,
        catalogIdentityConflicts,
        catalogPriceHistory,
        catalogAttributeDefinitions,
        catalogMediaAssets,
        units,
        unitConversions,
        taxRates,
        taxProfiles,
        organizationTaxSettings,
        customers,
        sales,
        serviceOrders,
        reservations,
        reservationSlots,
        capacityResources,
        payments,
        receivables,
        cashSessions,
        cashMovements,
        commercialDocuments,
        electronicDocumentPayloads,
        sriSubmissions,
        electronicSignatures,
        electronicSignatureEvents,
        stockBalances,
        stockMovements,
        stockReservations,
        auditLogs,
        domainEvents,
        outboxEvents,
        organizationSettings,
        featureFlags,
        counters,
        businessHours,
        specialHours,
        temporaryClosures,
        serviceAreas,
        catalogImportJobs,
        returnPolicies,
        warrantyPolicies,
    )
}
