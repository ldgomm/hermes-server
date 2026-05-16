package com.hermes.infrastructure.mongo.transaction

import com.mongodb.*
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoClient

class MongoTransactionRunner(
    private val client: MongoClient,
) {
    private val transactionOptions: TransactionOptions = TransactionOptions.builder()
        .readConcern(ReadConcern.SNAPSHOT)
        .writeConcern(WriteConcern.MAJORITY)
        .readPreference(ReadPreference.primary())
        .build()

    fun <T> runInTransaction(block: (ClientSession) -> T): T {
        client.startSession(
            ClientSessionOptions.builder()
                .causallyConsistent(true)
                .build(),
        ).use { session ->
            session.startTransaction(transactionOptions)
            return try {
                val result = block(session)
                session.commitTransaction()
                result
            } catch (error: Throwable) {
                runCatching { session.abortTransaction() }
                throw error
            }
        }
    }
}
