package com.hermes.backend.shared

import com.hermes.backend.auth.AuthModule
import com.hermes.backend.auth.AuthModuleFactory
import com.hermes.backend.catalog.CatalogModule
import com.hermes.backend.catalog.CatalogModuleFactory
import com.hermes.backend.config.AppConfig
import com.hermes.backend.electronicinvoicing.ElectronicInvoicingModule
import com.hermes.backend.electronicinvoicing.ElectronicInvoicingModuleFactory
import com.hermes.backend.health.ApplicationHealthCheck
import com.hermes.backend.health.HealthCheck
import com.hermes.backend.health.MinioHealthCheck
import com.hermes.backend.health.MongoHealthCheck
import com.hermes.backend.health.RedisHealthCheck
import com.hermes.backend.payments.PaymentsModule
import com.hermes.backend.payments.PaymentsModuleFactory
import com.hermes.backend.sales.ReservationSchedulingModule
import com.hermes.backend.sales.ReservationSchedulingModuleFactory
import com.hermes.backend.sales.SalesModule
import com.hermes.backend.sales.SalesModuleFactory
import com.hermes.backend.tax.TaxModule
import com.hermes.backend.tax.TaxModuleFactory
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.minio.MinioClient
import java.io.Closeable

interface AppResources : Closeable {
    val healthChecks: List<HealthCheck>
    val authModule: AuthModule
    val taxModule: TaxModule
    val catalogModule: CatalogModule
    val salesModule: SalesModule
    val reservationSchedulingModule: ReservationSchedulingModule
    val paymentsModule: PaymentsModule
    val electronicInvoicingModule: ElectronicInvoicingModule
}

class DefaultAppResources private constructor(
    private val mongoClient: MongoClient,
    private val redisClient: RedisClient,
    private val redisConnection: StatefulRedisConnection<String, String>,
    override val healthChecks: List<HealthCheck>,
    override val authModule: AuthModule,
    override val taxModule: TaxModule,
    override val catalogModule: CatalogModule,
    override val salesModule: SalesModule,
    override val reservationSchedulingModule: ReservationSchedulingModule,
    override val paymentsModule: PaymentsModule,
    override val electronicInvoicingModule: ElectronicInvoicingModule,
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
                MongoHealthCheck(mongoDatabase),
                RedisHealthCheck(redisConnection),
                MinioHealthCheck(client = minioClient, bucket = config.minio.healthBucket),
            )
            val authModule = AuthModuleFactory.fromMongo(client = mongoClient, database = mongoDatabase, config = config)
            val taxModule = TaxModuleFactory.fromMongo(database = mongoDatabase)
            val catalogModule = CatalogModuleFactory.fromMongo(database = mongoDatabase)
            val salesModule = SalesModuleFactory.fromMongo(database = mongoDatabase)
            val reservationSchedulingModule = ReservationSchedulingModuleFactory.fromMongo(database = mongoDatabase)
            val paymentsModule = PaymentsModuleFactory.fromMongo(client = mongoClient, database = mongoDatabase)
            val electronicInvoicingModule = ElectronicInvoicingModuleFactory.fromMongo(database = mongoDatabase)

            return DefaultAppResources(
                mongoClient = mongoClient,
                redisClient = redisClient,
                redisConnection = redisConnection,
                healthChecks = checks,
                authModule = authModule,
                taxModule = taxModule,
                catalogModule = catalogModule,
                salesModule = salesModule,
                reservationSchedulingModule = reservationSchedulingModule,
                paymentsModule = paymentsModule,
                electronicInvoicingModule = electronicInvoicingModule,
            )
        }
    }

    override fun close() {
        runCatching { redisConnection.close() }
        runCatching { redisClient.shutdown() }
        runCatching { mongoClient.close() }
    }
}
