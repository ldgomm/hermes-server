package com.hermes.infrastructure.mongo.id

import java.util.*

object MongoIdGenerator {
    fun newId(prefix: MongoIdPrefix): MongoId {
        val randomPart = UUID.randomUUID().toString().replace("-", "")
        return MongoId.of(prefix.value + randomPart, prefix)
    }

    fun newIdValue(prefix: MongoIdPrefix): String = newId(prefix).value
}
