package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.*
import com.hermes.application.signature.*

/**
 * Keeps 12A read-only construction compatible while allowing 12B Admin/SRI routes
 * to require their use cases at runtime.
 */
data class ElectronicInvoicingModule(
    val getElectronicInvoiceUseCase: GetElectronicInvoiceUseCase,
    val listElectronicInvoicesUseCase: ListElectronicInvoicesUseCase,
    val getOrganizationSriSettingsUseCase: GetOrganizationSriSettingsUseCase? = null,
    val upsertOrganizationSriSettingsUseCase: UpsertOrganizationSriSettingsUseCase? = null,
    val checkOrganizationSriReadinessUseCase: CheckOrganizationSriReadinessUseCase? = null,
    val uploadElectronicSignatureUseCase: UploadElectronicSignatureUseCase? = null,
    val listElectronicSignaturesUseCase: ListElectronicSignaturesUseCase? = null,
    val getElectronicSignatureUseCase: GetElectronicSignatureUseCase? = null,
    val validateElectronicSignatureUseCase: ValidateElectronicSignatureUseCase? = null,
    val activateElectronicSignatureUseCase: ActivateElectronicSignatureUseCase? = null,
    val revokeElectronicSignatureUseCase: RevokeElectronicSignatureUseCase? = null,
    val ensureElectronicSequenceAdminUseCase: EnsureElectronicSequenceAdminUseCase? = null,
    val listElectronicSequencesUseCase: ListElectronicSequencesUseCase? = null,
    val getElectronicSequenceUseCase: GetElectronicSequenceUseCase? = null,
)
