package com.hermes.application.admin.access

import com.hermes.application.auth.*
import com.hermes.domain.invitation.Invitation
import com.hermes.domain.invitation.InvitationStatus
import com.hermes.domain.organization.MembershipStatus
import com.hermes.domain.organization.Organization
import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.permission.PermissionCatalog
import com.hermes.domain.permission.PermissionDefinition
import com.hermes.domain.role.RoleDefinition
import com.hermes.domain.role.RoleScope
import com.hermes.domain.role.RoleStatus
import com.hermes.domain.role.RoleType
import com.hermes.domain.session.UserSession
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.domain.user.User
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.*

class AdminInvitationsUseCasesTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `creates invitation through existing auth invitation flow and returns admin response`() {
        val repository = InMemoryAdminInvitationsRepository13B5()
        val delivery = RecordingInvitationDelivery13B5()
        val credentialAudit = RecordingCredentialAuditLogger13B5()
        val adminAudit = RecordingAdminAccessAuditLogger13B5()
        val delegate = inviteUserUseCase(repository, delivery, credentialAudit)
        val useCase = CreateAdminInvitationUseCase(
            delegate = delegate,
            accessRepository = repository,
            auditLogger = adminAudit,
            clock = fixedClock,
        )

        val result = useCase.execute(
            CreateAdminInvitationCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                email = "NEW.CASHIER@HERMES.LOCAL",
                displayName = "Nuevo Cajero",
                roleIds = setOf("role_cashier"),
                reason = "Invitar cajero para operación de caja",
                ipAddress = "127.0.0.1",
                userAgent = "Admin iOS test",
            )
        )

        assertEquals("inv_001", result.invitation.id)
        assertEquals("new.cashier@hermes.local", result.invitation.email)
        assertEquals(InvitationStatus.PENDING.name, result.invitation.status)
        assertEquals("usr_001", result.user.id)
        assertEquals("Nuevo Cajero", result.user.displayName)
        assertEquals("mem_001", result.membershipId)
        assertEquals(MembershipStatus.PENDING_INVITATION.name, result.user.membershipStatus)
        assertTrue(result.rawInvitationToken.isNotBlank())
        assertEquals(result.rawInvitationToken, delivery.deliveredToken)
        assertEquals("new.cashier@hermes.local", delivery.deliveredEmail)
        assertEquals(result.invitationUrl, delivery.deliveredUrl)
        assertTrue(repository.invitations.any { it.id == "inv_001" })
        assertEquals(AdminAccessAuditAction.INVITATION_CREATED, adminAudit.events.single().action)
        assertEquals(
            listOf(CredentialAuditAction.USER_INVITED, CredentialAuditAction.INVITATION_CREATED),
            credentialAudit.events.map { it.action },
        )
    }

    @Test
    fun `lists and gets invitations with role names`() {
        val repository = InMemoryAdminInvitationsRepository13B5()
        repository.seedPendingInvitation(id = "inv_existing", email = "cashier@hermes.local")

        val list = ListAdminInvitationsUseCase(repository).execute(
            ListAdminInvitationsCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
            )
        )
        val detail = GetAdminInvitationUseCase(repository).execute(
            GetAdminInvitationCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                invitationId = "inv_existing",
            )
        )

        assertEquals(1, list.invitations.size)
        assertEquals("inv_existing", detail.invitation.id)
        assertEquals(listOf("Cashier"), detail.invitation.roleNames)
    }

    @Test
    fun `resends pending invitation with new token and delivery`() {
        val repository = InMemoryAdminInvitationsRepository13B5()
        val invitation = repository.seedPendingInvitation(id = "inv_resend", email = "cashier@hermes.local")
        val delivery = RecordingInvitationDelivery13B5()
        val auditLogger = RecordingAdminAccessAuditLogger13B5()

        val result = ResendAdminInvitationUseCase(
            repository = repository,
            delivery = delivery,
            clock = fixedClock,
            auditLogger = auditLogger,
        ).execute(
            ResendAdminInvitationCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                invitationId = "inv_resend",
                reason = "El usuario pidió reenvío del enlace",
            )
        )

        val updated = repository.invitations.single { it.id == "inv_resend" }
        assertNotEquals(invitation.tokenHash, updated.tokenHash)
        assertEquals(Instant.parse("2026-05-26T12:00:00Z"), updated.expiresAt)
        assertEquals(InvitationStatus.PENDING.name, result.invitation.status)
        assertEquals(result.rawInvitationToken, delivery.deliveredToken)
        assertEquals("cashier@hermes.local", delivery.deliveredEmail)
        assertEquals(AdminAccessAuditAction.INVITATION_RESENT, auditLogger.events.single().action)
    }

    @Test
    fun `rejects resend when invitation is not pending`() {
        val repository = InMemoryAdminInvitationsRepository13B5()
        repository.seedRevokedInvitation(id = "inv_revoked")

        assertFailsWith<DomainRuleViolation> {
            ResendAdminInvitationUseCase(repository, clock = fixedClock).execute(
                ResendAdminInvitationCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                    invitationId = "inv_revoked",
                    reason = "No debe reenviar revocada",
                )
            )
        }
    }

    @Test
    fun `revokes pending invitation`() {
        val repository = InMemoryAdminInvitationsRepository13B5()
        repository.seedPendingInvitation(id = "inv_revoke", email = "cashier@hermes.local")
        val auditLogger = RecordingAdminAccessAuditLogger13B5()

        val result = RevokeAdminInvitationUseCase(repository, fixedClock, auditLogger).execute(
            RevokeAdminInvitationCommand(
                organizationId = "org_1",
                actorUserId = "usr_owner",
                actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                invitationId = "inv_revoke",
                reason = "Invitación emitida por error",
            )
        )

        assertEquals(InvitationStatus.REVOKED.name, result.invitation.status)
        assertEquals(Instant.parse("2026-05-19T12:00:00Z"), result.invitation.revokedAt)
        assertEquals(AdminAccessAuditAction.INVITATION_REVOKED, auditLogger.events.single().action)
    }

    @Test
    fun `rejects duplicate pending invitation through existing auth flow`() {
        val repository = InMemoryAdminInvitationsRepository13B5()
        repository.seedPendingInvitation(id = "inv_duplicate", email = "cashier@hermes.local")
        val useCase = CreateAdminInvitationUseCase(
            delegate = inviteUserUseCase(
                repository, RecordingInvitationDelivery13B5(), RecordingCredentialAuditLogger13B5()
            ),
            accessRepository = repository,
            clock = fixedClock,
        )

        assertFailsWith<DomainRuleViolation> {
            useCase.execute(
                CreateAdminInvitationCommand(
                    organizationId = "org_1",
                    actorUserId = "usr_owner",
                    actorEffectivePermissions = setOf(PermissionCatalog.CREDENTIALS_USERS_INVITE),
                    email = "cashier@hermes.local",
                    displayName = "Cashier",
                    roleIds = setOf("role_cashier"),
                    reason = "Duplicado",
                )
            )
        }
    }

    private fun inviteUserUseCase(
        repository: InMemoryAdminInvitationsRepository13B5,
        delivery: InvitationDelivery,
        auditLogger: CredentialAuditLogger,
    ): InviteUserUseCase = InviteUserUseCase(
        userRepository = repository,
        organizationRepository = repository,
        membershipRepository = repository,
        roleRepository = repository,
        invitationRepository = repository,
        idGenerator = SequentialAuthIdGenerator13B5(),
        tokenGenerator = SecureTokenGenerator(bytes = 32),
        delivery = delivery,
        auditLogger = auditLogger,
        clock = fixedClock,
    )
}

