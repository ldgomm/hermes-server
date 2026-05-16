package com.hermes.infrastructure.mongo.migration

data class MongoMigrationRunReport(
    val totalMigrations: Int,
    val alreadyApplied: Int,
    val applied: List<MongoMigrationAppliedReport>,
) {
    val appliedCount: Int get() = applied.size
    val pendingCountAfterRun: Int get() = totalMigrations - alreadyApplied - appliedCount
}

data class MongoMigrationAppliedReport(
    val id: String,
    val description: String,
    val executionMillis: Long,
)
