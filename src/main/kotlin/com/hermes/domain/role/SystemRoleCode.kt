package com.hermes.domain.role

enum class SystemRoleCode(val code: String) {
    PLATFORM_SUPER_ADMIN("platform_super_admin"),
    PLATFORM_SUPPORT("platform_support"),
    ORGANIZATION_OWNER("organization_owner"),
    ORGANIZATION_ADMIN("organization_admin"),
    MANAGER("manager"),
    OPERATOR("operator"),
    ACCOUNTANT("accountant"),
    READ_ONLY("read_only");

    companion object {
        val codes: Set<String> = entries.map { it.code }.toSet()
    }
}
