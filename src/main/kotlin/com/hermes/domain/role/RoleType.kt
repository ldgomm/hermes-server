package com.hermes.domain.role

/**
 * SYSTEM: platform-owned role, not editable by organizations.
 * ORGANIZATION: platform-owned organization role template.
 * CUSTOM: organization-owned role created later by a business admin.
 */
enum class RoleType {
    SYSTEM,
    ORGANIZATION,
    CUSTOM
}
