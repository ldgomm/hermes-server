package com.hermes.backend

import com.hermes.backend.config.AppConfig
import com.hermes.backend.health.HealthService
import com.hermes.backend.plugins.configureCallLogging
import com.hermes.backend.plugins.configureCors
import com.hermes.backend.plugins.configureSerialization
import com.hermes.backend.plugins.configureStatusPages
import com.hermes.backend.routes.configureSystemRoutes
import com.hermes.backend.shared.AppResources
import com.hermes.backend.shared.DefaultAppResources
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = AppConfig.loadFromEnvironment()

    embeddedServer(
        factory = Netty,
        host = config.server.host,
        port = config.server.port,
    ) {
        val resources = DefaultAppResources.start(config)
        configureHermesApplication(
            config = config,
            resources = resources,
        )
    }.start(wait = true)
}

fun Application.module() {
    val config = AppConfig.loadFromEnvironment()
    val resources = DefaultAppResources.start(config)

    configureHermesApplication(
        config = config,
        resources = resources,
    )
}

private fun Application.configureHermesApplication(
    config: AppConfig,
    resources: AppResources,
) {
    configureSerialization()
    configureStatusPages()
    configureCallLogging()
    configureCors(config)

    val healthService = HealthService(
        checks = resources.healthChecks,
    )

    configureSystemRoutes(
        config = config,
        healthService = healthService,
    )

    monitor.subscribe(ApplicationStopping) {
        resources.close()
    }
}
