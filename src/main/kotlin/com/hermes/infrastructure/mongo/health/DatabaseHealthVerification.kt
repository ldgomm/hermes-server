package com.hermes.infrastructure.mongo.health

data class DatabaseHealthVerification(
    val ok: Boolean,
    val databaseName: String,
    val latencyMs: Long,
    val mongoVersion: String?,
    val replicaSetName: String?,
    val isWritablePrimary: Boolean?,
    val supportsSessions: Boolean,
    val transactionProbe: TransactionProbeResult,
    val message: String,
)

enum class TransactionProbeResult {
    NOT_REQUESTED, SUPPORTED, UNSUPPORTED, FAILED,
}
