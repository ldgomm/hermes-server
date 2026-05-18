package com.hermes.domain.electronicinvoicing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SriErrorClassifierTest {
    @Test
    fun `classifies processing as authorization retry only`() {
        val result = SriErrorClassifier.classify(
            SriErrorClassificationInput(
                authorizationStatus = SriAuthorizationStatus.PROCESSING,
            )
        )

        assertEquals(SriErrorCategory.SRI_PROCESSING, result.category)
        assertEquals(SriErrorRecoverability.RETRY_AUTHORIZATION_QUERY_ONLY, result.recoverability)
        assertTrue(result.shouldKeepSameAccessKey)
        assertFalse(result.userActionRequired)
    }

    @Test
    fun `classifies reception returned as data correction with same access key`() {
        val result = SriErrorClassifier.classify(
            SriErrorClassificationInput(
                receptionStatus = SriReceptionStatus.RETURNED,
                messages = listOf(SriMessage.error(identifier = "35", message = "DOCUMENTO INVALIDO")),
            )
        )

        assertEquals(SriErrorCategory.SRI_RECEPTION_RETURNED, result.category)
        assertEquals(SriErrorRecoverability.CORRECT_DATA_AND_RETRY_SAME_ACCESS_KEY, result.recoverability)
        assertTrue(result.shouldKeepSameAccessKey)
        assertTrue(result.userActionRequired)
    }

    @Test
    fun `classifies technical timeout as retryable`() {
        val result = SriErrorClassifier.classify(
            SriErrorClassificationInput(
                technicalCause = "Read timed out",
            )
        )

        assertEquals(SriErrorCategory.SRI_TIMEOUT, result.category)
        assertEquals(SriErrorRecoverability.RETRY_TECHNICAL, result.recoverability)
        assertTrue(result.shouldKeepSameAccessKey)
        assertFalse(result.userActionRequired)
    }

    @Test
    fun `classifies signature failure as configuration required`() {
        val result = SriErrorClassifier.classify(
            SriErrorClassificationInput(
                technicalCause = "PKCS12 certificate password is invalid",
            )
        )

        assertEquals(SriErrorCategory.SIGNATURE_ERROR, result.category)
        assertEquals(SriErrorRecoverability.CONFIGURATION_REQUIRED, result.recoverability)
        assertTrue(result.shouldKeepSameAccessKey)
        assertTrue(result.userActionRequired)
    }
}
