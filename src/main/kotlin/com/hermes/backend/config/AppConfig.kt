package com.hermes.backend.config

data class AppConfig(
    val app: AppInfo,
    val server: ServerConfig,
    val mongo: MongoConfig,
    val redis: RedisConfig,
    val minio: MinioConfig,
    val cors: CorsConfig,
) {
    companion object {
        fun loadFromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            val environment = env.valueOrDefault("APP_ENV", "local")
            return AppConfig(
                app = AppInfo(
                    name = env.valueOrDefault("APP_NAME", "Hermes Business Platform API"),
                    version = env.valueOrDefault("APP_VERSION", "0.1.0"),
                    environment = environment,
                    buildTime = env.valueOrDefault("BUILD_TIME", "local"),
                    commitSha = env.valueOrDefault("COMMIT_SHA", "local"),
                ),
                server = ServerConfig(
                    host = env.valueOrDefault("HOST", "0.0.0.0"),
                    port = env.valueOrDefault("PORT", "8080").toIntStrict("PORT"),
                ),
                mongo = MongoConfig(
                    uri = env.valueOrDefault(
                        "MONGODB_URI",
                        "mongodb://localhost:27017/hermes_local?replicaSet=rs0",
                    ),
                    database = env.valueOrDefault("MONGODB_DATABASE", "hermes_local"),
                ),
                redis = RedisConfig(
                    uri = env.valueOrDefault("REDIS_URI", "redis://localhost:6379/0"),
                ),
                minio = MinioConfig(
                    endpoint = env.valueOrDefault("MINIO_ENDPOINT", "http://localhost:9000"),
                    accessKey = env.valueOrDefault("MINIO_ACCESS_KEY", "hermes_minio"),
                    secretKey = env.valueOrDefault("MINIO_SECRET_KEY", "hermes_minio_secret"),
                    healthBucket = env.valueOrDefault("MINIO_HEALTH_BUCKET", "hermes-health"),
                ),
                cors = CorsConfig(
                    anyHost = env.valueOrDefault("CORS_ANY_HOST", (environment == "local").toString()).toBooleanStrictOrNull() ?: true,
                    allowedHosts = env.valueOrDefault(
                        "CORS_ALLOWED_HOSTS",
                        "localhost:3000,localhost:8081,127.0.0.1:3000",
                    ).split(',').map { it.trim() }.filter { it.isNotBlank() },
                ),
            )
        }

        fun test(): AppConfig = loadFromEnvironment(
            mapOf(
                "APP_ENV" to "test",
                "APP_VERSION" to "0.1.0-test",
                "BUILD_TIME" to "test",
                "COMMIT_SHA" to "test",
                "PORT" to "8080",
                "MONGODB_URI" to "mongodb://localhost:27017/hermes_test?replicaSet=rs0",
                "MONGODB_DATABASE" to "hermes_test",
                "REDIS_URI" to "redis://localhost:6379/1",
                "MINIO_ENDPOINT" to "http://localhost:9000",
            ),
        )
    }
}

data class AppInfo(
    val name: String,
    val version: String,
    val environment: String,
    val buildTime: String,
    val commitSha: String,
)

data class ServerConfig(
    val host: String,
    val port: Int,
)

data class MongoConfig(
    val uri: String,
    val database: String,
)

data class RedisConfig(
    val uri: String,
)

data class MinioConfig(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val healthBucket: String,
)

data class CorsConfig(
    val anyHost: Boolean,
    val allowedHosts: List<String>,
)

private fun Map<String, String>.valueOrDefault(key: String, default: String): String =
    this[key]?.takeIf { it.isNotBlank() } ?: default

private fun String.toIntStrict(name: String): Int =
    toIntOrNull() ?: error("Environment variable $name must be a valid integer. Current value: $this")
