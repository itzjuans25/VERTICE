# Auditoría VÉRTICE

## Galería editorial · ejecución local

| Requisito | Estado | Evidencia |
| --- | --- | --- |
| Núcleo de comercio | Cumple | Compra 2938 descontó la última unidad. Lote 661 verificó extensión, adjudicación 41 y pago idempotente. |
| Mínimo 3 microservicios | Cumple | Cuatro servicios Spring Boot con ArchUnit, contratos Swagger y health local. |
| Base de datos | Cumple | PostgreSQL conserva 45 productos del seed original, 18 lotes curados y la muestra de compras, pujas y adjudicaciones previas. |
| Sistema de notificación | Cumple | RabbitMQ y Mailpit entregaron los correos del lote y la compra. Dos JWT vieron avisos separados. |
| 5 APIs propias por servicio | Cumple | [Endpoints de cada servicio](../README.md#cinco-apis-no-crud-por-microservicio). |
| 80 % de cobertura unitaria | Parcial | Unidad 20,04 / 34,56 / 68,61 / 16,90 %. Con integración 82,44 / 86,97 / 81,88 / 92,14 %. Orden Auth, Catálogo, Notificaciones y Subastas. |
| 2 APIs externas | Cumple | Frankfurter, Nager.Date y Open-Meteo devolvieron datos en la corrida. |
| Despliegue completo | Parcial | Windows nativo en ejecución. Compose válido. El motor de Docker Desktop está apagado y las URLs públicas siguen pendientes. |
| Carga y estrés | Cumple | 50 rutas con k6 y 88 peticiones en Newman. |
| Máximo 500 ms | No cumple | 3 VU 417,65 ms, 6 VU 2404,86 ms, 8 VU 2494,37 ms. Newman registró 266,00 ms. |
| Artículo IEEE de hasta 4 páginas | Cumple | Versión corta de 4 páginas y versión extendida de 8, compiladas. |

[Resultados por ruta](../resultados/vertice-galeria-rendimiento.md) · [Cobertura](../resultados/vertice-galeria-cobertura.json) · [Conservación de datos](../resultados/vertice-galeria-datos.json) · [Capturas](capturas/)

## Corrida VÉRTICE · 25 de noviembre de 2026

| Requisito del profesor | Estado | Evidencia de esta corrida |
| --- | --- | --- |
| 1. Núcleo de comercio electrónico | Cumple | Compra 2772, stock de 1 a 0. Lote 421, dos postores, extensión y adjudicación 39 con pago simulado idempotente. [Compra](../resultados/vertice-compra.json) · [Subasta](../resultados/vertice-puja.json) |
| 2. Al menos 3 microservicios | Cumple | Auth, Catálogo, Notificaciones y Subastas. Maven multimódulo, reglas hexagonales con ArchUnit y cuatro contratos OpenAPI. [Módulos](../pom.xml) |
| 3. Base de datos | Cumple | PostgreSQL `ecommerce`, 45 fichas de Tecnología con la misma huella y 18 lotes curados. La segunda ejecución del seed no insertó ni modificó filas. [Datos](../resultados/vertice-datos.json) · [MER](pgadmin/vertice-mer.md) |
| 4. Notificaciones | Cumple | RabbitMQ y Mailpit confirmaron la compra, puja superada y adjudicación. El envío externo requiere credenciales del titular. [Capturas](capturas/) |
| 5. Cinco APIs propias por servicio | Cumple | Operaciones, reglas y rutas de los cuatro servicios en el README. [APIs](../README.md#cinco-apis-no-crud-por-microservicio) |
| 6. 80 % de cobertura unitaria por servicio | Parcial | JaCoCo de líneas unitarias, 20,04 / 34,56 / 68,61 / 16,90 %. Con integración, 82,44 / 86,97 / 81,88 / 92,14 % en el mismo orden. [Medición](../resultados/vertice-cobertura.json) |
| 7. Dos APIs externas | Cumple | Frankfurter, Nager.Date y Open-Meteo respondieron a través del sistema. [Respuestas](../resultados/vertice-integraciones.json) |
| 8. Despliegue completo | Parcial | Arranque local comprobado, configuración pública documentada. La publicación necesita las cuentas del titular. [Publicación](Render.md) |
| 9. Carga y estrés | Cumple | k6 con 3, 6 y 8 usuarios virtuales y comprobaciones funcionales Newman. [Rendimiento](../resultados/vertice-rendimiento.md) |
| 10. Máximo de 500 ms por API | No cumple | Newman observó un máximo de 3821 ms. [Newman](../resultados/vertice-postman.json) · [Latencia por ruta](../resultados/vertice-rendimiento.md) |
| 11. Artículo IEEEtran de hasta 4 páginas | Cumple | Versiones corta (4 páginas) y extendida (8), compiladas. [Fuentes](articulo/) |

Las tablas posteriores conservan sus mediciones históricas.

## Catálogo general

Las dos modalidades comparten nueve categorías principales. El inventario histórico se verificó por huella de las 45 filas en PostgreSQL. El seed añadió 30 referencias y su segunda ejecución insertó cero filas. La revisión SHA-256 comparó las imágenes principales que muestra la interfaz para 75 productos y 21 lotes activos. Encontró cero repetidas. Los archivos de resultados explican el alcance de cada comprobación.

[Datos PostgreSQL](../resultados/remata-datos.json) · [Imágenes](../resultados/remata-imagenes.json) · [Tienda y subastas](capturas/remata-tienda-y-subastas.png)

## Rama subastas

| Requisito | Estado | Evidencia |
| --- | --- | --- |
| Core e-commerce | Cumple | Auth, catálogo, stock, carrito y compra comprobados en Postman. Se añadió el recorrido de puja y adjudicación. |
| Al menos tres microservicios | Cumple | Cuatro procesos locales, con Swagger y health en 9090, 9092, 9093 y 9094. |
| Una base de datos | Cumple | PostgreSQL compartido. [Consulta de lotes y pedidos](../resultados/pgadmin-subastas.txt) |
| Notificaciones | Cumple local | RabbitMQ y Mailpit entregaron puja superada y ganada para el lote 18. [Evidencia](../resultados/subastas-e2e.json) |
| Cinco APIs propias por servicio | Cumple | [APIs existentes y siete rutas de Subastas](../README.md) |
| Cobertura mínima de 80 % | Cumple en suite completa | JaCoCo considera juntas las pruebas unitarias y de integración. Véanse las cifras de la [revisión](../REVISION_FINAL.md). |
| Dos APIs externas | Cumple local | Frankfurter y Nager.Date pasaron la validación previa a k6. La ficha usa el tipo de cambio y la entrega en días hábiles. |
| Despliegue completo | Parcial | Arranque Windows probado. Configuración Vercel, Render, Neon, CloudAMQP y Brevo preparada. Publicación pendiente. |
| Carga y estrés | Cumple | 43 rutas por perfil, con 3, 6 y 8 VU. [Corridas y fases Auth](../REVISION_FINAL.md) |
| Máximo de 500 ms | No cumple | k6 alcanzó 5.616,10 ms. Newman alcanzó 3.087 ms. |
| IEEE en LaTeX | Cumple | [Artículo extendido](../output/pdf/articulo-ieee.pdf) y [edición de máximo 4 páginas](../output/pdf/articulo-ieee-4p.pdf). |

## Evidencia de la entrega estable

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
