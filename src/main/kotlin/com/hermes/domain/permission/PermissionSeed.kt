package com.hermes.domain.permission

object PermissionSeed {
    const val VERSION = "2026.05.phase5.10"

    val all: List<PermissionDefinition> = PermissionCatalog.definitions

    val active: List<PermissionDefinition> = PermissionCatalog.active

    val reserved: List<PermissionDefinition> = PermissionCatalog.reserved
}
