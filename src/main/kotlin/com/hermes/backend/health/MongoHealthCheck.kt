package com.hermes.backend.health

import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import kotlin.system.measureTimeMillis

class MongoHealthCheck(
    private val database: MongoDatabase,
) : HealthCheck {

    override suspend fun check(): HealthCheckResult {
        var message = "MongoDB ping OK"

        val latency = measureTimeMillis {
            try {
                withContext(Dispatchers.IO) {
                    database.runCommand(Document("ping", 1))
                }
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