private class RecordingInvitationDelivery13B5 : InvitationDelivery {
    var deliveredEmail: String? = null
    var deliveredToken: String? = null
    var deliveredUrl: String? = null

    override fun buildInvitationUrl(rawToken: String): String = "https://admin.hermes.local/invitations/$rawToken"

    override fun deliverInvitation(email: String, rawToken: String, invitationUrl: String?) {
        deliveredEmail = email
        deliveredToken = rawToken
        deliveredUrl = invitationUrl
    }
}

private class RecordingAdminAccessAuditLogger13B5 : AdminAccessAuditLogger {
    val events = mutableListOf<AdminAccessAuditEvent>()
    override fun log(event: AdminAccessAuditEvent) {
        events += event
    }
}

private class RecordingCredentialAuditLogger13B5 : CredentialAuditLogger {
    val events = mutableListOf<CredentialAuditEvent>()
    override fun log(event: CredentialAuditEvent) {
        events += event
    }
}

private class SequentialAuthIdGenerator13B5 : AuthIdGenerator {
    private val counters = mutableMapOf<String, Int>()
    override fun newId(prefix: String): String {
        val next = (counters[prefix] ?: 0) + 1
        counters[prefix] = next
        return "${prefix}_${next.toString().padStart(3, '0')}"
    }
}

