package com.hermes.backend.health

import java.time.Instant

class HealthService(
    private val checks: List<HealthCheck>,
) {
    suspend fun check(): HealthResponse {
        val results = checks.map { check -> check.check() }
        val status = when {
            results.all { it.status == "UP" } -> "UP"
            results.any { it.status == "UP" } -> "DEGRADED"
            else -> "DOWN"
        }

        return HealthResponse(
            status = status,
            timestamp = Instant.now().toString(),
            checks = results,
        )
    }
}
