# Fase 12.21 — Smoke test integral API/Admin Facturación Electrónica

## Objetivo

Ejecutar un recorrido manual, repetible y documentado para validar que la API de Facturación Electrónica/SRI funciona de punta a punta desde la perspectiva de Admin.

Este smoke test no reemplaza tests automatizados ni homologación oficial. Sirve para detectar problemas de wiring, permisos, headers, organización activa, DTOs, descargas y flujos principales.

---

## Supuestos

Antes de ejecutar:

- Backend levantado local/staging.
- MongoDB disponible.
- Usuario owner/admin creado.
- Organización activa creada.
- Venta/documento comercial elegible para facturar.
- Permisos de facturación electrónica asignados al usuario.
- Feature flag global de producción apagado salvo prueba controlada.
- Ambiente SRI TEST configurado para smoke test.
- Firma electrónica de prueba cargada o mock/fake configurado según ambiente.
- Email provider en modo sandbox/mock si aplica.

---

## Variables necesarias

| Variable | Descripción |
|---|---|
| `BASE_URL` | URL base del backend |
| `ACCESS_TOKEN` | Token válido de usuario admin |
| `ORGANIZATION_ID` | Organización activa |
| `SALE_ID` | Venta/documento comercial elegible |
| `DOCUMENT_ID` | Documento electrónico emitido |
| `SIGNATURE_ID` | Firma electrónica cargada |
| `SEQUENCE_ID` | Secuencia activa |
| `HOMOLOGATION_RUN_ID` | Ejecución de homologación |

Header recomendado:

```http
Authorization: Bearer {{ACCESS_TOKEN}}
X-Hermes-Organization-Id: {{ORGANIZATION_ID}}
Content-Type: application/json
```

---

## Orden del smoke test

### 1. Validar sesión y organización activa

- [ ] Login o token válido.
- [ ] `/me` devuelve organización activa o permite resolverla por header.
- [ ] Usuario tiene permisos `DOCUMENTS_ELECTRONIC_INVOICE_*` necesarios.

Resultado esperado:

- HTTP 200.
- Organización activa correcta.
- Permisos disponibles.

---

### 2. Consultar configuración SRI

Endpoint:

```http
GET /api/v1/sri/settings
```

Validar:

- [ ] Devuelve settings actuales o estado no configurado.
- [ ] No devuelve secretos.
- [ ] No expone rutas internas.

---

### 3. Actualizar configuración SRI TEST

Endpoint:

```http
PUT /api/v1/sri/settings
```

Validar:

- [ ] Guarda RUC, razón social, direcciones, establecimiento, punto de emisión.
- [ ] Guarda endpoints por ambiente desde configuración, no quemados en código.
- [ ] `productionEnabled` permanece `false`.

---

### 4. Consultar readiness

Endpoint:

```http
GET /api/v1/sri/settings/readiness
```

Validar:

- [ ] Devuelve checks individuales.
- [ ] Marca faltantes como `BLOCKING` o equivalente.
- [ ] Antes de firma/secuencia debe mostrar no listo.
- [ ] Después de firma/secuencia debe pasar para TEST.

---

### 5. Cargar firma electrónica

Endpoint:

```http
POST /api/v1/electronic-signatures
```

Validar:

- [ ] Permite crear credencial de firma.
- [ ] No devuelve bytes, password, private key ni object key interno.
- [ ] Devuelve metadata segura: alias, subject, issuer, serial, fingerprint, validFrom, validTo, status.

---

### 6. Validar y activar firma

Endpoints:

```http
POST /api/v1/electronic-signatures/{signatureId}/validate
POST /api/v1/electronic-signatures/{signatureId}/activate
```

Validar:

- [ ] Firma válida queda activa.
- [ ] Firma vencida o inválida queda bloqueada.
- [ ] Activar una firma desactiva la anterior si aplica.

---

### 7. Asegurar secuencia

Endpoint:

```http
POST /api/v1/electronic-sequences/ensure
```

Validar:

- [ ] Crea o devuelve secuencia activa.
- [ ] No decrementa secuencial.
- [ ] Respeta ambiente, tipo de documento, establecimiento y punto de emisión.

---

### 8. Readiness final antes de emitir

Endpoint:

```http
GET /api/v1/sri/settings/readiness
```

Resultado esperado:

- [ ] Ready para TEST.
- [ ] Producción sigue bloqueada.

---

### 9. Emitir factura electrónica desde venta

Endpoint:

```http
POST /api/v1/electronic-invoices
```

Validar:

- [ ] Valida organización activa.
- [ ] Valida permiso de emisión.
- [ ] Valida venta elegible.
- [ ] Evita doble factura autorizada para la misma venta.
- [ ] Genera clave de acceso.
- [ ] Genera XML.
- [ ] Valida XSD.
- [ ] Firma XML.
- [ ] Envía a recepción SRI o fake/mock según ambiente.
- [ ] Persiste estado, artefactos y respuesta.

Estados esperados posibles:

