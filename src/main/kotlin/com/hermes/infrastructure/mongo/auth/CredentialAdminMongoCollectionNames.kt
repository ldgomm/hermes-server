package com.hermes.infrastructure.mongo.auth

object CredentialAdminMongoCollectionNames {
    const val USERS = "users"
    const val CREDENTIALS = "credentials"
    const val ORGANIZATIONS = "organizations"
    const val MEMBERSHIPS = "memberships"
    const val ROLES = "roles"
    const val USER_SESSIONS = "user_sessions"
    const val REFRESH_TOKENS = "refresh_tokens"
    const val INVITATIONS = "invitations"
    const val PASSWORD_RESET_TOKENS = "password_reset_tokens"
    const val CREDENTIAL_EVENTS = "credential_events"
}
