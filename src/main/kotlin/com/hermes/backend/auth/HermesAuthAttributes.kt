package com.hermes.backend.auth

import com.hermes.application.auth.AuthenticatedRequestContext
import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey

object HermesAuthAttributes {
    val Context: AttributeKey<AuthenticatedRequestContext> = AttributeKey("HermesAuthenticatedContext")
}

fun ApplicationCall.hermesAuthContextOrNull(): AuthenticatedRequestContext? =
    attributes.getOrNull(HermesAuthAttributes.Context)

fun ApplicationCall.hermesAuthContext(): AuthenticatedRequestContext =
    hermesAuthContextOrNull()
        ?: throw IllegalStateException("Hermes authentication context is not available in this call.")
