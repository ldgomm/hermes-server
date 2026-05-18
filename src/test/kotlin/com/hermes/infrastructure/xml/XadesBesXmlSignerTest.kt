package com.hermes.infrastructure.xml

import com.hermes.application.electronicinvoicing.SignXmlCommand
import com.hermes.domain.shared.DomainRuleViolation
import com.hermes.infrastructure.security.Pkcs12SigningKeyMaterialLoader
import com.hermes.testing.electronicinvoicing.TestPkcs12Fixture
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class XadesBesXmlSignerTest {
    private val loader = Pkcs12SigningKeyMaterialLoader()
    private val signer = XadesBesXmlSigner()

    @Test
    fun `signs factura xml with enveloped ds signature and xades object`() {
        val material = loader.loadPkcs12(TestPkcs12Fixture.content(), TestPkcs12Fixture.password())
        val signed = signer.sign(
            SignXmlCommand(
                organizationId = "org_1",
                signatureId = "sig_1",
                xml = unsignedFacturaXml().toByteArray(Charsets.UTF_8),
                accessKey = "1234567890123456789012345678901234567890123456789",
                signedAt = Instant.parse("2026-05-18T10:15:30Z"),
            ),
            material,
        )

        val text = signed.signedXmlText
        assertTrue(text.contains("<ds:Signature"), text)
        assertTrue(text.contains("QualifyingProperties"), text)
        assertTrue(text.contains("SignedProperties"), text)
        assertTrue(text.contains("SigningTime"), text)
        assertTrue(text.contains("id=\"comprobante\""), text)
        assertTrue(signed.xadesBesObjectIncluded)
        assertTrue(signed.signedXmlSha256.length == 64)
        assertTrue(signed.certificateFingerprintSha256.length == 64)
    }

    @Test
    fun `rejects already signed xml`() {
        val material = loader.loadPkcs12(TestPkcs12Fixture.content(), TestPkcs12Fixture.password())

        assertFailsWith<DomainRuleViolation> {
            signer.sign(
                SignXmlCommand(
                    organizationId = "org_1",
                    signatureId = "sig_1",
                    xml = signedXml().toByteArray(Charsets.UTF_8),
                ),
                material,
            )
        }
    }

    private fun unsignedFacturaXml(): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <factura id="comprobante" version="2.0.0">
          <infoTributaria>
            <ambiente>1</ambiente>
            <tipoEmision>1</tipoEmision>
            <razonSocial>HERMES TEST</razonSocial>
            <ruc>1790012345001</ruc>
            <claveAcceso>1234567890123456789012345678901234567890123456789</claveAcceso>
            <codDoc>01</codDoc>
            <estab>001</estab>
            <ptoEmi>001</ptoEmi>
            <secuencial>000000001</secuencial>
            <dirMatriz>Quito</dirMatriz>
          </infoTributaria>
          <infoFactura>
            <fechaEmision>18/05/2026</fechaEmision>
            <tipoIdentificacionComprador>07</tipoIdentificacionComprador>
            <razonSocialComprador>CONSUMIDOR FINAL</razonSocialComprador>
            <identificacionComprador>9999999999999</identificacionComprador>
            <totalSinImpuestos>1.00</totalSinImpuestos>
            <totalDescuento>0.00</totalDescuento>
            <totalConImpuestos>
              <totalImpuesto>
                <codigo>2</codigo>
                <codigoPorcentaje>0</codigoPorcentaje>
                <baseImponible>1.00</baseImponible>
                <valor>0.00</valor>
              </totalImpuesto>
            </totalConImpuestos>
            <importeTotal>1.00</importeTotal>
          </infoFactura>
          <detalles>
            <detalle>
              <descripcion>Producto test</descripcion>
              <cantidad>1.00</cantidad>
              <precioUnitario>1.00</precioUnitario>
              <descuento>0.00</descuento>
              <precioTotalSinImpuesto>1.00</precioTotalSinImpuesto>
              <impuestos>
                <impuesto>
                  <codigo>2</codigo>
                  <codigoPorcentaje>0</codigoPorcentaje>
                  <tarifa>0.00</tarifa>
                  <baseImponible>1.00</baseImponible>
                  <valor>0.00</valor>
                </impuesto>
              </impuestos>
            </detalle>
          </detalles>
        </factura>
    """.trimIndent()

    private fun signedXml(): String = """
        <factura id="comprobante" version="2.0.0" xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
          <ds:Signature></ds:Signature>
        </factura>
    """.trimIndent()
}
