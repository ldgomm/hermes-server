package com.hermes.domain.credential

enum class CredentialStatus {
    ACTIVE,
    TEMPORARY,
    FORCE_CHANGE_REQUIRED,
    EXPIRED,
    REVOKED,
    LOCKED,
    DISABLED
}
