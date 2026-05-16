package com.hermes.backend.health

class ApplicationHealthCheck : HealthCheck {
    override suspend fun check(): HealthCheckResult =
        HealthCheckResult.up(
            name = "application",
            message = "Application is running",
            latencyMs = 0,
        )
}
