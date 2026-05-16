package com.hermes.backend.version

import kotlinx.serialization.Serializable

@Serializable
data class VersionResponse(
    val appName: String,
    val version: String,
    val environment: String,
    val buildTime: String,
    val commitSha: String,
)
