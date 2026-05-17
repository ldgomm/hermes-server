package com.hermes.backend.catalog

import kotlinx.serialization.Serializable

@Serializable
data class CatalogRemoveLocalItemRequest(
    val reason: String,
)
