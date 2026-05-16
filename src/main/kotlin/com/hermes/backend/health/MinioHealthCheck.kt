package com.hermes.backend.health

import io.minio.BucketExistsArgs
import io.minio.MinioClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class MinioHealthCheck(
    private val client: MinioClient,
    private val bucket: String,
) : HealthCheck {
    override suspend fun check(): HealthCheckResult {
        var message = "MinIO bucket exists"
        val latency = measureTimeMillis {
            val exists = runCatching {
                withContext(Dispatchers.IO) {
                    client.bucketExists(
                        BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build(),
                    )
                }
            }.getOrElse { error ->
                message = error.message ?: "MinIO check failed"
                return HealthCheckResult.down(
                    name = "minio",
                    message = message,
                )
            }

            if (!exists) {
                return HealthCheckResult.down(
                    name = "minio",
                    message = "MinIO bucket '$bucket' does not exist",
                )
            }
        }

        return HealthCheckResult.up(
            name = "minio",
            message = message,
            latencyMs = latency,
        )
    }
}
