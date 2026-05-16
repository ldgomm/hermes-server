package com.hermes.backend.shared

import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.AuthModuleFactory
import com.hermes.backend.config.AppConfig
import com.hermes.backend.health.*
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.minio.MinioClient
import java.io.Closeable

interface AppResources : Closeable {
    val healthChecks: List<HealthCheck>
    val authModule: AuthModule
}

class DefaultAppResources private constructor(
    private val mongoClient: MongoClient,
    private val redisClient: RedisClient,
    private val redisConnection: StatefulRedisConnection<String, String>,
    override val healthChecks: List<HealthCheck>,
    override val authModule: AuthModule,
) : AppResources {
    companion object {
        fun start(config: AppConfig): DefaultAppResources {
            val mongoClient = MongoClients.create(config.mongo.uri)
            val mongoDatabase = mongoClient.getDatabase(config.mongo.database)

            val redisClient = RedisClient.create(config.redis.uri)
            val redisConnection = redisClient.connect()

            val minioClient = MinioClient.builder()
                .endpoint(config.minio.endpoint)
                .credentials(config.minio.accessKey, config.minio.secretKey)
                .build()

            val checks = listOf(
                ApplicationHealthCheck(),
                MongoHealthCheck(mongoDatabase), // Argument type mismatch: actual type is 'com.mongodb.client.MongoDatabase!', but 'com.mongodb.kotlin.client.coroutine.MongoDatabase' was expected.
                RedisHealthCheck(redisConnection),
                MinioHealthCheck(
                    client = minioClient,
                    bucket = config.minio.healthBucket,
                ),
            )

            val authModule = AuthModuleFactory.fromMongo(
                client = mongoClient,
                database = mongoDatabase,
                config = config,
            )

            return DefaultAppResources(
                mongoClient = mongoClient,
                redisClient = redisClient,
                redisConnection = redisConnection,
                healthChecks = checks,
                authModule = authModule,
            )
        }
    }

    override fun close() {
        runCatching { redisConnection.close() }
        runCatching { redisClient.shutdown() }
        runCatching { mongoClient.close() }
    }
}
