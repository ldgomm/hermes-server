package com.hermes.application.auth

import com.hermes.domain.shared.DomainRuleViolation

data class PasswordPolicy(
    val minLength: Int = 12,
    val maxLength: Int = 128,
    val requireLowercase: Boolean = true,
    val requireUppercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSymbol: Boolean = true,
) {
    init {
        require(minLength >= 8) { "Password minLength must be at least 8." }
        require(maxLength >= minLength) { "Password maxLength must be greater than or equal to minLength." }
    }

    fun validate(password: String, email: String? = null, displayName: String? = null): PasswordValidationResult {
        val failures = buildList {
            if (password.length < minLength) add(PasswordFailure.TOO_SHORT)
            if (password.length > maxLength) add(PasswordFailure.TOO_LONG)
            if (password.any { it.isWhitespace() }) add(PasswordFailure.CONTAINS_WHITESPACE)
            if (requireLowercase && password.none { it.isLowerCase() }) add(PasswordFailure.MISSING_LOWERCASE)
            if (requireUppercase && password.none { it.isUpperCase() }) add(PasswordFailure.MISSING_UPPERCASE)
            if (requireDigit && password.none { it.isDigit() }) add(PasswordFailure.MISSING_DIGIT)
            if (requireSymbol && password.none { !it.isLetterOrDigit() && !it.isWhitespace() }) {
                add(PasswordFailure.MISSING_SYMBOL)
            }

            val normalizedPassword = password.lowercase()
            val emailLocalPart = email
                ?.substringBefore('@')
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.length >= 4 }

            if (emailLocalPart != null && normalizedPassword.contains(emailLocalPart)) {
                add(PasswordFailure.CONTAINS_EMAIL_LOCAL_PART)
            }

            val meaningfulNameParts = displayName
                ?.lowercase()
                ?.split(Regex("[^a-z0-9áéíóúñ]+"))
                ?.filter { it.length >= 4 }
                .orEmpty()

            if (meaningfulNameParts.any { normalizedPassword.contains(it) }) {
                add(PasswordFailure.CONTAINS_DISPLAY_NAME)
            }
        }

        return PasswordValidationResult(failures)
    }

    fun assertValid(password: String, email: String? = null, displayName: String? = null) {
        val result = validate(password = password, email = email, displayName = displayName)
        if (!result.valid) {
            throw DomainRuleViolation("Password does not satisfy policy: ${result.failures.joinToString()}.")
        }
    }
}

data class PasswordValidationResult(
    val failures: List<PasswordFailure>,
) {
    val valid: Boolean get() = failures.isEmpty()
}

enum class PasswordFailure {
    TOO_SHORT,
    TOO_LONG,
    CONTAINS_WHITESPACE,
    MISSING_LOWERCASE,
    MISSING_UPPERCASE,
    MISSING_DIGIT,
    MISSING_SYMBOL,
    CONTAINS_EMAIL_LOCAL_PART,
    CONTAINS_DISPLAY_NAME
}
