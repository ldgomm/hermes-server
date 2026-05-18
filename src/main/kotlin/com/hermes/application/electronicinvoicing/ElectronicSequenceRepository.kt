package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.ElectronicSequence
import com.hermes.domain.electronicinvoicing.ElectronicSequenceKey
import com.hermes.domain.electronicinvoicing.ElectronicSequenceReservation

interface ElectronicSequenceRepository {
    fun createIfMissing(sequence: ElectronicSequence): ElectronicSequence
    fun findByKey(key: ElectronicSequenceKey): ElectronicSequence?
    fun nextSequential(command: NextElectronicSequentialCommand): ElectronicSequenceReservation
}
