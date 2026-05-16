package com.hermes.infrastructure.mongo.auth

import com.hermes.domain.organization.OrganizationMembership
import com.hermes.domain.role.RoleSeed
import com.hermes.domain.role.SystemRoleCode
import com.hermes.domain.session.RefreshToken
import com.hermes.domain.user.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Instant

class MongoAuthMappersTest {
    @Test
    fun `maps user document roundtrip`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val user = User.createOwner(
            id = "usr_1",
            email = "owner@hermes.local",
            displayName = "Owner",
            now = now,
        )

        val mapped = MongoAuthMappers.userFromDocument(MongoAuthMappers.userToDocument(user))

        assertEquals(user.id, mapped.id)
        assertEquals(user.email, mapped.email)
        assertEquals(user.status, mapped.status)
    }

    @Test
    fun `maps membership with roleIds and legacy roleId`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val membership = OrganizationMembership.owner(
            id = "mem_1",
            organizationId = "org_1",
            userId = "usr_1",
            ownerRoleId = "role_organization_owner",
            now = now,
        )

        val document = MongoAuthMappers.membershipToDocument(membership)
        val mapped = MongoAuthMappers.membershipFromDocument(document)

        assertEquals(setOf("role_organization_owner"), mapped.roleIds)
        assertEquals("role_organization_owner", document.getString("roleId"))
    }

    @Test
    fun `maps role seed document`() {
        val role = RoleSeed.get(SystemRoleCode.ORGANIZATION_OWNER)
        val document = org.bson.Document("_id", role.id)
            .append("code", role.code)
            .append("scope", role.scope.name.lowercase())
            .append("type", role.type.name.lowercase())
            .append("name", role.name)
            .append("description", role.description)
            .append("permissionKeys", role.permissionKeys.toList())
            .append("systemRole", role.systemRole)
            .append("critical", role.critical)
            .append("editable", role.editable)
            .append("status", role.status.name.lowercase())
            .append("schemaVersion", role.schemaVersion)

        val mapped = MongoAuthMappers.roleFromDocument(document)

        assertEquals(role.id, mapped.id)
        assertTrue(mapped.permissionKeys.isNotEmpty())
    }

    @Test
    fun `maps refresh token roundtrip`() {
        val now = Instant.parse("2026-05-16T00:00:00Z")
        val token = RefreshToken(
            id = "rt_1",
            sessionId = "ses_1",
            userId = "usr_1",
            tokenHash = "hash",
            createdAt = now,
            expiresAt = now.plusSeconds(3600),
        )

        val mapped = MongoAuthMappers.refreshTokenFromDocument(MongoAuthMappers.refreshTokenToDocument(token))

        assertEquals(token.id, mapped.id)
        assertEquals(token.tokenHash, mapped.tokenHash)
    }
}
