package com.hermes.infrastructure.mongo.repository.core

import com.hermes.infrastructure.mongo.MongoCollectionNames
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.regex
import org.bson.Document
import java.util.regex.Pattern

class UserRepository(database: MongoDatabase) : DocumentMongoRepository(
    database = database,
    collectionName = MongoCollectionNames.USERS,
) {
    fun findByEmail(email: String): Document? = findOne(
        regex("email", "^${Pattern.quote(email.trim())}$", "i"),
    )

    fun findByPhone(phone: String): Document? = findOne(eq("phone", phone.trim()))
}
