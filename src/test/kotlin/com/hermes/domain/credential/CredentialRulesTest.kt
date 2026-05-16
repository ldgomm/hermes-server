package com.hermes.domain.credential

import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CredentialRulesTest {

    @Test
    fun `allows active credential to authenticate`() {
        CredentialRules.assertCanAuthenticate(CredentialStatus.ACTIVE)
    }

    @Test
    fun `rejects temporary credential for normal access`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanAuthenticate(CredentialStatus.TEMPORARY)
        }
    }

    @Test
    fun `rejects force change credential for normal access`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanAuthenticate(CredentialStatus.FORCE_CHANGE_REQUIRED)
        }
    }

    @Test
    fun `rejects revoked credential for normal access`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanAuthenticate(CredentialStatus.REVOKED)
        }
    }

    @Test
    fun `rejects locked credential for normal access`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanAuthenticate(CredentialStatus.LOCKED)
        }
    }

    @Test
    fun `allows temporary credential to start password change`() {
        CredentialRules.assertCanStartPasswordChange(CredentialStatus.TEMPORARY)
    }

    @Test
    fun `allows force change credential to start password change`() {
        CredentialRules.assertCanStartPasswordChange(CredentialStatus.FORCE_CHANGE_REQUIRED)
    }

    @Test
    fun `rejects revoked credential starting password change`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanStartPasswordChange(CredentialStatus.REVOKED)
        }
    }

    @Test
    fun `allows active credential to be forced to change password`() {
        CredentialRules.assertCanForcePasswordChange(CredentialStatus.ACTIVE)
    }

    @Test
    fun `rejects expired credential being forced to change password`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanForcePasswordChange(CredentialStatus.EXPIRED)
        }
    }

    @Test
    fun `allows active credential to be locked`() {
        CredentialRules.assertCanLock(CredentialStatus.ACTIVE)
    }

    @Test
    fun `rejects already locked credential being locked again`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanLock(CredentialStatus.LOCKED)
        }
    }

    @Test
    fun `allows locked credential to be unlocked`() {
        CredentialRules.assertCanUnlock(CredentialStatus.LOCKED)
    }

    @Test
    fun `rejects active credential being unlocked`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanUnlock(CredentialStatus.ACTIVE)
        }
    }

    @Test
    fun `rejects revoked credential rotation`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanRotate(CredentialStatus.REVOKED)
        }
    }

    @Test
    fun `rejects disabled credential rotation`() {
        assertFailsWith<DomainRuleViolation> {
            CredentialRules.assertCanRotate(CredentialStatus.DISABLED)
        }
    }
}
