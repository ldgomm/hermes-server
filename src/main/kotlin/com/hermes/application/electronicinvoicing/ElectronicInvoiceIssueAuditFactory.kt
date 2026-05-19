package com.hermes.application.electronicinvoicing

import java.time.Instant

internal fun ElectronicInvoiceIssueRecord.toIssueAuditEvent(
    action: ElectronicInvoiceIssueAuditAction,
    actorUserId: String?,
    now: Instant,
    message: String? = null,
): ElectronicInvoiceIssueAuditEvent = ElectronicInvoiceIssueAuditEvent(
    action = action,
    actorUserId = actorUserId,
    organizationId = organizationId,
    documentId = id,
    saleId = saleId,
    accessKey = accessKey.value,
    status = status,
    message = message,
    createdAt = now,
)
