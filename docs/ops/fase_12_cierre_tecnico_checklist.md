# Fase 12.21 — Cierre técnico de Fase 12

## Objetivo

Validar que la **Fase 12 — API/Admin de Facturación Electrónica + Operación SRI** quedó cerrada a nivel backend/API, con contratos consumibles por Admin iOS/Web Admin, permisos aplicados, documentación operativa y smoke test integral.

> Este cierre técnico no equivale a homologación oficial ante el SRI en ambiente real. La homologación real requiere RUC, firma, endpoints/configuración y ejecución contra ambiente de pruebas real del SRI.

---

## Criterio de salida

La Fase 12 se considera cerrada cuando:

- [ ] `./gradlew clean test` está verde.
- [ ] Existen rutas Ktor para facturación electrónica.
- [ ] Todas las rutas requieren autenticación.
- [ ] Todas las rutas resuelven organización activa.
- [ ] Todas las rutas validan permisos específicos.
- [ ] Se puede consultar listado y detalle de facturas electrónicas.
- [ ] Se puede configurar SRI por organización.
- [ ] Se puede validar readiness.
- [ ] Se puede administrar firma electrónica sin exponer secretos.
- [ ] Se puede administrar secuenciales.
- [ ] Se puede emitir factura electrónica desde venta/documento comercial.
- [ ] Se puede reintentar autorización sin reenviar XML.
- [ ] Se pueden consultar errores SRI para Admin.
- [ ] Se puede generar/regenerar RIDE.
- [ ] Se pueden descargar XML/RIDE según permisos.
- [ ] Se puede enviar/reintentar email.
- [ ] Se puede consultar timeline/auditoría operativa del documento.
- [ ] Se puede ejecutar homologación desde API.
- [ ] Producción está bloqueada por gate global + gate por organización.
- [ ] Mongo tiene índices para operación electrónica.
- [ ] La documentación API/Admin/Ops está actualizada.
- [ ] Queda explícito qué NO soporta todavía la fase.

---

## Validaciones rápidas por bloque

### 12A — Contratos, permisos y rutas base

- [ ] `PermissionCatalog` contiene permisos específicos `DOCUMENTS_ELECTRONIC_INVOICE_*`.
- [ ] Los DTOs públicos no exponen entidades de dominio completas.
- [ ] Los DTOs no devuelven `objectKey`, rutas internas de storage, certificados ni secretos.
- [ ] `GET /api/v1/electronic-invoices` filtra por `organizationId`.
- [ ] `GET /api/v1/electronic-invoices/{documentId}` valida pertenencia a la organización activa.

### 12B — Configuración SRI y readiness

- [ ] `OrganizationSriSettings` está separado de `Organization`.
- [ ] `GET /api/v1/sri/settings/readiness` devuelve checks accionables.
- [ ] Firma electrónica nunca devuelve bytes, password, private key ni object key interno.
- [ ] Secuenciales no permiten decrementos ni cambios inseguros.

### 12C — Emisión y operación

- [ ] `POST /api/v1/electronic-invoices` valida readiness antes de emitir.
- [ ] La emisión evita doble factura autorizada para la misma venta.
- [ ] La emisión reserva secuencial de forma controlada.
- [ ] `POST /retry-authorization` no reenvía XML; solo consulta autorización por clave de acceso.
- [ ] `GET /errors` devuelve mensajes útiles para Admin sin filtrar detalles sensibles.

### 12D — Artefactos, RIDE y email

- [ ] RIDE se genera desde XML autorizado.
- [ ] XML autorizado y RIDE PDF se descargan con headers correctos.
- [ ] XML sin firmar/firmado requiere permiso fuerte.
- [ ] Email adjunta XML autorizado + RIDE PDF.
- [ ] Timeline ordena eventos por fecha y filtra por organización.

### 12E — Homologación y producción

- [ ] Homologación corre solo en ambiente TEST.
- [ ] Homologación guarda evidencia y reporte.
- [ ] `enable-production` exige confirmación fuerte.
- [ ] Producción depende de feature flag global y flag por organización.
- [ ] Producción queda apagada por defecto en ambientes no autorizados.

---

## Comandos finales

```bash
./gradlew clean test

./gradlew test --tests "*ElectronicInvoice*"
./gradlew test --tests "*Sri*"
./gradlew test --tests "*Homologation*"
./gradlew test --tests "*Signature*"
```

---

## Entregables de cierre

- `docs/ops/fase_12_smoke_test.md`
- `docs/ops/fase_12_demo_flow.http`
- `docs/ops/fase_12_env_vars.md`
- `docs/ops/fase_12_release_notes.md`
- `docs/ops/fase_12_cierre_tecnico_checklist.md`
- `prompts/fase_13_admin_general_api_prompt.md`
