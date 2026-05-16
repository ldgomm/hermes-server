package com.hermes.infrastructure.mongo.health

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.hermes.infrastructure.mongo.MongoDocumentFields
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.Document
import java.time.Instant
import java.util.Date
import kotlin.system.measureTimeMillis

class DatabaseHealthVerifier private constructor(
    private val client: MongoClient?,
    private val database: MongoDatabase,
    private val probeTransactions: Boolean,
) {
    constructor(database: MongoDatabase) : this(
        client = null,
        database = database,
        probeTransactions = false,
    )

    constructor(client: MongoClient, database: MongoDatabase) : this(
        client = client,
        database = database,
        probeTransactions = true,
    )

    fun verify(): DatabaseHealthVerification {
        var buildInfo: Document? = null
        var hello: Document? = null
        var transactionProbe = TransactionProbeResult.NOT_REQUESTED
        var message = "MongoDB ping OK"

        val latency = measureTimeMillis {
            database.runCommand(Document("ping", 1))
            buildInfo = runCatching { database.runCommand(Document("buildInfo", 1)) }.getOrNull()
            hello = runCatching { database.runCommand(Document("hello", 1)) }
                .recoverCatching { database.runCommand(Document("isMaster", 1)) }
                .getOrNull()

            transactionProbe = runTransactionProbe(hello)
        }

        val supportsSessions = hello?.containsKey("logicalSessionTimeoutMinutes") == true
        val replicaSetName = hello?.getString("setName")
        val isWritablePrimary = hello?.getBoolean("isWritablePrimary") ?: hello?.getBoolean("ismaster")
        val transactionSupported = transactionProbe == TransactionProbeResult.SUPPORTED ||
            (!probeTransactions && supportsSessions && replicaSetName != null)

        val ok = supportsSessions && transactionSupported
        if (!ok) {
            message = "MongoDB is reachable, but replica set/session/transaction readiness is incomplete."
        }

        return DatabaseHealthVerification(
            ok = ok,
            databaseName = database.name,
            latencyMs = latency,
            mongoVersion = buildInfo?.getString("version"),
            replicaSetName = replicaSetName,
            isWritablePrimary = isWritablePrimary,
            supportsSessions = supportsSessions,
            transactionProbe = transactionProbe,
            message = message,
        )
    }

    fun requireHealthy(): DatabaseHealthVerification {
        val result = verify()
        if (!result.ok) {
            throw DatabaseHealthVerificationException(result.message)
        }
        return result
    }

    private fun runTransactionProbe(hello: Document?): TransactionProbeResult {
        if (!probeTransactions || client == null) return TransactionProbeResult.NOT_REQUESTED
        val supportsSessions = hello?.containsKey("logicalSessionTimeoutMinutes") == true
        val replicaSetName = hello?.getString("setName")
        if (!supportsSessions || replicaSetName == null) return TransactionProbeResult.UNSUPPORTED

        return try {
            client.startSession().use { session ->
                session.startTransaction()
                database.getCollection(MongoCollectionNames.DATABASE_HEALTH_CHECKS).insertOne(
                    session,
                    Document(MongoDocumentFields.ID, "health_${Instant.now().toEpochMilli()}")
                        .append("checkedAt", Date.from(Instant.now()))
                        .append(MongoDocumentFields.SCHEMA_VERSION, 1),
                )
                session.abortTransaction()
            }
            TransactionProbeResult.SUPPORTED
        } catch (_: Throwable) {
            TransactionProbeResult.FAILED
        }
    }
}

class DatabaseHealthVerificationException(message: String) : RuntimeException(message)
