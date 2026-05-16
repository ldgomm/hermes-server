package com.hermes.backend.shared

import com.hermes.backend.config.AppConfig
import com.hermes.backend.health.*
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.minio.MinioClient
import java.io.Closeable

interface AppResources : Closeable {
    val healthChecks: List<HealthCheck>
}

class DefaultAppResources private constructor(
    private val mongoClient: MongoClient,
    private val redisClient: RedisClient,
    private val redisConnection: StatefulRedisConnection<String, String>,
    override val healthChecks: List<HealthCheck>,
) : AppResources {
    companion object {
        fun start(config: AppConfig): DefaultAppResources {
            val mongoClient = MongoClient.create(config.mongo.uri) //Unresolved reference 'create'.
            val mongoDatabase = mongoClient.getDatabase(config.mongo.database)

            val redisClient = RedisClient.create(config.redis.uri)
            val redisConnection = redisClient.connect()

            val minioClient = MinioClient.builder()
                .endpoint(config.minio.endpoint)
                .credentials(config.minio.accessKey, config.minio.secretKey)
                .build()

            val checks = listOf(
                ApplicationHealthCheck(),
                MongoHealthCheck(mongoDatabase),
                RedisHealthCheck(redisConnection),
                MinioHealthCheck(
                    client = minioClient,
                    bucket = config.minio.healthBucket,
                ),
            )

            return DefaultAppResources(
                mongoClient = mongoClient,
                redisClient = redisClient,
                redisConnection = redisConnection,
                healthChecks = checks,
            )
        }
    }

    override fun close() {
        runCatching { redisConnection.close() }
        runCatching { redisClient.shutdown() }
        runCatching { mongoClient.close() }
    }
}
