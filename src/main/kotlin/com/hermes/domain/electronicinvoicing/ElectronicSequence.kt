package com.hermes.domain.electronicinvoicing

import com.hermes.domain.shared.DomainRuleViolation
import java.time.Instant

enum class ElectronicSequenceStatus(
    val storageValue: String,
) {
    ACTIVE("active"),
    INACTIVE("inactive"),
    ARCHIVED("archived");

    companion object {
        fun fromStorage(value: String): ElectronicSequenceStatus {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { it.storageValue == normalized }
                ?: throw DomainRuleViolation("Unknown electronic sequence status: $value.")
        }
    }
}

/**
 * Atomic SRI sequence cursor for one organization + environment + document type + series.
 *
 * currentValue stores the last issued sequential number. A brand new sequence starts at 0,
 * so the first reservation returns 000000001.
 */
data class ElectronicSequence(
    val id: String,
    val organizationId: String,
    val environment: SriEnvironment,
    val documentType: SriDocumentType,
    val series: SriSeries,
    val currentValue: Int,
    val status: ElectronicSequenceStatus = ElectronicSequenceStatus.ACTIVE,
    val lastIssuedDocumentId: String? = null,
    val lastIssuedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int = 1,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    init {
        if (id.isBlank()) throw DomainRuleViolation("Electronic sequence id cannot be blank.")
        if (organizationId.isBlank()) throw DomainRuleViolation("Electronic sequence organization id cannot be blank.")
        if (currentValue !in MIN_CURRENT_VALUE..SriSequential.MAX_VALUE) {
            throw DomainRuleViolation("Electronic sequence current value must be between $MIN_CURRENT_VALUE and ${SriSequential.MAX_VALUE}.")
        }
        lastIssuedDocumentId?.let {
            if (it.isBlank()) throw DomainRuleViolation("Electronic sequence last issued document id cannot be blank when provided.")
        }
        if (version < 1) throw DomainRuleViolation("Electronic sequence version must be at least 1.")
        if (schemaVersion < 1) throw DomainRuleViolation("Electronic sequence schema version must be at least 1.")
    }

    val key: ElectronicSequenceKey
        get() = ElectronicSequenceKey(
            organizationId = organizationId,
            environment = environment,
            documentType = documentType,
            series = series,
        )

    val isActive: Boolean get() = status == ElectronicSequenceStatus.ACTIVE
    val lastIssuedSequential: SriSequential? get() = currentValue.takeIf { it > 0 }?.let(::SriSequential)

    fun nextSequential(): SriSequential {
        assertActive()
        if (currentValue >= SriSequential.MAX_VALUE) {
            throw DomainRuleViolation("Electronic sequence ${key.storageKey} is exhausted.")
        }
        return SriSequential(currentValue + 1)
    }

    fun markIssued(
        sequential: SriSequential,
        documentId: String?,
        issuedAt: Instant,
    ): ElectronicSequence {
        assertActive()
        if (sequential.value != currentValue + 1) {
            throw DomainRuleViolation("Electronic sequence must be issued in strict order. Expected ${currentValue + 1}, got ${sequential.value}.")
        }
        return copy(
            currentValue = sequential.value,
            lastIssuedDocumentId = documentId?.trim()?.takeIf { it.isNotBlank() },
            lastIssuedAt = issuedAt,
            updatedAt = issuedAt,
            version = version + 1,
        )
    }

    fun deactivate(now: Instant): ElectronicSequence = copy(
        status = ElectronicSequenceStatus.INACTIVE,
        updatedAt = now,
        version = version + 1,
    )

    fun archive(now: Instant): ElectronicSequence = copy(
        status = ElectronicSequenceStatus.ARCHIVED,
        updatedAt = now,
        version = version + 1,
    )

    fun assertActive() {
        if (!isActive) {
            throw DomainRuleViolation("Electronic sequence ${key.storageKey} is not active.")
        }
    }

    companion object {
        const val SCHEMA_VERSION: Int = 1
        const val MIN_CURRENT_VALUE: Int = 0

        fun create(
            id: String,
            organizationId: String,
            environment: SriEnvironment,
            documentType: SriDocumentType,
            series: SriSeries,
            startsAfter: Int = 0,
            now: Instant,
        ): ElectronicSequence {
            if (startsAfter !in MIN_CURRENT_VALUE until SriSequential.MAX_VALUE) {
                throw DomainRuleViolation("Electronic sequence startsAfter must be between 0 and ${SriSequential.MAX_VALUE - 1}.")
            }
            return ElectronicSequence(
                id = id,
                organizationId = organizationId.trim(),
                environment = environment,
                documentType = documentType,
                series = series,
                currentValue = startsAfter,
                status = ElectronicSequenceStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

data class ElectronicSequenceKey(
    val organizationId: String,
    val environment: SriEnvironment,
    val documentType: SriDocumentType,
    val series: SriSeries,
) {
    init {
        if (organizationId.isBlank()) throw DomainRuleViolation("Electronic sequence key organization id cannot be blank.")
    }

    val storageKey: String
        get() = listOf(
            organizationId.trim(),
            environment.storageValue,
            documentType.storageValue,
            series.establishmentCode,
            series.emissionPointCode,
        ).joinToString(":")
}

data class ElectronicSequenceReservation(
    val sequence: ElectronicSequence,
    val sequential: SriSequential,
) {
    val series: SriSeries get() = sequence.series
    val documentNumber: String get() = "${series.displayValue}-${sequential.formatted}"
}