- `AUTHORIZED`
- `AUTHORIZATION_PENDING`
- `RECEIVED_BY_SRI`
- `RETURNED_BY_SRI`
- `XSD_INVALID`
- `REJECTED`

---

### 10. Listar facturas electrónicas

Endpoint:

```http
GET /api/v1/electronic-invoices
```

Validar:

- [ ] Lista solo documentos de la organización activa.
- [ ] Soporta filtros principales si fueron implementados.
- [ ] No expone object keys internos.

---

### 11. Consultar detalle

Endpoint:

```http
GET /api/v1/electronic-invoices/{documentId}
```

Validar:

- [ ] Devuelve accessKey, estado, número, autorización si existe.
- [ ] Devuelve disponibilidad de artefactos.
- [ ] Devuelve mensajes SRI de forma segura.

---

### 12. Consultar errores SRI

Endpoint:

```http
GET /api/v1/electronic-invoices/{documentId}/errors
```

Validar:

- [ ] Devuelve errores comprensibles para Admin.
- [ ] No expone payloads sensibles innecesarios.

---

### 13. Reintentar autorización si aplica

Endpoint:

```http
POST /api/v1/electronic-invoices/{documentId}/retry-authorization
```

Validar:

- [ ] Solo permite `AUTHORIZATION_PENDING` o `RECEIVED_BY_SRI`.
- [ ] No reenvía XML.
- [ ] Consulta autorización por clave de acceso.
- [ ] Bloquea si ya está `AUTHORIZED`.

---

### 14. Generar RIDE

Endpoint:

```http
POST /api/v1/electronic-invoices/{documentId}/ride
```

Validar:

- [ ] Solo genera desde XML autorizado.
- [ ] Persiste PDF.
- [ ] Actualiza disponibilidad de RIDE.

---

### 15. Descargar XML autorizado

Endpoint:

```http
GET /api/v1/electronic-invoices/{documentId}/authorized-xml
```

Headers esperados:

```http
Content-Type: application/xml; charset=UTF-8
Content-Disposition: attachment; filename="..._authorized.xml"
```

---

### 16. Descargar XML firmado

Endpoint:

```http
GET /api/v1/electronic-invoices/{documentId}/signed-xml
```

Validar:

- [ ] Requiere permiso fuerte `DOWNLOAD_XML`.
- [ ] No está disponible para usuarios operativos básicos.

---

### 17. Descargar RIDE PDF

Endpoint:

```http
GET /api/v1/electronic-invoices/{documentId}/ride.pdf
```

Headers esperados:

```http
Content-Type: application/pdf
Content-Disposition: attachment; filename="..._ride.pdf"
```

---

### 18. Enviar email al cliente

Endpoint:

```http
POST /api/v1/electronic-invoices/{documentId}/email
```

Validar:

- [ ] Adjunta XML autorizado.
- [ ] Adjunta RIDE PDF.
- [ ] Si no existe RIDE, lo genera automáticamente.
- [ ] Registra delivery status.
- [ ] Registra error si falla.

---

### 19. Consultar timeline

Endpoint:

```http
GET /api/v1/electronic-invoices/{documentId}/timeline
```

Validar:

- [ ] Eventos ordenados.
- [ ] Incluye emisión, firma, envío, autorización, RIDE, email, retry.
- [ ] Filtra por organización.

---

### 20. Homologación readiness

Endpoint:

```http
GET /api/v1/electronic-invoices/homologation/readiness
```

Validar:

- [ ] Solo TEST.
- [ ] Señala faltantes.
- [ ] Producción bloqueada si no hay homologación aprobada.

---

### 21. Ejecutar homologación

Endpoint:

```http
POST /api/v1/electronic-invoices/homologation/run
```

Validar:

- [ ] Crea run.
- [ ] Guarda escenarios.
- [ ] Genera reporte.
- [ ] No habilita producción automáticamente.

---

### 22. Consultar runs y reporte

Endpoints:

```http
GET /api/v1/electronic-invoices/homologation/runs
GET /api/v1/electronic-invoices/homologation/runs/{runId}
GET /api/v1/electronic-invoices/homologation/runs/{runId}/report.md
```

Validar:

- [ ] Lista por organización.
- [ ] Detalle incluye escenarios y resultado.
- [ ] Reporte markdown descargable/visible.

---

### 23. Validar gate de producción bloqueado

Endpoint:

```http
POST /api/v1/sri/settings/enable-production
```

Con feature flag global apagado, debe fallar.

Resultado esperado:

- [ ] HTTP 400/403 según convención.
- [ ] Mensaje claro: producción no está habilitada globalmente o faltan requisitos.

---

## Resultado esperado final

Al terminar este smoke test:

- Admin puede preparar una organización para facturar en TEST.
- Admin puede emitir factura desde venta.
- Admin puede operar errores, retry, RIDE, descargas, email y timeline.
- Admin puede correr homologación técnica.
- Producción sigue bloqueada hasta cumplir gate.

