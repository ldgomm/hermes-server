package com.hermes.backend.routes

import com.hermes.backend.config.AppConfig
import com.hermes.backend.health.HealthService
import com.hermes.backend.version.VersionResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureSystemRoutes(
    config: AppConfig,
    healthService: HealthService,
) {
    routing {
        get("/") {
            call.respond(
                mapOf(
                    "name" to config.app.name,
                    "version" to config.app.version,
                    "environment" to config.app.environment,
                ),
            )
        }

        get("/health") {
            val response = healthService.check()
            val statusCode = if (response.status == "DOWN") {
                HttpStatusCode.ServiceUnavailable
            } else {
                HttpStatusCode.OK
            }
            call.respond(status = statusCode, message = response)
        }

        get("/version") {
            call.respond(
                VersionResponse(
                    appName = config.app.name,
                    version = config.app.version,
                    environment = config.app.environment,
                    buildTime = config.app.buildTime,
                    commitSha = config.app.commitSha,
                ),
            )
        }
    }
}
