package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.SriAccessKey
import com.hermes.domain.electronicinvoicing.SriSequential

data class ElectronicSequenceResult(
    val sequence: ElectronicSequence,
)

data class ReserveSriAccessKeyResult(
    val sequence: ElectronicSequence,
    val sequential: SriSequential,
    val documentNumber: String,
    val accessKey: SriAccessKey,
    val authorizationNumber: String,
) {
    init {
        require(authorizationNumber == accessKey.value) {
            "Offline SRI authorization number must match access key."
        }
    }
}
