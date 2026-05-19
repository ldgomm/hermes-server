package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.migration.core.*

object HermesMongoMigrations {
    val all: List<MongoMigration> = listOf(
        M001CreateOrganizationsMigration,
        M002CreateOrganizationActivitiesMigration,
        M003CreateBranchesAndEmissionPointsMigration,
        M004CreateUsersRolesPermissionsCredentialsMigration,
        M005CreatePlatformCatalogMigration,
        M006CreateOrganizationCatalogMigration,
        M007CreateCatalogRequestsMigration,
        M008CreateTaxEngineMigration,
        M009CreateCustomersMigration,
        M010CreateSalesMigration,
        M011CreateServicesReservationsMigration,
        M012CreatePaymentsReceivablesMigration,
        M013CreateCashMigration,
        M014CreateCommercialDocumentsMigration,
        M015CreateElectronicSignaturesMigration,
        M016CreateInventoryMigration,
        M017CreateAuditLogsMigration,
        M018CreateSettingsFeatureFlagsMigration,
        M019CreateCountersMigration,
        M020CreateOutboxEventsMigration,
        M021CreateCatalogIdentityFoundationMigration,
        M022CreateCatalogAttributeDefinitionsMigration,
        M023CreateCatalogMediaAssetsMigration,
        M024CreateUnitConversionsMigration,
        M025CreateBusinessHoursSpecialHoursMigration,
        M026CreateServiceAreasMigration,
        M027CreateCatalogImportJobsFutureMigration,
        M028CreateReturnAndWarrantyPoliciesFutureMigration,
        M021CreateElectronicSequencesMigration,
        M029CreateElectronicInvoicingPersistenceMigration,
    )
}
