package com.hermes.infrastructure.mongo.migration

import com.mongodb.client.MongoClients

/**
 * CLI entrypoint for Mongo migrations.
 *
 * Usage:
 *
 * MONGODB_URI='mongodb://localhost:27017/hermes_local?replicaSet=rs0' \
 * MONGODB_DATABASE='hermes_local' \
 * MIGRATION_MODE='up' \
 * ./gradlew runMigrationCommand
 *
 * Supported modes:
 * - up: validates and applies pending migrations
 * - check: validates migration metadata only
 */
fun main() {
    val uri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017/hermes_local?replicaSet=rs0"

    val databaseName = System.getenv("MONGODB_DATABASE") ?: "hermes_local"

    val mode = System.getenv("MIGRATION_MODE") ?: "up"

    require(mode in setOf("up", "check")) {
        "MIGRATION_MODE must be 'up' or 'check'. Current value: $mode"
    }

    MongoClients.create(uri).use { client ->
        val database = client.getDatabase(databaseName)

        val runner = MongoMigrationRunner(
            database = database,
        )

        when (mode) {
            "up" -> {
                val report = runner.migrate(HermesMongoMigrations.all)

                println("Mongo migrations completed.")
                println("Total migrations: ${report.totalMigrations}")
                println("Already applied: ${report.alreadyApplied}")
                println("Applied now: ${report.applied.size}")

                report.applied.forEach { applied ->
                    println("- ${applied.id}: ${applied.description} (${applied.executionMillis}ms)")
                }
            }

            "check" -> {
                runner.validateOnly(HermesMongoMigrations.all)

                println("Mongo migration validation completed.")
                println("Total migrations: ${HermesMongoMigrations.all.size}")
                println("Status: OK")
            }
        }
    }
}