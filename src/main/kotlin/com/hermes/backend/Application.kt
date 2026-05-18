package com.hermes.backend

import com.hermes.backend.config.AppConfig
import com.hermes.backend.health.HealthService
import com.hermes.backend.plugins.configureCallLogging
import com.hermes.backend.plugins.configureCors
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.backend.routes.*
import com.hermes.backend.shared.AppResources
import com.hermes.backend.shared.DefaultAppResources
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    val config = AppConfig.loadFromEnvironment()

    embeddedServer(
        factory = Netty,
        host = config.server.host,
        port = config.server.port,
    ) {
        val resources = DefaultAppResources.start(config)
        configureHermesApplication(config = config, resources = resources)
    }.start(wait = true)
}

fun Application.module() {
    val config = AppConfig.loadFromEnvironment()
    val resources = DefaultAppResources.start(config)
    configureHermesApplication(config = config, resources = resources)
}

private fun Application.configureHermesApplication(config: AppConfig, resources: AppResources) {
    configureSerialization()
    configureStatusPages()
    configureCallLogging()
    configureCors(config)

    val healthService = HealthService(checks = resources.healthChecks)

    configureSystemRoutes(config = config, healthService = healthService)
    configureAuthRoutes(
        registerOwnerUseCase = resources.authModule.registerOwnerUseCase,
        registerOwnerWorkspaceUseCase = resources.authModule.registerOwnerWorkspaceUseCase,
        loginUseCase = resources.authModule.loginUseCase,
        refreshSessionUseCase = resources.authModule.refreshSessionUseCase,
        revokeSessionUseCase = resources.authModule.revokeSessionUseCase,
        authenticateRequestUseCase = resources.authModule.authenticateRequestUseCase,
        activeOrganizationResolverUseCase = resources.authModule.activeOrganizationResolverUseCase,
        effectivePermissionResolverUseCase = resources.authModule.effectivePermissionResolverUseCase,
    )
    configureCredentialAdminRoutes(authModule = resources.authModule)
    configureOrganizationRoutes(
        createOrganizationUseCase = resources.authModule.createOrganizationUseCase,
        createOwnerMembershipUseCase = resources.authModule.createOwnerMembershipUseCase,
    )
    configureMeRoutes(meUseCase = resources.authModule.meUseCase)
    configureTaxRoutes(authModule = resources.authModule, taxModule = resources.taxModule)
    configureTaxAdminRoutes(authModule = resources.authModule, taxModule = resources.taxModule)
    configureCatalogRoutes(authModule = resources.authModule, catalogModule = resources.catalogModule)
    configureCatalogRequestAdvancedRoutes(authModule = resources.authModule, catalogModule = resources.catalogModule)
    configureCatalogObservabilityRoutes(authModule = resources.authModule, catalogModule = resources.catalogModule)
    configureCatalogSeedRoutes(authModule = resources.authModule, catalogModule = resources.catalogModule)
    configureSalesRoutes(authModule = resources.authModule, salesModule = resources.salesModule)
    configureReservationSchedulingRoutes(
        authModule = resources.authModule,
        reservationSchedulingModule = resources.reservationSchedulingModule,
    )

    monitor.subscribe(ApplicationStopping) { resources.close() }
}
