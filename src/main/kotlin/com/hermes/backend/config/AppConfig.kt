package com.hermes.backend.config

data class AppConfig(
    val app: AppInfo,
    val server: ServerConfig,
    val mongo: MongoConfig,
    val redis: RedisConfig,
    val minio: MinioConfig,
    val cors: CorsConfig,
    val auth: AuthConfig,
) {
    companion object {
        fun loadFromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            val environment = env.valueOrDefault("APP_ENV", "local")
            val isLocal = environment == "local" || environment == "test"

            val corsAnyHost = env.optionalBoolean("CORS_ANY_HOST") ?: isLocal
            if (!isLocal && corsAnyHost) {
                error("CORS_ANY_HOST=true is not allowed outside local/test.")
            }

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
                    anyHost = corsAnyHost,
                    allowedHosts = env.valueOrDefault(
                        "CORS_ALLOWED_HOSTS",
                        "localhost:3000,localhost:8081,127.0.0.1:3000",
                    ).split(',').map { it.trim() }.filter { it.isNotBlank() },
                ),
                auth = AuthConfig(
                    jwtSecret = env.requiredSecretOrLocalDefault(
                        key = "JWT_SECRET",
                        isLocal = isLocal,
                        localDefault = "local-development-jwt-secret-change-me-please-32chars",
                    ),
                    jwtIssuer = env.valueOrDefault("JWT_ISSUER", "hermes-api"),
                    accessTokenTtlSeconds = env.valueOrDefault("ACCESS_TOKEN_TTL_SECONDS", "900")
                        .toLongStrict("ACCESS_TOKEN_TTL_SECONDS"),
                    refreshTokenTtlDays = env.valueOrDefault("REFRESH_TOKEN_TTL_DAYS", "30")
                        .toLongStrict("REFRESH_TOKEN_TTL_DAYS"),
                    sessionTtlDays = env.valueOrDefault("SESSION_TTL_DAYS", "30").toLongStrict("SESSION_TTL_DAYS"),
                    maxFailedLoginAttempts = env.valueOrDefault("MAX_FAILED_LOGIN_ATTEMPTS", "5")
                        .toIntStrict("MAX_FAILED_LOGIN_ATTEMPTS"),
                    credentialLockDurationMinutes = env.valueOrDefault("CREDENTIAL_LOCK_MINUTES", "15")
                        .toLongStrict("CREDENTIAL_LOCK_MINUTES"),
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
                "CORS_ANY_HOST" to "true",
                "JWT_SECRET" to "test-jwt-secret-for-hermes-auth-tests-32chars-minimum",
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

data class AuthConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlDays: Long,
    val sessionTtlDays: Long,
    val maxFailedLoginAttempts: Int,
    val credentialLockDurationMinutes: Long,
) {
    init {
        require(jwtSecret.length >= 32) { "JWT secret must contain at least 32 characters." }
        require(jwtIssuer.isNotBlank()) { "JWT issuer cannot be blank." }
        require(accessTokenTtlSeconds > 0) { "Access token TTL must be positive." }
        require(refreshTokenTtlDays > 0) { "Refresh token TTL days must be positive." }
        require(sessionTtlDays > 0) { "Session TTL days must be positive." }
        require(maxFailedLoginAttempts >= 1) { "Max failed login attempts must be at least 1." }
        require(credentialLockDurationMinutes > 0) { "Credential lock duration must be positive." }
    }
}

private fun Map<String, String>.valueOrDefault(key: String, default: String): String =
    this[key]?.takeIf { it.isNotBlank() } ?: default

private fun Map<String, String>.requiredSecretOrLocalDefault(
    key: String,
    isLocal: Boolean,
    localDefault: String,
): String {
    val value = this[key]?.takeIf { it.isNotBlank() }
    if (value != null) return value
    if (isLocal) return localDefault
    error("Environment variable $key is required outside local/test.")
}

private fun Map<String, String>.optionalBoolean(key: String): Boolean? {
    val raw = this[key]?.takeIf { it.isNotBlank() } ?: return null
    return raw.toBooleanStrictOrNull()
        ?: error("Environment variable $key must be true or false. Current value: $raw")
}

private fun String.toIntStrict(name: String): Int =
    toIntOrNull() ?: error("Environment variable $name must be a valid integer. Current value: $this")

private fun String.toLongStrict(name: String): Long =
    toLongOrNull() ?: error("Environment variable $name must be a valid long. Current value: $this")
