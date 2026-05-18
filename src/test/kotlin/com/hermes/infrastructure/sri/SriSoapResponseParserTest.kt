package com.hermes.infrastructure.sri

import com.hermes.domain.electronicinvoicing.SriAuthorizationStatus
import com.hermes.domain.electronicinvoicing.SriEnvironment
import com.hermes.domain.electronicinvoicing.SriReceptionStatus
import com.hermes.domain.shared.DomainRuleViolation
import kotlin.test.*

class SriSoapResponseParserTest {
    private val parser = SriSoapResponseParser()
    private val accessKey = testAccessKey()

    @Test
    fun `parses received reception response`() {
        val result = parser.parseReception(SriEnvironment.TEST, receivedReceptionResponse(), accessKey)

        assertEquals(SriReceptionStatus.RECEIVED, result.status)
        assertEquals(accessKey, result.accessKey)
        assertTrue(result.messages.isEmpty())
    }

    @Test
    fun `parses returned reception response with messages`() {
        val result = parser.parseReception(SriEnvironment.TEST, returnedReceptionResponse(), accessKey)

        assertEquals(SriReceptionStatus.RETURNED, result.status)
        assertEquals(1, result.messages.size)
        assertEquals("35", result.messages.first().identifier)
        assertEquals("DOCUMENTO INVALIDO", result.messages.first().message)
    }

    @Test
    fun `parses authorized authorization response`() {
        val result = parser.parseAuthorization(SriEnvironment.TEST, accessKey, authorizedResponse(accessKey.value))

        assertEquals(SriAuthorizationStatus.AUTHORIZED, result.status)
        assertEquals(accessKey.value, result.authorizationNumber)
        assertNotNull(result.authorizedAt)
        assertTrue(result.authorizedXml!!.contains("<factura"))
    }

    @Test
    fun `parses rejected authorization response with messages`() {
        val result = parser.parseAuthorization(SriEnvironment.TEST, accessKey, rejectedResponse(accessKey.value))

        assertEquals(SriAuthorizationStatus.NOT_AUTHORIZED, result.status)
        assertEquals(1, result.messages.size)
        assertEquals("ERROR", result.messages.first().type.name)
    }

    @Test
    fun `parses processing authorization response when there is no authorization node`() {
        val result = parser.parseAuthorization(SriEnvironment.TEST, accessKey, processingResponse(accessKey.value))

        assertEquals(SriAuthorizationStatus.PROCESSING, result.status)
    }

    @Test
    fun `rejects malformed soap response`() {
        assertFailsWith<DomainRuleViolation> {
            parser.parseReception(SriEnvironment.TEST, "<soap>", accessKey)
        }
    }
}

internal fun receivedReceptionResponse(): String = """
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
        <validarComprobanteResponse>
          <RespuestaRecepcionComprobante>
            <estado>RECIBIDA</estado>
          </RespuestaRecepcionComprobante>
        </validarComprobanteResponse>
      </soap:Body>
    </soap:Envelope>
""".trimIndent()

internal fun returnedReceptionResponse(): String = """
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
        <validarComprobanteResponse>
          <RespuestaRecepcionComprobante>
            <estado>DEVUELTA</estado>
            <comprobantes>
              <comprobante>
                <mensajes>
                  <mensaje>
                    <identificador>35</identificador>
                    <mensaje>DOCUMENTO INVALIDO</mensaje>
                    <informacionAdicional>La clave de acceso es inválida</informacionAdicional>
                    <tipo>ERROR</tipo>
                  </mensaje>
                </mensajes>
              </comprobante>
            </comprobantes>
          </RespuestaRecepcionComprobante>
        </validarComprobanteResponse>
      </soap:Body>
    </soap:Envelope>
""".trimIndent()

internal fun authorizedResponse(accessKey: String): String = """
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
        <autorizacionComprobanteResponse>
          <RespuestaAutorizacionComprobante>
            <claveAccesoConsultada>$accessKey</claveAccesoConsultada>
            <numeroComprobantes>1</numeroComprobantes>
            <autorizaciones>
              <autorizacion>
                <estado>AUTORIZADO</estado>
                <numeroAutorizacion>$accessKey</numeroAutorizacion>
                <fechaAutorizacion>2026-05-18T12:00:00-05:00</fechaAutorizacion>
                <ambiente>PRUEBAS</ambiente>
                <comprobante><![CDATA[<factura id="comprobante" version="2.0.0"/>]]></comprobante>
              </autorizacion>
            </autorizaciones>
          </RespuestaAutorizacionComprobante>
        </autorizacionComprobanteResponse>
      </soap:Body>
    </soap:Envelope>
""".trimIndent()

internal fun rejectedResponse(accessKey: String): String = """
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
        <autorizacionComprobanteResponse>
          <RespuestaAutorizacionComprobante>
            <claveAccesoConsultada>$accessKey</claveAccesoConsultada>
            <numeroComprobantes>1</numeroComprobantes>
            <autorizaciones>
              <autorizacion>
                <estado>RECHAZADO</estado>
                <mensajes>
                  <mensaje>
                    <identificador>43</identificador>
                    <mensaje>CLAVE ACCESO REGISTRADA</mensaje>
                    <informacionAdicional>La clave de acceso ya existe</informacionAdicional>
                    <tipo>ERROR</tipo>
                  </mensaje>
                </mensajes>
              </autorizacion>
            </autorizaciones>
          </RespuestaAutorizacionComprobante>
        </autorizacionComprobanteResponse>
      </soap:Body>
    </soap:Envelope>
""".trimIndent()

internal fun processingResponse(accessKey: String): String = """
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
        <autorizacionComprobanteResponse>
          <RespuestaAutorizacionComprobante>
            <claveAccesoConsultada>$accessKey</claveAccesoConsultada>
            <numeroComprobantes>0</numeroComprobantes>
            <estado>PPR</estado>
          </RespuestaAutorizacionComprobante>
        </autorizacionComprobanteResponse>
      </soap:Body>
    </soap:Envelope>
""".trimIndent()
