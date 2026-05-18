package com.hermes.application.electronicinvoicing

import java.time.Instant

enum class ElectronicSignatureUsageAuditAction {
    ELECTRONIC_SIGNATURE_USED,
    ELECTRONIC_SIGNATURE_FAILED,
}

data class ElectronicSignatureUsageAuditEvent(
    val action: ElectronicSignatureUsageAuditAction,
    val organizationId: String,
    val signatureId: String,
    val documentId: String?,
    val accessKey: String?,
    val message: String? = null,
    val createdAt: Instant,
)

interface ElectronicSignatureUsageAuditLogger {
    fun log(event: ElectronicSignatureUsageAuditEvent)
}

object NoopElectronicSignatureUsageAuditLogger : ElectronicSignatureUsageAuditLogger {
    override fun log(event: ElectronicSignatureUsageAuditEvent) = Unit
}
