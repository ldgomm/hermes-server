package com.hermes.infrastructure.mongo.testing

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

object MongoIntegrationTestSupport {
    private val imageName: DockerImageName = DockerImageName.parse("mongo:7.0.14")

    private val containerResult: Result<MongoDBContainer> by lazy {
        runCatching {
            MongoDBContainer(imageName)
                .withStartupAttempts(1)
                .also { container -> container.start() }
        }
    }

    val isAvailable: Boolean
        get() = containerResult.isSuccess

    fun assumeMongoAvailable() {
        val result = containerResult
        assumeTrue(result.isSuccess) {
            val reason = result.exceptionOrNull()?.message ?: "Docker/Testcontainers is not available."
            "Skipping MongoDB integration test because Testcontainers could not start MongoDB. Reason: $reason"
        }
    }

    fun client(): MongoClient {
        val container = containerResult.getOrElse { error ->
            throw IllegalStateException(
                "MongoDB Testcontainers is not available. Start Docker Desktop and rerun the tests, " +
                        "or keep the test skipped through assumeMongoAvailable().",
                error,
            )
        }

        return MongoClients.create(container.replicaSetUrl)
    }

    fun databaseName(prefix: String): String =
        prefix + "_" + UUID.randomUUID().toString().replace("-", "")
}
