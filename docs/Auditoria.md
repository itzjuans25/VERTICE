# Auditoría TechStore

Mediciones locales del 22 de noviembre de 2026. Los resultados anteriores se conservan como evidencia histórica.

| Requisito del profesor | Estado | Evidencia |
| --- | --- | --- |
| Core e-commerce | Cumple | Registro, inventario, carrito, pedido 2151 e historial. |
| Tres microservicios | Cumple | Auth, Catálogo y Notificaciones con HTTP y Swagger independientes. |
| Base de datos | Cumple | PostgreSQL. 45 productos en 11 categorías, todos con imagen y descripción. [Verificación](../resultados/pgadmin-local.txt) |
| Notificaciones | Cumple local | RabbitMQ, Mailpit y SMS simulado. Confirmación recibida para el pedido 2151. |
| Cinco APIs no CRUD por servicio | Cumple | [Rutas y lógica](../README.md#cinco-apis-no-crud-por-microservicio) |
| 80 % por microservicio | Cumple en suite completa | Auth 85,28 %, Catálogo 88,49 %, Notificaciones 82,95 %. La suite incluye pruebas unitarias e integración. |
| Dos APIs externas | Cumple | Frankfurter y Nager.Date respondieron en Newman y en el calentamiento de k6. |
| Despliegue completo | Cumple local | Windows activo con PostgreSQL, RabbitMQ y Mailpit. Blueprint Render preparado sin publicación. |
| Carga y estrés | Cumple | 36 rutas con 3, 6 y 8 usuarios. [Desglose](../resultados/latencias-techstore.md) |
| Máximo de 500 ms | No cumple en todos los perfiles | Mayor máximo observado en la revisión: 1086,76 ms. Véase el detalle por ruta. |
| Artículo IEEE | Cumple alcance actualizado | Versión en IEEEtran de dos columnas con métricas nuevas, diagramas y evidencia local. [PDF](../output/pdf/articulo-ieee.pdf) |

## Resultados

Pasaron **76 pruebas Java** y **12 React**. JaCoCo de líneas en la suite Java completa: **Auth 85,28 %, Catálogo 88,49 %, Notificaciones 82,95 %**.

El pedido #2151 de Portátil ThinkPad T480 por 850.000 COP se confirmó en el navegador y se encontró el correo correspondiente en Mailpit.

Newman ejecutó 48 peticiones y 145 aserciones. Hubo 0 aserciones fallidas en la corrida de revisión, con máximo 194,00 ms. En la primera corrida de la revisión hubo 2 fallos de tiempo y un máximo de 1354,00 ms.

| Usuarios | Promedio ms | p95 ms | p99 ms | Máximo ms | Errores | Peticiones | req/s |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | 29,01 | 124,80 | 218,55 | 344,08 | 0 | 1665 | 61,16 |
| 6 | 23,84 | 114,04 | 211,19 | 284,96 | 0 | 3996 | 104,42 |
| 8 | 38,78 | 192,45 | 463,72 | 1086,76 | 0 | 3700 | 96,00 |

- **3 usuarios.** Rutas por encima de 500 ms: ninguna. Cambio de contraseña: máximo 257,77 ms y p95 247,55 ms.
- **6 usuarios.** Rutas por encima de 500 ms: ninguna. Cambio de contraseña: máximo 284,96 ms y p95 244,85 ms.
- **8 usuarios.** Rutas por encima de 500 ms: POST /save (861,08 ms), POST /login (591,87 ms), POST /renovar (629,82 ms), POST /cambiar-clave (1086,76 ms). Cambio de contraseña: máximo 1086,76 ms y p95 826,43 ms.

Argon2id conserva 19 MiB, dos iteraciones y paralelismo uno. Auth verifica la credencial previa y deriva la nueva fuera de la transacción de escritura. Las muestras HTTP muestran el costo conjunto y la cola en concurrencia. La instrumentación no atribuye por separado los máximos a CPU, GC o PostgreSQL.

Render es opcional para la entrega local. Para obtener un enlace público hace falta conectar el repositorio en tu cuenta, crear RabbitMQ en CloudAMQP y añadir un remitente y credenciales de Brevo. [Pasos](Render.md)
