package com.hermes.application.electronicinvoicing

import com.hermes.domain.electronicinvoicing.SriInvoicePaymentForm
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SriInvoiceXmlModelsTest {
    @Test
    fun `accepts valid SRI payment form code`() {
        assertEquals("20", SriInvoicePaymentForm("20").code)
    }

    @Test
    fun `rejects invalid SRI payment form code`() {
        assertFailsWith<DomainRuleViolation> { SriInvoicePaymentForm("22") }
        assertFailsWith<DomainRuleViolation> { SriInvoicePaymentForm("1") }
    }

    @Test
    fun `rejects payment total with more than two decimals`() {
        assertFailsWith<DomainRuleViolation> {
            SriInvoicePayment(SriInvoicePaymentForm.WITHOUT_FINANCIAL_SYSTEM, BigDecimal("10.123"))
        }
    }

    @Test
    fun `rejects invoice detail with more than six quantity decimals`() {
        assertFailsWith<DomainRuleViolation> {
            SriInvoiceDetail(
                codigoPrincipal = "ITEM",
                descripcion = "Item",
                cantidad = BigDecimal("1.1234567"),
                precioUnitario = BigDecimal("10.000000"),
                descuento = BigDecimal("0.00"),
                precioTotalSinImpuesto = BigDecimal("10.00"),
                impuestos = listOf(
                    SriInvoiceDetailTax(
                        codigo = "2",
                        codigoPorcentaje = "0",
                        tarifa = BigDecimal("0.00"),
                        baseImponible = BigDecimal("10.00"),
                        valor = BigDecimal("0.00"),
                    )
                )
            )
        }
    }
}
