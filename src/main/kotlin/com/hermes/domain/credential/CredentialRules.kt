package com.hermes.domain.credential

import com.hermes.domain.shared.DomainRuleViolation

object CredentialRules {

    /**
     * Normal application access.
     *
     * Only ACTIVE credentials can access protected business routes normally.
     * TEMPORARY and FORCE_CHANGE_REQUIRED credentials must be limited to
     * password-change flows.
     */
    fun assertCanAuthenticate(status: CredentialStatus) {
        if (status != CredentialStatus.ACTIVE) {
            throw DomainRuleViolation("Credential cannot authenticate from status $status.")
        }
    }

    /**
     * Login is allowed for ACTIVE, TEMPORARY and FORCE_CHANGE_REQUIRED credentials.
     *
     * TEMPORARY and FORCE_CHANGE_REQUIRED can log in only so the backend/client
     * can force the password-change flow.
     */
    fun assertCanStartLogin(status: CredentialStatus) {
        if (status !in setOf(
                CredentialStatus.ACTIVE,
                CredentialStatus.TEMPORARY,
                CredentialStatus.FORCE_CHANGE_REQUIRED,
            )
        ) {
            throw DomainRuleViolation("Credential cannot start login from status $status.")
        }
    }

    fun assertCanStartPasswordChange(status: CredentialStatus) {
        if (status !in setOf(
                CredentialStatus.ACTIVE,
                CredentialStatus.TEMPORARY,
                CredentialStatus.FORCE_CHANGE_REQUIRED,
                CredentialStatus.EXPIRED,
            )
        ) {
            throw DomainRuleViolation("Credential cannot start password change from status $status.")
        }
    }

    fun assertCanForcePasswordChange(status: CredentialStatus) {
        if (status !in setOf(CredentialStatus.ACTIVE, CredentialStatus.TEMPORARY)) {
            throw DomainRuleViolation("Only active or temporary credentials can be forced to change password.")
        }
    }

    fun assertCanRevoke(status: CredentialStatus) {
        if (status == CredentialStatus.REVOKED) {
            throw DomainRuleViolation("Credential is already revoked.")
        }
    }

    fun assertCanLock(status: CredentialStatus) {
        if (status in setOf(CredentialStatus.REVOKED, CredentialStatus.DISABLED)) {
            throw DomainRuleViolation("Revoked or disabled credentials cannot be locked.")
        }

        if (status == CredentialStatus.LOCKED) {
            throw DomainRuleViolation("Credential is already locked.")
        }
    }

    fun assertCanUnlock(status: CredentialStatus) {
        if (status != CredentialStatus.LOCKED) {
            throw DomainRuleViolation("Only locked credentials can be unlocked.")
        }
    }

    fun assertCanDisable(status: CredentialStatus) {
        if (status in setOf(CredentialStatus.REVOKED, CredentialStatus.DISABLED)) {
            throw DomainRuleViolation("Credential cannot be disabled from status $status.")
        }
    }

    fun assertCanRotate(status: CredentialStatus) {
        if (status in setOf(CredentialStatus.REVOKED, CredentialStatus.DISABLED)) {
            throw DomainRuleViolation("Revoked or disabled credentials cannot be rotated.")
        }
    }
}