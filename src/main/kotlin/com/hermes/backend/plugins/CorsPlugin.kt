package com.hermes.backend.plugins

import com.hermes.backend.auth.HermesHeaders
import com.hermes.backend.config.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors(config: AppConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HermesHeaders.ORGANIZATION_ID)
        allowHeader("X-App-Version")
        allowHeader("X-Platform")
        allowHeader("X-Request-Id")

        if (config.cors.anyHost) {
            anyHost()
        } else {
            config.cors.allowedHosts.forEach { host ->
                allowHost(host, schemes = listOf("http", "https"))
            }
        }
    }
}