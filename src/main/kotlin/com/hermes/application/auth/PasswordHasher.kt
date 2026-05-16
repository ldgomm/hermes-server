package com.hermes.application.auth

interface PasswordHasher {
    fun hash(password: CharArray): String
    fun verify(password: CharArray, encodedHash: String): Boolean
}
