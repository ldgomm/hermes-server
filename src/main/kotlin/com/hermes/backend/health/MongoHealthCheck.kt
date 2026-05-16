package com.hermes.backend.health

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document
import kotlin.system.measureTimeMillis

class MongoHealthCheck(
    private val database: MongoDatabase,
) : HealthCheck {

    override suspend fun check(): HealthCheckResult {
        var message = "MongoDB ping OK"

        val latency = measureTimeMillis {
            try {
                database.runCommand<Document>(Document("ping", 1))
            } catch (error: Throwable) {
                message = error.message ?: "MongoDB ping failed"

                return HealthCheckResult.down(
                    name = "mongodb",
                    message = message,
                )
            }
        }

        return HealthCheckResult.up(
            name = "mongodb",
            message = message,
            latencyMs = latency,
        )
    }
}