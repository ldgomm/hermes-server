package com.hermes.backend.plugins

import com.hermes.backend.shared.ErrorEnvelope
import com.hermes.backend.shared.ErrorResponse
import com.hermes.domain.shared.DomainRuleViolation
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.respond

private val logger = KotlinLogging.logger {}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<DomainRuleViolation> { call, cause ->
            call.respond(
                status = HttpStatusCode.UnprocessableEntity,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "domain_rule_violation",
                        message = cause.message ?: "Domain rule violation",
                        requestId = call.request.header("X-Request-Id"),
                        details = null,
                    ),
                ),
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "bad_request",
                        message = cause.message ?: "Bad request",
                        requestId = call.request.header("X-Request-Id"),
                        details = null,
                    ),
                ),
            )
        }

        exception<IllegalStateException> { call, cause ->
            call.respond(
                status = HttpStatusCode.Conflict,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "conflict",
                        message = cause.message ?: "Conflict",
                        requestId = call.request.header("X-Request-Id"),
                        details = null,
                    ),
                ),
            )
        }

        exception<Throwable> { call, cause ->
            logger.error(cause) { "Unhandled server error" }
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorEnvelope(
                    error = ErrorResponse(
                        code = "internal_error",
                        message = "Unexpected server error",
                        requestId = call.request.header("X-Request-Id"),
                        details = null,
                    ),
                ),
            )
        }
    }
}
