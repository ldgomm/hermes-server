package com.hermes.backend.shared

import kotlinx.serialization.Serializable

@Serializable
data class ErrorEnvelope(
    val error: ErrorResponse,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val requestId: String? = null,
    val details: String? = null,
)
