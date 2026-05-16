package com.hermes.backend.plugins

import com.hermes.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors(config: AppConfig) {
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
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
