package com.hermes.backend.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val checks: List<HealthCheckResult>,
)

@Serializable
data class HealthCheckResult(
    val name: String,
    val status: String,
    val message: String,
    val latencyMs: Long? = null,
) {
    companion object {
        fun up(name: String, message: String, latencyMs: Long? = null): HealthCheckResult =
            HealthCheckResult(name = name, status = "UP", message = message, latencyMs = latencyMs)

        fun down(name: String, message: String, latencyMs: Long? = null): HealthCheckResult =
            HealthCheckResult(name = name, status = "DOWN", message = message, latencyMs = latencyMs)
    }
}
