package com.hermes.infrastructure.xml

import java.math.BigDecimal
import java.math.RoundingMode

internal object SriDecimalFormatter {
    fun money(value: BigDecimal): String = value.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
    fun quantity(value: BigDecimal): String =
        value.setScale(6, RoundingMode.UNNECESSARY).stripTrailingZeros().toPlainString()

    fun unitPrice(value: BigDecimal): String =
        value.setScale(6, RoundingMode.UNNECESSARY).stripTrailingZeros().toPlainString()

    fun rate(value: BigDecimal): String =
        value.setScale(2, RoundingMode.UNNECESSARY).stripTrailingZeros().toPlainString()

    fun term(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
}
