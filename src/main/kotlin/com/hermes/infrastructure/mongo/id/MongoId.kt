package com.hermes.infrastructure.mongo.id

@JvmInline
value class MongoId(val value: String) {
    init {
        require(value.isNotBlank()) { "Mongo id cannot be blank." }
        require(value.length <= MAX_LENGTH) { "Mongo id cannot be longer than $MAX_LENGTH characters." }
        require(ID_PATTERN.matches(value)) {
            "Mongo id must use a semantic lowercase prefix followed by '_' and a safe identifier body. Current value: $value"
        }
    }

    fun requirePrefix(prefix: MongoIdPrefix): MongoId {
        require(value.startsWith(prefix.value)) {
            "Mongo id '$value' must start with prefix '${prefix.value}'."
        }
        return this
    }

    override fun toString(): String = value

    companion object {
        private const val MAX_LENGTH = 128
        private val ID_PATTERN = Regex("^[a-z][a-z0-9]*_[A-Za-z0-9][A-Za-z0-9_-]{1,120}$")

        fun of(value: String): MongoId = MongoId(value.trim())

        fun of(value: String, prefix: MongoIdPrefix): MongoId = of(value).requirePrefix(prefix)

        fun isValid(value: String): Boolean = runCatching { of(value) }.isSuccess
    }
}
