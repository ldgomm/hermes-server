package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.*
import com.hermes.application.signature.*
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicInvoiceIssueRepository
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoElectronicSequenceRepository
import com.hermes.infrastructure.mongo.electronicinvoicing.MongoOrganizationSriSettingsRepository
import com.hermes.infrastructure.mongo.signature.MongoElectronicSignatureRepository
import com.hermes.infrastructure.security.LocalEncryptedSignatureSecretStore
import com.hermes.infrastructure.security.Pkcs12CertificateInspector
import com.mongodb.client.MongoDatabase

object ElectronicInvoicingModuleFactory {
    fun fromMongo(database: MongoDatabase): ElectronicInvoicingModule {
        val issueRepository = MongoElectronicInvoiceIssueRepository(database)
        val settingsRepository = MongoOrganizationSriSettingsRepository(database)
        val sequenceRepository = MongoElectronicSequenceRepository(database)
        val signatureRepository = MongoElectronicSignatureRepository(database)
        val signatureSecretStore = LocalEncryptedSignatureSecretStore.default()
        val certificateInspector = Pkcs12CertificateInspector()

        return ElectronicInvoicingModule(
            getElectronicInvoiceUseCase = GetElectronicInvoiceUseCase(issueRepository),
            listElectronicInvoicesUseCase = ListElectronicInvoicesUseCase(issueRepository),
            getOrganizationSriSettingsUseCase = GetOrganizationSriSettingsUseCase(settingsRepository),
            upsertOrganizationSriSettingsUseCase = UpsertOrganizationSriSettingsUseCase(settingsRepository),
            checkOrganizationSriReadinessUseCase = CheckOrganizationSriReadinessUseCase(
                settingsRepository = settingsRepository,
                signatureRepository = signatureRepository,
                sequenceRepository = sequenceRepository,
            ),
            uploadElectronicSignatureUseCase = UploadElectronicSignatureUseCase(
                repository = signatureRepository,
                secretStore = signatureSecretStore,
                inspector = certificateInspector,
            ),
            listElectronicSignaturesUseCase = ListElectronicSignaturesUseCase(signatureRepository),
            getElectronicSignatureUseCase = GetElectronicSignatureUseCase(signatureRepository),
            validateElectronicSignatureUseCase = ValidateElectronicSignatureUseCase(
                repository = signatureRepository,
                secretReader = signatureSecretStore,
                inspector = certificateInspector,
            ),
            activateElectronicSignatureUseCase = ActivateElectronicSignatureUseCase(signatureRepository),
            revokeElectronicSignatureUseCase = RevokeElectronicSignatureUseCase(signatureRepository),
            ensureElectronicSequenceAdminUseCase = EnsureElectronicSequenceAdminUseCase(sequenceRepository),
            listElectronicSequencesUseCase = ListElectronicSequencesUseCase(sequenceRepository),
            getElectronicSequenceUseCase = GetElectronicSequenceUseCase(sequenceRepository),
        )
    }
}
