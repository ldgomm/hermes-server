package com.hermes.infrastructure.xml

import com.hermes.application.electronicinvoicing.*
import com.hermes.domain.electronicinvoicing.*
import com.hermes.domain.shared.DomainRuleViolation
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.*

class SriInvoiceXmlBuilderTest {
    private val builder = SriInvoiceXmlBuilder()

    @Test
    fun `builds unsigned invoice xml with required sections`() {
        val result = builder.build(validCommand())

        assertEquals("factura_V2.1.0", result.schemaVersion.schemaVersionCode)
        assertTrue(result.xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(result.xml.contains("<factura id=\"comprobante\" version=\"2.1.0\">"))
        assertTrue(result.xml.contains("<infoTributaria>"))
        assertTrue(result.xml.contains("<ambiente>1</ambiente>"))
        assertTrue(result.xml.contains("<tipoEmision>1</tipoEmision>"))
        assertTrue(result.xml.contains("<codDoc>01</codDoc>"))
        assertTrue(result.xml.contains("<estab>001</estab>"))
        assertTrue(result.xml.contains("<ptoEmi>002</ptoEmi>"))
        assertTrue(result.xml.contains("<secuencial>000000123</secuencial>"))
        assertTrue(result.xml.contains("<infoFactura>"))
        assertTrue(result.xml.contains("<fechaEmision>18/05/2026</fechaEmision>"))
        assertTrue(result.xml.contains("<tipoIdentificacionComprador>07</tipoIdentificacionComprador>"))
        assertTrue(result.xml.contains("<identificacionComprador>9999999999999</identificacionComprador>"))
        assertTrue(result.xml.contains("<totalSinImpuestos>24.00</totalSinImpuestos>"))
        assertTrue(result.xml.contains("<importeTotal>27.60</importeTotal>"))
        assertTrue(result.xml.contains("<pagos>"))
        assertTrue(result.xml.contains("<formaPago>01</formaPago>"))
        assertTrue(result.xml.contains("<detalles>"))
        assertTrue(result.xml.contains("<codigoPrincipal>CUY-ENTERO</codigoPrincipal>"))
        assertTrue(result.xml.contains("<cantidad>1</cantidad>"))
        assertTrue(result.xml.contains("<precioUnitario>24</precioUnitario>"))
        assertTrue(result.xml.contains("<infoAdicional>"))
        assertTrue(result.xml.contains("<campoAdicional nombre=\"Email\">cliente@example.com</campoAdicional>"))
        assertFalse(result.xml.contains("<ds:Signature"))
        assertEquals(64, result.sha256.length)
    }

    @Test
    fun `escapes xml text and attributes`() {
        val result = builder.build(
            validCommand(
                buyerName = "Cliente & Hijos <Test>",
                detailDescription = "Cuy & papas <especial>",
                additionalName = "Nota \"cliente\"",
                additionalValue = "A&B <C>",
            )
        )

        assertTrue(result.xml.contains("Cliente &amp; Hijos &lt;Test&gt;"))
        assertTrue(result.xml.contains("Cuy &amp; papas &lt;especial&gt;"))
        assertTrue(result.xml.contains("nombre=\"Nota &quot;cliente&quot;\""))
        assertTrue(result.xml.contains("A&amp;B &lt;C&gt;"))
    }

    @Test
    fun `renders optional accounting and rimpe fields when present`() {
        val result = builder.build(
            validCommand(
                obligadoContabilidad = SriAccountingObligation.NO,
                rimpeLegend = SriInvoiceRimpeLegend.CONTRIBUYENTE_REGIMEN_RIMPE,
            )
        )

        assertTrue(result.xml.contains("<obligadoContabilidad>NO</obligadoContabilidad>"))
        assertTrue(result.xml.contains("<contribuyenteRimpe>CONTRIBUYENTE RÉGIMEN RIMPE</contribuyenteRimpe>"))
    }

    @Test
    fun `rejects invoice whose subtotals do not match detail subtotals`() {
        assertFailsWith<DomainRuleViolation> {
            validCommand(totalSinImpuestos = bd("25.00"))
        }
    }

    @Test
    fun `rejects invoice whose payments do not match importeTotal`() {
        assertFailsWith<DomainRuleViolation> {
            validCommand(paymentTotal = bd("20.00"))
        }
    }

    @Test
    fun `rejects more than fifteen additional fields`() {
        assertFailsWith<DomainRuleViolation> {
            validCommand(additionalFields = (1..16).map { SriInvoiceAdditionalField("Campo $it", "Valor $it") })
        }
    }

    private fun validCommand(
        buyerName: String = "Consumidor final",
        detailDescription: String = "Cuy entero asado",
        additionalName: String = "Email",
        additionalValue: String = "cliente@example.com",
        totalSinImpuestos: BigDecimal = bd("24.00"),
        paymentTotal: BigDecimal = bd("27.60"),
        obligadoContabilidad: SriAccountingObligation? = null,
        rimpeLegend: SriInvoiceRimpeLegend = SriInvoiceRimpeLegend.NONE,
        additionalFields: List<SriInvoiceAdditionalField> = listOf(
            SriInvoiceAdditionalField(
                additionalName,
                additionalValue
            )
        ),
    ): BuildSriInvoiceXmlCommand {
        val issuedDate = LocalDate.of(2026, 5, 18)
        val series = SriSeries(establishmentCode = "001", emissionPointCode = "002")
        val sequential = SriSequential(123)
        val accessKey = SriAccessKeyGenerator.generate(
            SriAccessKeyGenerationCommand(
                issuedDate = issuedDate,
                documentType = SriDocumentType.INVOICE,
                ruc = "1790012345001",
                environment = SriEnvironment.TEST,
                series = series,
                sequential = sequential,
                numericCode = SriNumericCode("12345678"),
            )
        )
        return BuildSriInvoiceXmlCommand(
            infoTributaria = SriInvoiceTaxInfo(
                environment = SriEnvironment.TEST,
                razonSocial = "ALTOS DEL MURCO",
                nombreComercial = "Altos del Murco",
                ruc = "1790012345001",
                accessKey = accessKey,
                series = series,
                sequential = sequential,
                dirMatriz = "Tambillo",
                contribuyenteRimpe = rimpeLegend,
            ),
            infoFactura = SriInvoiceInfo(
                fechaEmision = issuedDate,
                dirEstablecimiento = "Tambillo km 1",
                obligadoContabilidad = obligadoContabilidad,
                buyerIdentificationType = SriIdentificationType.FINAL_CONSUMER,
                buyerLegalName = buyerName,
                buyerIdentification = SriIdentificationType.FINAL_CONSUMER_IDENTIFICATION,
                totalSinImpuestos = totalSinImpuestos,
                totalDescuento = bd("0.00"),
                totalConImpuestos = listOf(
                    SriInvoiceTotalTax(
                        codigo = "2",
                        codigoPorcentaje = "4",
                        baseImponible = bd("24.00"),
                        tarifa = bd("15.00"),
                        valor = bd("3.60"),
                    )
                ),
                importeTotal = bd("27.60"),
                pagos = listOf(SriInvoicePayment(SriInvoicePaymentForm.WITHOUT_FINANCIAL_SYSTEM, paymentTotal)),
            ),
            detalles = listOf(
                SriInvoiceDetail(
                    codigoPrincipal = "CUY-ENTERO",
                    descripcion = detailDescription,
                    cantidad = bd("1.000000"),
                    precioUnitario = bd("24.000000"),
                    descuento = bd("0.00"),
                    precioTotalSinImpuesto = bd("24.00"),
                    detallesAdicionales = listOf(SriInvoiceDetailAdditional("Porción", "Entero")),
                    impuestos = listOf(
                        SriInvoiceDetailTax(
                            codigo = "2",
                            codigoPorcentaje = "4",
                            tarifa = bd("15.00"),
                            baseImponible = bd("24.00"),
                            valor = bd("3.60"),
                        )
                    )
                )
            ),
            infoAdicional = additionalFields,
        )
    }

    private fun bd(value: String): BigDecimal = BigDecimal(value)
}
