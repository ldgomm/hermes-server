package com.hermes.infrastructure.mongo.migration.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.migration.MongoMigration
import com.hermes.infrastructure.mongo.migration.MongoMigrationSupport
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Indexes

object M004CreateUsersRolesPermissionsCredentialsMigration : MongoMigration {
    override val id: String = "M004_create_users_roles_permissions_credentials"
    override val description: String = "Create users, memberships, roles, permissions, credential events and sessions."

    override fun up(database: MongoDatabase) {
        createUsers(database)
        createMemberships(database)
        createRoles(database)
        createPermissions(database)
        createCredentialEvents(database)
        createUserSessions(database)
    }

    private fun createUsers(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("email", MongoMigrationSupport.string(maxLength = 320))
            .append("phone", MongoMigrationSupport.nullableString(maxLength = 32))
            .append("displayName", MongoMigrationSupport.string(maxLength = 128))
            .append(
                "status",
                MongoMigrationSupport.enum(listOf("invited", "active", "blocked", "disabled", "archived"))
            )
            .append("auth", MongoMigrationSupport.obj())
            .append("profile", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.USERS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "email",
                    "displayName",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("email"),
            name = "users_email_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("status", "createdAt"),
            name = "users_status_created_at_idx",
        )
    }

    private fun createMemberships(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = true)
            .append("userId", MongoMigrationSupport.id(prefix = "usr_"))
            .append("roleId", MongoMigrationSupport.id(prefix = "role_"))
            .append("status", MongoMigrationSupport.enum(listOf("invited", "active", "suspended", "removed")))
            .append("effectivePermissions", MongoMigrationSupport.array())
            .append("invitedBy", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("joinedAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.MEMBERSHIPS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = true) + listOf(
                    "userId",
                    "roleId",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "userId"),
            name = "memberships_org_user_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "roleId", "status"),
            name = "memberships_org_role_status_idx",
        )
    }

    private fun createRoles(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("code", MongoMigrationSupport.string(maxLength = 64))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("scope", MongoMigrationSupport.enum(listOf("system", "organization")))
            .append("status", MongoMigrationSupport.enum(listOf("active", "disabled", "archived")))
            .append("permissions", MongoMigrationSupport.array(MongoMigrationSupport.string(maxLength = 128)))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.ROLES,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "code",
                    "name",
                    "scope",
                    "status",
                    "permissions",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("code"),
            name = "roles_system_code_unique_idx",
            unique = true,
            partialFilterExpression = Filters.eq("scope", "system"),
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "code"),
            name = "roles_org_code_unique_idx",
            unique = true,
            partialFilterExpression = Filters.eq("scope", "organization"),
        )
    }

    private fun createPermissions(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("code", MongoMigrationSupport.string(maxLength = 128))
            .append("name", MongoMigrationSupport.string(maxLength = 128))
            .append("module", MongoMigrationSupport.string(maxLength = 64))
            .append("status", MongoMigrationSupport.enum(listOf("active", "deprecated", "disabled")))
            .append("description", MongoMigrationSupport.nullableString(maxLength = 1024))

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.PERMISSIONS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "code",
                    "name",
                    "module",
                    "status",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("code"),
            name = "permissions_code_unique_idx",
            unique = true,
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("module", "status"),
            name = "permissions_module_status_idx",
        )
    }

    private fun createCredentialEvents(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("userId", MongoMigrationSupport.id(prefix = "usr_"))
            .append("organizationId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append(
                "eventType",
                MongoMigrationSupport.enum(
                    listOf(
                        "password_created",
                        "password_changed",
                        "temporary_password_created",
                        "login_success",
                        "login_failed",
                        "user_blocked",
                        "user_unblocked",
                        "session_revoked"
                    )
                )
            )
            .append("occurredAt", MongoMigrationSupport.date())
            .append("actorUserId", MongoMigrationSupport.nullableString(maxLength = 128))
            .append("metadata", MongoMigrationSupport.obj())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.CREDENTIAL_EVENTS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "userId",
                    "eventType",
                    "occurredAt",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("userId", "occurredAt"),
            name = "credential_events_user_occurred_at_idx",
        )
        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("organizationId", "occurredAt"),
            name = "credential_events_org_occurred_at_idx",
            sparse = true,
        )
    }

    private fun createUserSessions(database: MongoDatabase) {
        val properties = MongoMigrationSupport.commonRootProperties(requireOrganizationId = false)
            .append("userId", MongoMigrationSupport.id(prefix = "usr_"))
            .append("status", MongoMigrationSupport.enum(listOf("active", "revoked", "expired")))
            .append("createdAt", MongoMigrationSupport.date())
            .append("expiresAt", MongoMigrationSupport.date())
            .append("revokedAt", MongoMigrationSupport.nullableDate())
            .append("lastSeenAt", MongoMigrationSupport.nullableDate())

        val collection = MongoMigrationSupport.ensureCollection(
            database = database,
            name = MongoCollectionNames.USER_SESSIONS,
            validator = MongoMigrationSupport.jsonSchema(
                required = MongoMigrationSupport.commonRequired(requireOrganizationId = false) + listOf(
                    "userId",
                    "status",
                    "expiresAt",
                ),
                properties = properties,
            ),
        )

        MongoMigrationSupport.createIndex(
            collection = collection,
            keys = Indexes.ascending("userId", "status", "expiresAt"),
            name = "user_sessions_user_status_expires_at_idx",
        )
    }
}
