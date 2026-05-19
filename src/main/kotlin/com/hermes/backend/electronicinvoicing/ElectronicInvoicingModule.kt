package com.hermes.backend.electronicinvoicing

import com.hermes.application.electronicinvoicing.CheckOrganizationSriReadinessUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceTimelineUseCase
import com.hermes.application.electronicinvoicing.GenerateElectronicInvoiceRideUseCase
import com.hermes.application.electronicinvoicing.EmailElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.DownloadElectronicInvoiceArtifactUseCase
import com.hermes.application.electronicinvoicing.EnableSriProductionUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationReadinessUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationReportUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceHomologationRunUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoiceHomologationRunsUseCase
import com.hermes.application.electronicinvoicing.RunElectronicInvoiceHomologationFromAdminUseCase
import com.hermes.application.electronicinvoicing.EnsureElectronicSequenceAdminUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceUseCase
import com.hermes.application.electronicinvoicing.GetElectronicSequenceUseCase
import com.hermes.application.electronicinvoicing.GetOrganizationSriSettingsUseCase
import com.hermes.application.electronicinvoicing.GetElectronicInvoiceErrorsUseCase
import com.hermes.application.electronicinvoicing.IssueElectronicInvoiceFromSaleUseCase
import com.hermes.application.electronicinvoicing.ListElectronicInvoicesUseCase
import com.hermes.application.electronicinvoicing.ListElectronicSequencesUseCase
import com.hermes.application.electronicinvoicing.RetryElectronicInvoiceAuthorizationUseCase
import com.hermes.application.electronicinvoicing.UpsertOrganizationSriSettingsUseCase
import com.hermes.application.signature.ActivateElectronicSignatureUseCase
import com.hermes.application.signature.GetElectronicSignatureUseCase
import com.hermes.application.signature.ListElectronicSignaturesUseCase
import com.hermes.application.signature.RevokeElectronicSignatureUseCase
import com.hermes.application.signature.UploadElectronicSignatureUseCase
import com.hermes.application.signature.ValidateElectronicSignatureUseCase

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
    val issueElectronicInvoiceFromSaleUseCase: IssueElectronicInvoiceFromSaleUseCase? = null,
    val retryElectronicInvoiceAuthorizationUseCase: RetryElectronicInvoiceAuthorizationUseCase? = null,
    val getElectronicInvoiceErrorsUseCase: GetElectronicInvoiceErrorsUseCase? = null,
    val generateElectronicInvoiceRideUseCase: GenerateElectronicInvoiceRideUseCase? = null,
    val emailElectronicInvoiceUseCase: EmailElectronicInvoiceUseCase? = null,
    val downloadElectronicInvoiceArtifactUseCase: DownloadElectronicInvoiceArtifactUseCase? = null,
    val getElectronicInvoiceTimelineUseCase: GetElectronicInvoiceTimelineUseCase? = null,
    val getElectronicInvoiceHomologationReadinessUseCase: GetElectronicInvoiceHomologationReadinessUseCase? = null,
    val runElectronicInvoiceHomologationFromAdminUseCase: RunElectronicInvoiceHomologationFromAdminUseCase? = null,
    val listElectronicInvoiceHomologationRunsUseCase: ListElectronicInvoiceHomologationRunsUseCase? = null,
    val getElectronicInvoiceHomologationRunUseCase: GetElectronicInvoiceHomologationRunUseCase? = null,
    val getElectronicInvoiceHomologationReportUseCase: GetElectronicInvoiceHomologationReportUseCase? = null,
    val enableSriProductionUseCase: EnableSriProductionUseCase? = null,
)
