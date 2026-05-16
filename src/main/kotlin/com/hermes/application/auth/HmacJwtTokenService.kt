package com.hermes.application.auth

import com.hermes.domain.shared.DomainRuleViolation
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class HmacJwtTokenService(
    secret: String,
    private val issuer: String = "hermes-api",
    private val accessTokenTtlSeconds: Long = 900,
) : JwtTokenService {

    private val secretBytes: ByteArray = secret.toByteArray(StandardCharsets.UTF_8)

    init {
        require(secret.length >= 32) { "JWT secret must contain at least 32 characters." }
        require(issuer.isNotBlank()) { "JWT issuer cannot be blank." }
        require(accessTokenTtlSeconds > 0) { "Access token TTL must be positive." }
    }

    override fun issueAccessToken(userId: String, sessionId: String, issuedAt: Instant): JwtAccessToken {
        if (userId.isBlank()) throw DomainRuleViolation("JWT user id cannot be blank.")
        if (sessionId.isBlank()) throw DomainRuleViolation("JWT session id cannot be blank.")

        val expiresAt = issuedAt.plusSeconds(accessTokenTtlSeconds)
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload = buildString {
            append('{')
            append("\"iss\":\"").append(jsonEscape(issuer)).append("\",")
            append("\"sub\":\"").append(jsonEscape(userId)).append("\",")
            append("\"sid\":\"").append(jsonEscape(sessionId)).append("\",")
            append("\"iat\":").append(issuedAt.epochSecond).append(',')
            append("\"exp\":").append(expiresAt.epochSecond)
            append('}')
        }

        val signingInput = base64Url(header.toByteArray(StandardCharsets.UTF_8)) + "." +
            base64Url(payload.toByteArray(StandardCharsets.UTF_8))
        val signature = base64Url(hmacSha256(signingInput))

        return JwtAccessToken(
            token = "$signingInput.$signature",
            expiresAt = expiresAt,
        )
    }

    override fun validateAccessToken(token: String, now: Instant): JwtClaims {
        val parts = token.split('.')
        if (parts.size != 3) throw DomainRuleViolation("Invalid JWT format.")

        val signingInput = parts[0] + "." + parts[1]
        val expectedSignature = base64Url(hmacSha256(signingInput))
        if (!MessageDigest.isEqual(expectedSignature.toByteArray(), parts[2].toByteArray())) {
            throw DomainRuleViolation("Invalid JWT signature.")
        }

        val payload = runCatching {
            String(decoder.decode(parts[1]), StandardCharsets.UTF_8)
        }.getOrElse {
            throw DomainRuleViolation("Invalid JWT payload.")
        }

        val tokenIssuer = extractString(payload, "iss")
        if (tokenIssuer != issuer) throw DomainRuleViolation("Invalid JWT issuer.")

        val userId = extractString(payload, "sub")
        val sessionId = extractString(payload, "sid")
        val issuedAt = Instant.ofEpochSecond(extractLong(payload, "iat"))
        val expiresAt = Instant.ofEpochSecond(extractLong(payload, "exp"))

        if (!expiresAt.isAfter(now)) {
            throw DomainRuleViolation("JWT access token has expired.")
        }

        return JwtClaims(
            userId = userId,
            sessionId = sessionId,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            issuer = tokenIssuer,
        )
    }

    private fun hmacSha256(value: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secretBytes, "HmacSHA256"))
        return mac.doFinal(value.toByteArray(StandardCharsets.UTF_8))
    }

    private fun extractString(json: String, key: String): String {
        val regex = Regex("\\\"$key\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
        return regex.find(json)?.groupValues?.get(1)
            ?: throw DomainRuleViolation("JWT claim $key is missing.")
    }

    private fun extractLong(json: String, key: String): Long {
        val regex = Regex("\\\"$key\\\"\\s*:\\s*(\\d+)")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
            ?: throw DomainRuleViolation("JWT claim $key is missing or invalid.")
    }

    private fun jsonEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun base64Url(bytes: ByteArray): String = encoder.encodeToString(bytes)

    companion object {
        private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}
