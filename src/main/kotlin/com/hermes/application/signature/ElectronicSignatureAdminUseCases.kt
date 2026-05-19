package com.hermes.application.signature

import com.hermes.application.electronicinvoicing.ElectronicSignatureSecretReader
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.signature.ElectronicSignature
import com.hermes.domain.signature.ElectronicSignatureStatus
import com.hermes.domain.signature.SignatureValidityReport
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Arrays
import java.util.UUID

fun interface ElectronicSignatureIdGenerator {
    fun newId(prefix: String): String
}

class UuidElectronicSignatureIdGenerator : ElectronicSignatureIdGenerator {
    override fun newId(prefix: String): String = prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}

class UploadElectronicSignatureUseCase(
    private val repository: ElectronicSignatureRepository,
    private val secretStore: SignatureSecretStore,
    private val inspector: SignatureCertificateInspector,
    private val idGenerator: ElectronicSignatureIdGenerator = UuidElectronicSignatureIdGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: UploadElectronicSignatureCommand): ElectronicSignatureResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
        val now = Instant.now(clock)
        val organizationId = command.organizationId.trim()
        val actorUserId = command.actorUserId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (actorUserId.isBlank()) throw DomainRuleViolation("Actor user id is required.")
        if (command.content.isEmpty()) throw DomainRuleViolation("Electronic signature content is required.")
        if (command.password.isEmpty()) throw DomainRuleViolation("Electronic signature password is required.")

        val password = command.password.copyOf()
        try {
            val metadata = inspector.inspectPkcs12(command.content, password, command.fileName)
            val signatureId = idGenerator.newId("sig")
            val storageKey = secretStore.storeEncryptedSignature(
                organizationId = organizationId,
                signatureId = signatureId,
                fileName = command.fileName,
                content = command.content,
            )
            val passwordSecretRef = secretStore.storePasswordSecret(
                organizationId = organizationId,
                signatureId = signatureId,
                password = password,
            )
            val signature = ElectronicSignature.upload(
                id = signatureId,
                organizationId = organizationId,
                storageKey = storageKey,
                passwordSecretRef = passwordSecretRef,
                subject = metadata.subject,
                issuer = metadata.issuer,
                validFrom = metadata.validFrom,
                validTo = metadata.validTo,
                uploadedBy = actorUserId,
                uploadedAt = now,
            )
            repository.create(signature)
            return ElectronicSignatureResult(signature = signature, validation = signature.validityReport(now))
        } finally {
            Arrays.fill(password, '\u0000')
        }
    }
}

class ListElectronicSignaturesUseCase(
    private val repository: ElectronicSignatureRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ListElectronicSignaturesCommand): ElectronicSignaturesResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        val now = Instant.now(clock)
        return ElectronicSignaturesResult(
            signatures = repository.findByOrganizationId(organizationId).sortedByDescending { it.uploadedAt },
            checkedAt = now,
        )
    }
}

class GetElectronicSignatureUseCase(
    private val repository: ElectronicSignatureRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: GetElectronicSignatureCommand): ElectronicSignatureResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
        val signature = repository.findById(command.signatureId.trim())
            ?: throw DomainRuleViolation("Electronic signature does not exist.")
        signature.assertBelongsTo(command.organizationId)
        val now = Instant.now(clock)
        return ElectronicSignatureResult(signature = signature, validation = signature.validityReport(now))
    }
}

class ValidateElectronicSignatureUseCase(
    private val repository: ElectronicSignatureRepository,
    private val secretReader: ElectronicSignatureSecretReader,
    private val inspector: SignatureCertificateInspector,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ValidateElectronicSignatureCommand): ElectronicSignatureResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
        val now = Instant.now(clock)
        val signature = repository.findById(command.signatureId.trim())
            ?: throw DomainRuleViolation("Electronic signature does not exist.")
        signature.assertBelongsTo(command.organizationId)

        val content = secretReader.readSignatureContent(signature.storageKey)
        val password = secretReader.readPassword(signature.passwordSecretRef)
        try {
            val metadata = inspector.inspectPkcs12(content, password, "signature.p12")
            if (metadata.subject != signature.subject) {
                throw DomainRuleViolation("Stored signature subject does not match certificate metadata.")
            }
            val validated = if (signature.status == ElectronicSignatureStatus.VALID && signature.effectiveStatus(now) == ElectronicSignatureStatus.VALID) {
                signature
            } else {
                signature.markValidated(now)
            }
            repository.update(validated)
            return ElectronicSignatureResult(signature = validated, validation = validated.validityReport(now))
        } finally {
            Arrays.fill(password, '\u0000')
        }
    }
}

class ActivateElectronicSignatureUseCase(
    private val repository: ElectronicSignatureRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: ActivateElectronicSignatureCommand): ElectronicSignatureResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
        val now = Instant.now(clock)
        val signature = repository.findById(command.signatureId.trim())
            ?: throw DomainRuleViolation("Electronic signature does not exist.")
        signature.assertBelongsTo(command.organizationId)
        val active = repository.findActiveByOrganizationId(command.organizationId.trim())
        if (active != null && active.id != signature.id) {
            repository.update(active.disable())
        }
        val activated = if (signature.status == ElectronicSignatureStatus.VALID && signature.effectiveStatus(now) == ElectronicSignatureStatus.VALID) {
            signature
        } else {
            signature.markValidated(now)
        }
        repository.update(activated)
        return ElectronicSignatureResult(signature = activated, validation = activated.validityReport(now))
    }
}

class RevokeElectronicSignatureUseCase(
    private val repository: ElectronicSignatureRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RevokeElectronicSignatureCommand): ElectronicSignatureResult {
        PermissionRules.assertCanPerform(command.actorEffectivePermissions, PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS)
        val now = Instant.now(clock)
        val signature = repository.findById(command.signatureId.trim())
            ?: throw DomainRuleViolation("Electronic signature does not exist.")
        signature.assertBelongsTo(command.organizationId)
        val revoked = signature.revoke()
        repository.update(revoked)
        return ElectronicSignatureResult(signature = revoked, validation = revoked.validityReport(now))
    }
}

data class UploadElectronicSignatureCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val fileName: String,
    val content: ByteArray,
    val password: CharArray,
)

data class ListElectronicSignaturesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
)

data class GetElectronicSignatureCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val signatureId: String,
)

data class ValidateElectronicSignatureCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val signatureId: String,
)

data class ActivateElectronicSignatureCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val signatureId: String,
)

data class RevokeElectronicSignatureCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val signatureId: String,
)

data class ElectronicSignatureResult(
    val signature: ElectronicSignature,
    val validation: SignatureValidityReport,
)

data class ElectronicSignaturesResult(
    val signatures: List<ElectronicSignature>,
    val checkedAt: Instant,
)

private fun ElectronicSignature.assertBelongsTo(organizationId: String) {
    if (this.organizationId != organizationId.trim()) {
        throw DomainRuleViolation("Electronic signature does not belong to requested organization.")
    }
}

private fun ElectronicSignature.validityReport(now: Instant): SignatureValidityReport {
    val effectiveStatus = effectiveStatus(now)
    val days = Duration.between(now, validTo).toDays()
    return SignatureValidityReport(
        storedStatus = status,
        effectiveStatus = effectiveStatus,
        validFrom = validFrom,
        validTo = validTo,
        daysUntilExpiration = days,
        expiresSoon = days in 0..30,
    )
}
