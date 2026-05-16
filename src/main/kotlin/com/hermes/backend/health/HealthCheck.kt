package com.hermes.backend.health

fun interface HealthCheck {
    suspend fun check(): HealthCheckResult
}
