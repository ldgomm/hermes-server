package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionRules
import com.hermes.domain.shared.DomainRuleViolation
import java.time.Clock
import java.time.Instant
import java.util.*

fun interface ElectronicSequenceIdGenerator {
    fun newId(prefix: String): String
}

class UuidElectronicSequenceIdGenerator : ElectronicSequenceIdGenerator {
    override fun newId(prefix: String): String =
        prefix.trim().lowercase() + "_" + UUID.randomUUID().toString().replace("-", "")
}

data class ElectronicSequenceSearchQuery(
    val organizationId: String,
    val environment: SriEnvironment? = null,
    val documentType: SriDocumentType? = null,
    val status: ElectronicSequenceStatus? = null,
    val limit: Int = 100,
)

interface ElectronicSequenceQueryRepository {
    fun findById(organizationId: String, sequenceId: String): ElectronicSequence?
    fun search(query: ElectronicSequenceSearchQuery): List<ElectronicSequence>
}

class EnsureElectronicSequenceAdminUseCase(
    private val repository: ElectronicSequenceRepository,
    private val idGenerator: ElectronicSequenceIdGenerator = UuidElectronicSequenceIdGenerator(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: EnsureElectronicSequenceAdminCommand): ElectronicSequenceResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS
        )
        command.documentType.assertMvpSupported()
        val now = Instant.now(clock)
        val sequence = ElectronicSequence.create(
            id = idGenerator.newId("seq"),
            organizationId = command.organizationId,
            environment = command.environment,
            documentType = command.documentType,
            series = command.series,
            startsAfter = command.startsAfter,
            now = now,
        )
        return ElectronicSequenceResult(repository.createIfMissing(sequence))
    }
}

class ListElectronicSequencesUseCase(
    private val repository: ElectronicSequenceQueryRepository,
) {
    fun execute(command: ListElectronicSequencesCommand): ElectronicSequencesResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS
        )
        val organizationId = command.organizationId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        return ElectronicSequencesResult(
            repository.search(
                ElectronicSequenceSearchQuery(
                    organizationId = organizationId,
                    environment = command.environment,
                    documentType = command.documentType,
                    status = command.status,
                    limit = command.limit.coerceIn(1, 200),
                )
            )
        )
    }
}

class GetElectronicSequenceUseCase(
    private val repository: ElectronicSequenceQueryRepository,
) {
    fun execute(command: GetElectronicSequenceCommand): ElectronicSequenceResult {
        PermissionRules.assertCanPerform(
            command.actorEffectivePermissions,
            PermissionCatalog.DOCUMENTS_ELECTRONIC_INVOICE_MANAGE_SETTINGS
        )
        val organizationId = command.organizationId.trim()
        val sequenceId = command.sequenceId.trim()
        if (organizationId.isBlank()) throw DomainRuleViolation("Organization id is required.")
        if (sequenceId.isBlank()) throw DomainRuleViolation("Electronic sequence id is required.")
        val sequence = repository.findById(organizationId, sequenceId)
            ?: throw DomainRuleViolation("Electronic sequence does not exist.")
        return ElectronicSequenceResult(sequence)
    }
}

data class EnsureElectronicSequenceAdminCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val environment: SriEnvironment,
    val documentType: SriDocumentType = SriDocumentType.INVOICE,
    val series: SriSeries,
    val startsAfter: Int = 0,
)

data class ListElectronicSequencesCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val environment: SriEnvironment? = null,
    val documentType: SriDocumentType? = null,
    val status: ElectronicSequenceStatus? = null,
    val limit: Int = 100,
)

data class GetElectronicSequenceCommand(
    val organizationId: String,
    val actorUserId: String,
    val actorEffectivePermissions: Set<String>,
    val sequenceId: String,
)

data class ElectronicSequencesResult(
    val sequences: List<ElectronicSequence>,
)