private class InMemoryAdminInvitationsRepository13B5 : AdminAccessRepository, UserRepository, OrganizationRepository,
    MembershipMutationRepository, RoleQueryRepository, InvitationRepository {

    private val now: Instant = Instant.parse("2026-05-19T00:00:00Z")

    val users = mutableListOf(
        User.createOwner(
            id = "usr_owner",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )
    )

    val organizations = mutableListOf(
        Organization.create(
            id = "org_1",
            countryCode = "EC",
            taxId = "1790012345001",
            legalName = "Hermes Demo S.A.",
            commercialName = "Hermes Demo",
            ownerUserId = "usr_owner",
            now = now,
        )
    )

    val memberships = mutableListOf(
        OrganizationMembership.owner(
            id = "mem_owner",
            organizationId = "org_1",
            userId = "usr_owner",
            ownerRoleId = "role_owner",
            now = now,
        )
    )

    val roles = mutableListOf(
        RoleDefinition(
            id = "role_owner",
            code = "owner",
            organizationId = "org_1",
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Owner",
            description = "Owner role",
            permissionKeys = setOf(
                PermissionCatalog.CREDENTIALS_USERS_INVITE,
                PermissionCatalog.CREDENTIALS_USERS_VIEW,
                PermissionCatalog.CREDENTIALS_ROLES_MANAGE,
            ),
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        ),
        RoleDefinition(
            id = "role_cashier",
            code = "cashier",
            organizationId = "org_1",
            scope = RoleScope.ORGANIZATION,
            type = RoleType.CUSTOM,
            name = "Cashier",
            description = "Cashier role",
            permissionKeys = setOf(PermissionCatalog.SALES_VIEW),
            systemRole = false,
            critical = false,
            editable = true,
            status = RoleStatus.ACTIVE,
        ),
    )

    val invitations = mutableListOf<Invitation>()

    fun seedPendingInvitation(id: String, email: String): Invitation = Invitation(
        id = id,
        organizationId = "org_1",
        email = email.lowercase(),
        invitedByUserId = "usr_owner",
        roleIds = setOf("role_cashier"),
        tokenHash = "old_hash_$id",
        status = InvitationStatus.PENDING,
        createdAt = now,
        expiresAt = now.plusSeconds(604_800L),
    ).also { invitations += it }

    fun seedRevokedInvitation(id: String): Invitation = Invitation(
        id = id,
        organizationId = "org_1",
        email = "revoked@hermes.local",
        invitedByUserId = "usr_owner",
        roleIds = setOf("role_cashier"),
        tokenHash = "old_hash_$id",
        status = InvitationStatus.REVOKED,
        createdAt = now,
        expiresAt = now.plusSeconds(604_800L),
        revokedAt = now.plusSeconds(60),
    ).also { invitations += it }

    override fun listUserAccess(
        organizationId: String,
        query: String?,
        status: String?,
        limit: Int,
    ): List<AdminUserAccessRecord> = memberships.filter { it.organizationId == organizationId }
        .filter { status.isNullOrBlank() || it.status.name.equals(status, ignoreCase = true) }
        .mapNotNull { membership ->
            val user = users.firstOrNull { it.id == membership.userId } ?: return@mapNotNull null
            if (!query.isNullOrBlank() && !user.email.contains(query, ignoreCase = true) && !user.displayName.contains(
                    query, ignoreCase = true
                )
            ) {
                return@mapNotNull null
            }
            AdminUserAccessRecord(
                user = user,
                membership = membership,
                roles = findRolesByIds(membership.roleIds),
                activeSessionCount = 0,
            )
        }.take(limit)

    override fun findUserAccess(organizationId: String, userId: String): AdminUserAccessRecord? =
        listUserAccess(organizationId, query = null, status = null, limit = 250).firstOrNull { it.user.id == userId }

    override fun existsUserByEmail(email: String): Boolean = users.any { it.email == email.trim().lowercase() }

    override fun findUserByEmail(email: String): User? = users.firstOrNull { it.email == email.trim().lowercase() }

    override fun findUserById(userId: String): User? = users.firstOrNull { it.id == userId }

    override fun create(user: User) {
        users += user
    }

    override fun update(user: User) = updateUser(user)

    override fun updateUser(user: User) {
        users.replaceAll { if (it.id == user.id) user else it }
    }

    override fun existsByTaxId(countryCode: String, taxId: String): Boolean =
        organizations.any { it.countryCode == countryCode.trim().uppercase() && it.taxId == taxId.trim() }

    override fun findOrganizationById(organizationId: String): Organization? =
        organizations.firstOrNull { it.id == organizationId }

    override fun create(organization: Organization) {
        organizations += organization
    }

    override fun findMembership(organizationId: String, userId: String): OrganizationMembership? =
        memberships.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun findByOrganizationIdAndUserId(organizationId: String, userId: String): OrganizationMembership? =
        findMembership(organizationId, userId)

    override fun create(membership: OrganizationMembership) {
        memberships += membership
    }

    override fun updateMembership(membership: OrganizationMembership) = update(membership)

    override fun update(membership: OrganizationMembership) {
        memberships.replaceAll { if (it.id == membership.id) membership else it }
    }

    override fun findRolesByIds(roleIds: Set<String>): List<RoleDefinition> = roles.filter { it.id in roleIds }

    override fun findRoleById(roleId: String): RoleDefinition? = roles.firstOrNull { it.id == roleId }

    override fun listRoles(organizationId: String, includeSystemTemplates: Boolean): List<RoleDefinition> =
        roles.filter { it.organizationId == organizationId }

    override fun findRole(organizationId: String, roleId: String): RoleDefinition? =
        roles.firstOrNull { it.id == roleId && it.organizationId == organizationId }

    override fun existsRoleCode(organizationId: String, code: String, excludeRoleId: String?): Boolean =
        roles.any { it.organizationId == organizationId && it.code == code && it.id != excludeRoleId }

    override fun createRole(role: RoleDefinition) {
        roles += role
    }

    override fun updateRole(role: RoleDefinition) {
        roles.replaceAll { if (it.id == role.id) role else it }
    }

    override fun create(invitation: Invitation) {
        invitations += invitation
    }

    override fun findInvitationByTokenHash(tokenHash: String): Invitation? =
        invitations.firstOrNull { it.tokenHash == tokenHash }

    override fun findPendingByOrganizationAndEmail(organizationId: String, email: String): Invitation? =
        invitations.firstOrNull {
            it.organizationId == organizationId && it.email == email.trim()
                .lowercase() && it.status == InvitationStatus.PENDING
        }

    override fun listInvitations(organizationId: String, status: String?, limit: Int): List<Invitation> =
        invitations.filter { it.organizationId == organizationId }
            .filter { status.isNullOrBlank() || it.status.name.equals(status, ignoreCase = true) }
            .sortedByDescending { it.createdAt }.take(limit)

    override fun findInvitation(organizationId: String, invitationId: String): Invitation? =
        invitations.firstOrNull { it.organizationId == organizationId && it.id == invitationId }

    override fun updateInvitation(invitation: Invitation) = update(invitation)

    override fun update(invitation: Invitation) {
        invitations.replaceAll { if (it.id == invitation.id) invitation else it }
    }

    override fun listPermissionDefinitions(includeReserved: Boolean): List<PermissionDefinition> =
        if (includeReserved) PermissionCatalog.definitions else PermissionCatalog.active

    override fun findActiveSessionsByUserId(userId: String): List<UserSession> = emptyList()

    override fun updateSession(session: UserSession) = Unit

    override fun revokeActiveRefreshTokensBySessionIds(sessionIds: Set<String>, revokedAt: Instant): Int = 0

    override fun countActiveAdminMemberships(
        organizationId: String,
        excludingUserId: String?,
        adminPermissionKeys: Set<String>,
    ): Int =
        memberships.filter { it.organizationId == organizationId && it.status == MembershipStatus.ACTIVE && it.userId != excludingUserId }
            .count { membership ->
                findRolesByIds(membership.roleIds).any { role ->
                    role.status == RoleStatus.ACTIVE && role.permissionKeys.any { it in adminPermissionKeys }
                }
            }
}
