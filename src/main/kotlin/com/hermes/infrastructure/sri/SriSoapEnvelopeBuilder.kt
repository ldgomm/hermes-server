package com.hermes.infrastructure.sri

import java.util.*

object SriSoapEnvelopeBuilder {
    fun receptionEnvelope(signedXml: ByteArray): String {
        val encodedXml = Base64.getEncoder().encodeToString(signedXml)
        return """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:rec="http://ec.gob.sri.ws.recepcion">
              <soapenv:Header/>
              <soapenv:Body>
                <rec:validarComprobante>
                  <xml>$encodedXml</xml>
                </rec:validarComprobante>
              </soapenv:Body>
            </soapenv:Envelope>
        """.trimIndent()
    }

    fun authorizationEnvelope(accessKey: String): String = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:aut="http://ec.gob.sri.ws.autorizacion">
          <soapenv:Header/>
          <soapenv:Body>
            <aut:autorizacionComprobante>
              <claveAccesoComprobante>$accessKey</claveAccesoComprobante>
            </aut:autorizacionComprobante>
          </soapenv:Body>
        </soapenv:Envelope>
    """.trimIndent()
}
