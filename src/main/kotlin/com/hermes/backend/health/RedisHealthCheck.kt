package com.hermes.backend.health

import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class RedisHealthCheck(
    private val connection: StatefulRedisConnection<String, String>,
) : HealthCheck {
    override suspend fun check(): HealthCheckResult {
        var message = "Redis ping OK"
        val latency = measureTimeMillis {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    connection.sync().ping()
                }
            }.getOrElse { error ->
                message = error.message ?: "Redis ping failed"
                return HealthCheckResult.down(
                    name = "redis",
                    message = message,
                )
            }

            if (!result.equals("PONG", ignoreCase = true)) {
                return HealthCheckResult.down(
                    name = "redis",
                    message = "Unexpected Redis response: $result",
                )
            }
        }

        return HealthCheckResult.up(
            name = "redis",
            message = message,
            latencyMs = latency,
        )
    }
}
