# Fase 12 — Release notes técnicas

## Resumen

La Fase 12 convierte el motor SRI construido en Fase 11 en una API operable por usuarios Admin y futuras apps Admin iOS/Web.

La fase queda cerrada a nivel backend/API si los tests pasan y el smoke test integral confirma operación básica.

---

## Incluido

### API de facturación electrónica

- Emisión de factura electrónica desde venta/documento comercial.
- Consulta de detalle.
- Listado por organización.
- Consulta de errores SRI/Admin.
- Reintento controlado de autorización.
- Generación/regeneración de RIDE.
- Descarga de XML autorizado.
- Descarga de XML firmado bajo permiso fuerte.
- Descarga de RIDE PDF.
- Envío/reenvío de email.
- Timeline/auditoría operativa por documento.

### Configuración SRI

- Settings SRI por organización.
- Readiness check.
- Gate de producción.
- Bloqueo por feature flag global.

### Firma electrónica

- Carga/registro de firma.
- Validación.
- Activación.
- Revocación.
- Listado y detalle seguro.

### Secuenciales

- Ensure sequence.
- Listado.
- Detalle.
- Protección contra decrementos inseguros.

### Homologación

- Readiness de homologación.
- Ejecución de run.
- Listado de runs.
- Detalle de run.
- Reporte markdown.

### Mongo

- Repositorios de operación electrónica.
- Índices de documentos, settings, firmas, secuenciales, homologación y auditoría.

### Documentación

- Endpoints.
- Flujos Admin.
- Checklist de producción.
- Smoke test.
- Demo flow HTTP.
- Variables de entorno.

---

## No incluido todavía

Estos módulos quedan fuera de Fase 12:

- Notas de crédito.
- Notas de débito.
- Retenciones.
- Guías de remisión.
- Anulación SRI.
- Factura comercial negociable.
- ATS.
- Reportes tributarios avanzados.
- Portal público de comprobantes.
- Envío por WhatsApp.
- Cola avanzada/distribuida de jobs.
- Scheduler distribuido.
- Multi-provider de email.
- Admin General API.
- Admin iOS completo.

---

## Riesgos conocidos

| Riesgo | Mitigación |
|---|---|
| Homologación oficial pendiente | Ejecutar con RUC/firma/settings reales contra ambiente SRI correspondiente |
| Producción mal habilitada | Mantener `SRI_PRODUCTION_GLOBALLY_ENABLED=false` por defecto y exigir gate por organización |
| Exposición de secretos | Revisar DTOs, logs, exceptions y storage object keys |
| Secuenciales incorrectos | Probar concurrencia y transacciones antes de producción |
| Cambios normativos | Verificar SRI antes de liberar a clientes reales |
| Email fallido | Registrar delivery status y permitir reintento |
| Artefactos corruptos o ausentes | Smoke test de descargas + validación de hashes si aplica |

---

## Próxima fase recomendada

**Fase 13 — Admin General API**

Objetivo: exponer APIs administrativas generales para que Admin iOS/Web Admin puedan administrar el negocio completo: configuración, actividades, sucursales, usuarios, roles, catálogo, tax engine, caja, ventas, reportes y auditoría global.

