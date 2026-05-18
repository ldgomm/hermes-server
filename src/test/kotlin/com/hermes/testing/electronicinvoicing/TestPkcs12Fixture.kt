package com.hermes.testing.electronicinvoicing

object TestPkcs12Fixture {
    private const val resourcePath = "/signatures/hermes-test.p12"
    private const val testPassword = "changeit"

    fun password(): CharArray = testPassword.toCharArray()

    fun content(): ByteArray {
        val stream = TestPkcs12Fixture::class.java.getResourceAsStream(resourcePath) ?: throw IllegalStateException(
            "Missing test PKCS12 resource at $resourcePath. " + "Create it with keytool under src/test/resources/signatures/hermes-test.p12."
        )

        return stream.use { it.readBytes() }
    }
}