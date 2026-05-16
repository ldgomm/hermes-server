package com.hermes.application.auth

import com.hermes.domain.credential.UserCredential
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import java.time.Clock
import java.time.Instant

class RegisterOwnerUseCase(
    private val userRepository: UserRepository,
    private val credentialRepository: UserCredentialRepository,
    private val passwordPolicy: PasswordPolicy,
    private val passwordHasher: PasswordHasher,
    private val idGenerator: AuthIdGenerator,
    private val auditLogger: CredentialAuditLogger = NoopCredentialAuditLogger,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun execute(command: RegisterOwnerCommand): RegisterOwnerResult {
        val now = Instant.now(clock)
        val email = EmailNormalizer.normalize(command.email)

        if (userRepository.existsUserByEmail(email)) {
            throw DomainRuleViolation("User email already exists.")
        }

        passwordPolicy.assertValid(
            password = command.password,
            email = email,
            displayName = command.displayName,
        )

        val user = User.createOwner(
            id = idGenerator.newId("usr"),
            email = email,
            displayName = command.displayName,
            phone = command.phone,
            now = now,
        )

        val credential = UserCredential.createPasswordCredential(
            id = idGenerator.newId("cred"),
            userId = user.id,
            passwordHash = passwordHasher.hash(command.password.toCharArray()),
            now = now,
        )

        userRepository.create(user)
        credentialRepository.create(credential)

        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.USER_CREATED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = null,
                ipAddress = null,
                userAgent = null,
                createdAt = now,
            ),
        )
        auditLogger.log(
            CredentialAuditEvent(
                action = CredentialAuditAction.CREDENTIAL_CREATED,
                actorUserId = user.id,
                targetUserId = user.id,
                organizationId = null,
                sessionId = null,
                ipAddress = null,
                userAgent = null,
                createdAt = now,
            ),
        )

        return RegisterOwnerResult(user = user, credential = credential)
    }
}
