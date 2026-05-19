package com.hermes.infrastructure.mongo.migration

import com.hermes.infrastructure.mongo.migration.core.M001CreateOrganizationsMigration
import com.hermes.infrastructure.mongo.migration.core.M002CreateOrganizationActivitiesMigration
import com.hermes.infrastructure.mongo.migration.core.M003CreateBranchesAndEmissionPointsMigration
import com.hermes.infrastructure.mongo.migration.core.M004CreateUsersRolesPermissionsCredentialsMigration
import com.hermes.infrastructure.mongo.migration.core.M005CreatePlatformCatalogMigration
import com.hermes.infrastructure.mongo.migration.core.M006CreateOrganizationCatalogMigration
import com.hermes.infrastructure.mongo.migration.core.M007CreateCatalogRequestsMigration
import com.hermes.infrastructure.mongo.migration.core.M008CreateTaxEngineMigration
import com.hermes.infrastructure.mongo.migration.core.M009CreateCustomersMigration
import com.hermes.infrastructure.mongo.migration.core.M010CreateSalesMigration
import com.hermes.infrastructure.mongo.migration.core.M011CreateServicesReservationsMigration
import com.hermes.infrastructure.mongo.migration.core.M012CreatePaymentsReceivablesMigration
import com.hermes.infrastructure.mongo.migration.core.M013CreateCashMigration
import com.hermes.infrastructure.mongo.migration.core.M014CreateCommercialDocumentsMigration
import com.hermes.infrastructure.mongo.migration.core.M015CreateElectronicSignaturesMigration
import com.hermes.infrastructure.mongo.migration.core.M016CreateInventoryMigration
import com.hermes.infrastructure.mongo.migration.core.M017CreateAuditLogsMigration
import com.hermes.infrastructure.mongo.migration.core.M018CreateSettingsFeatureFlagsMigration
import com.hermes.infrastructure.mongo.migration.core.M019CreateCountersMigration
import com.hermes.infrastructure.mongo.migration.core.M020CreateOutboxEventsMigration
import com.hermes.infrastructure.mongo.migration.core.M021CreateCatalogIdentityFoundationMigration
import com.hermes.infrastructure.mongo.migration.core.M021CreateElectronicSequencesMigration
import com.hermes.infrastructure.mongo.migration.core.M022CreateCatalogAttributeDefinitionsMigration
import com.hermes.infrastructure.mongo.migration.core.M023CreateCatalogMediaAssetsMigration
import com.hermes.infrastructure.mongo.migration.core.M024CreateUnitConversionsMigration
import com.hermes.infrastructure.mongo.migration.core.M025CreateBusinessHoursSpecialHoursMigration
import com.hermes.infrastructure.mongo.migration.core.M026CreateServiceAreasMigration
import com.hermes.infrastructure.mongo.migration.core.M027CreateCatalogImportJobsFutureMigration
import com.hermes.infrastructure.mongo.migration.core.M028CreateReturnAndWarrantyPoliciesFutureMigration
import com.hermes.infrastructure.mongo.migration.core.M029CreateElectronicInvoicingPersistenceMigration
import com.hermes.infrastructure.mongo.migration.core.M030AddElectronicInvoiceRideDeliveryMigration

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
        M030AddElectronicInvoiceRideDeliveryMigration,
    )
}
