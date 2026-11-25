# Revisión final de Remata

## Remata · Compra directa y subastas

El símbolo, los iconos PNG y la imagen social se integraron desde el kit adjunto. La marca y los colores de la aplicación se editan en `frontend/src/marca.js`. La navegación, los filtros y las fichas usan nueve categorías compartidas. Tienda vende a precio fijo y Subastas adjudica con pago simulado. Los vehículos completos se publican como lotes.

La revisión de PostgreSQL contó 75 productos. La huella de las 45 fichas históricas coincide antes y después del despliegue. Los 30 SKU generales insertan una sola vez. Hay 21 lotes activos que representan los nueve grupos. Se eliminaron las pujas, adjudicaciones y lotes que coincidían con los prefijos automáticos de las pruebas anteriores. Las 96 imágenes principales de artículos visibles arrojaron cero huellas SHA-256 repetidas. Las 26 variantes para fichas históricas se resuelven en la interfaz. [Base de datos](resultados/remata-datos.json) · [Imágenes](resultados/remata-imagenes.json)

Pasaron 90 pruebas Java. Cobertura de líneas de JaCoCo: Auth 82,44 %, Catálogo 86,97 %, Notificaciones 81,76 % y Subastas 89,00 %. [Medición](resultados/remata-cobertura.json). El primer intento de Notificaciones alcanzó aproximadamente 77 %. Las pruebas del historial paginado y del JWT elevaron la cobertura. La suite de verificación incluye pruebas de integración. Un análisis de cobertura exclusivamente unitaria requiere una medición adicional.

La compra sintética 2550 registró stock de 1 a 0 y entregó el correo de confirmación en Mailpit. El lote 162 comprobó dos postores, puja superada, adjudicación y pago simulado. Newman hizo 86 peticiones y 256 aserciones, con dos incumplimientos de tiempo y máximo de 846 ms. [Compra](resultados/remata-compra-e2e.json) · [Subasta](resultados/remata-puja.json) · [Newman](resultados/remata-postman.json)

Los identificadores históricos de archivo y los resultados del apartado anterior a Remata siguen disponibles para comparar. El evento que traslada lotes desiertos a la tienda queda como trabajo futuro.

## Rama subastas, 24 de noviembre de 2026

- La etiqueta `version-estable` conserva la entrega del 22 de noviembre. El cuarto servicio se integra en la rama `subastas`, con JWT de Auth y PostgreSQL compartido.
- Maven verificó 86 pruebas Java y React ejecutó 12, con cero fallos. JaCoCo de líneas en la suite conjunta: Auth 84,00 %, Catálogo 88,39 %, Notificaciones 82,95 % y Subastas 98,97 %. La primera ejecución con `clean` no pudo borrar los JAR abiertos en Windows. Al detener solo los procesos Java y conservar PostgreSQL, el comando pasó para los cuatro servicios. [Cobertura](resultados/cobertura-subastas.json)
- La auditoría npm de dependencias de ejecución reportó cero alertas. El análisis local OSV de manifiestos encontró avisos para Bouncy Castle 1.83 en Auth. La revisión previa del grafo transitivo encontró otros avisos. Se mantienen las versiones probadas a la espera de verificar actualizaciones compatibles. [OSV](resultados/osv-java-subastas.json)
- Gitleaks verificó el historial y el árbol de entrega sin alertas pendientes. En el primer análisis del árbol señaló ocho valores de ejemplo y prueba. La configuración exceptúa solo los valores ficticios conocidos en sus archivos concretos. El ejemplo de token de la documentación ahora usa un marcador. La revocación de credenciales históricas se gestiona en las cuentas de cada proveedor.
- El lote 18 verificó dos pujas, el aviso a quien perdió el liderazgo, la extensión del plazo, la adjudicación y el pago simulado. El recorrido adelantó la fecha de cierre del lote de prueba para comprobar el proceso programado. [JSON](resultados/subastas-e2e.json) · [Capturas](docs/capturas)
- La compra 2548 confirmó stock de 1 a 0 en un producto de prueba de una sola unidad, mostró el pedido en el historial y recibió el correo en Mailpit. El navegador también verificó el cambio de contraseña y la ruta protegida después de cerrar sesión. [Evidencia](resultados/e2e-compra-subastas.json)
- La consulta real de PostgreSQL confirmó 16 lotes de muestra, dos por categoría. Ejecutar el seed de nuevo devolvió `INSERT 0 0`. [Consulta](resultados/pgadmin-subastas.txt)
- Newman ejecutó 80 peticiones y 238 aserciones. Hubo cero errores de petición y nueve fallos de tiempo. Máximo 3.087 ms. [JSON](resultados/postman-subastas.json) · [HTML](postman/reporte-subastas.html)
- Axe verificó 21 reglas sin infracciones detectadas en el listado a 390, 768 y 1440 px. El documento midió el mismo ancho que la ventana en los tres casos. La auditoría automática cubre el listado.

| VU | Media ms | p95 ms | p99 ms | Máximo ms | Errores | req/s |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | 98,82 | 309,94 | 873,52 | 5.616,10 | 0 | 15,07 |
| 6 | 49,45 | 241,46 | 469,38 | 1.064,97 | 0 | 61,03 |
| 8 | 58,21 | 275,16 | 739,62 | 1.701,67 | 0 | 69,22 |

Las tres corridas ejercitaron 43 rutas. Subastas registró máximos de 509,93, 221,31 y 196,47 ms. A 3 VU también superó 500 ms `GET /mias` con 502,68 ms. A 8 VU, el máximo fue el cambio de contraseña. La media muestral por hash fue 269,47 ms y su espera 30,73 ms. La consulta promedió 2,51 ms y la escritura 21,45 ms por cambio. Esas medias describen fases agrupadas. La medición no asigna los picos de Catálogo y Notificaciones a una sola causa. [3 VU](resultados/subastas-k6-3.json) · [6 VU](resultados/subastas-k6-6.json) · [8 VU](resultados/subastas-k6-8.json) · [Fases Auth](resultados/subastas-k6-fases-8.json)

**Pendientes.** Publicación desde las cuentas del propietario y prueba de Brevo. Docker Compose validó la configuración. Al construir, el cliente informó que el motor Docker Desktop Linux no estaba disponible. Quedan la latencia y una auditoría de accesibilidad del resto de pantallas. El catálogo conserva sus filtros existentes. El rediseño nuevo se concentra en inicio, cabecera y subastas. [Publicación y variables](docs/Render.md)

**Entrega.** El artículo IEEE extendido tiene 7 páginas y la edición corta 2 páginas. Ambos se compilaron y no registraron líneas desbordadas. [Artículo](output/pdf/articulo-ieee.pdf) · [Edición corta](output/pdf/articulo-ieee-4p.pdf) · [Guía](output/pdf/guia-ecommerce-frank.pdf)

**Acciones del propietario.** Revoca la clave AWS IAM histórica, rota el token Twilio y rota cualquier clave Brevo que hayas compartido. Publica el repositorio, crea Neon, CloudAMQP y Brevo, conecta Render y Vercel, configura CORS y prueba `smoke-nube` y pgAdmin con las URLs definitivas.

## Entrega estable, 22 de noviembre de 2026

Verificación del 22 de noviembre de 2026 en Windows. Entorno local iniciado con `iniciar.ps1`.

| Verificación | Resultado | Evidencia | Corrección |
| --- | --- | --- | --- |
| Arranque y servicios locales | Cumple | Puertos 3000, 8025, 9090, 9092 y 9093 respondieron HTTP 200. Notificaciones comprueba PostgreSQL y RabbitMQ en `/health`. [Prueba técnica](resultados/revision-tecnica.json) | Health unificado y tiempos de conexión configurados. |
| Pruebas Java y React | Cumple | 76 Java, incluidas 21 de Notificaciones. 12 React. [JaCoCo](resultados/cobertura-techstore.json) | Protección de login con caso de prueba para HTTP 429. |
| Cobertura mínima | Cumple | Auth 85,28 %, Catálogo 88,49 %, Notificaciones 82,95 %, por líneas de la suite Java completa. | Maven verifica el umbral de cada módulo. |
| Dependencias | Parcial | npm audit pasó de 65 alertas, 3 críticas, a 29, 0 críticas. `npm audit --omit=dev` devolvió 0. [Conteos](resultados/revision-tecnica.json) | Router 7.18.6, actualización de transitivas y Spring Boot 3.5.16. Las 29 restantes están en herramientas de desarrollo y compilación. |
| Navegación y compra | Cumple | Pedido 2151 de Portátil ThinkPad T480, guardado en el historial. [Recorrido](resultados/e2e-techstore.json) | Cambio de contraseña visible en el menú, nueva autenticación y protección tras cerrar sesión. |
| Catálogo de tecnología | Cumple | 45 fichas con fotos locales verificadas por HTTP y metadatos completos. Segunda carga: INSERT 0 0. [SQL](resultados/pgadmin-local.txt) | Se revisaron los productos activos y los rótulos de la tienda. |
| Responsive, foco y títulos | Cumple | 390, 768 y 1440 px sin scroll horizontal en catálogo. Favicon disponible, título de ficha y foco visibles. Contraste del buscador 17.29:1. | Se añadieron títulos de página y se verificó diseño. |
| APIs externas | Cumple | Frankfurter mostró USD en la ficha y Nager.Date calculó la entrega. El arranque también mostró aviso de calendario parcial antes de recuperar Nager.Date. | Se conservó el fallback sin bloquear el carrito. |
| RabbitMQ y correo local | Cumple | Mailpit recibió el correo del pedido 2151. [Captura](docs/capturas/techstore-correo.png) | Se conservó el reintento del consumidor y la cola de errores. |
| Brevo y Twilio reales | Parcial | SMTP por variables, aviso al faltar credenciales y SMS simulado local. La entrega a un operador o remitente real requiere cuentas del propietario. | Blueprint usa Brevo 2525 con STARTTLS en Render Free. |
| Secretos y Git | Parcial | Gitleaks encontró coincidencias en ejemplos, pruebas y archivos privados ignorados. El `CHANGELOG.md` documenta una clave AWS IAM y un token Twilio expuestos en el histórico anterior. | Se quitó el valor JWT de respaldo. Revocar o rotar las credenciales históricas en sus cuentas sigue pendiente de confirmación. |
| Autenticación, CORS y cabeceras | Cumple local | Preflight del frontend HTTP 200 y origen ajeno HTTP 403. Verificación de header `nosniff` y de login bloqueado. | JWT obligatorio por variable, CORS exacto y límite de 10 fallos por cuenta o 60 por origen en ventanas de un minuto, por instancia. |
| Render y arranque en frío | Parcial | Blueprint contrastado con el esquema oficial. HTTPS y CORS por entorno, PostgreSQL interno, AMQPS de CloudAMQP y /health. | Java respeta PORT. Se configuró tolerancia de arranque a PostgreSQL y al consumidor RabbitMQ. Falta verificar en la cuenta Render. |
| Build de Docker | Parcial | `compose config --quiet` válido. `compose build` no se conectó al motor Docker de Windows. El frontend y los tres JAR sí compilaron. | Dockerfiles con usuario no root, puerto variable y health. Falta ejecutar los contenedores. |
| Postman y Newman | Cumple | 48 peticiones, 145 aserciones, 0 fallos, máximo 194,00 ms. [HTML caliente](postman/reporte-local.html). La primera corrida tuvo 2 fallos de tiempo y máximo 1.354,00 ms. [HTML inicial](postman/reporte-revision-inicial.html) | Se agregaron los tres readiness checks y se revalidó la colección en local. |
| pgAdmin y diagrama real | Cumple local | [Consulta ejecutada](resultados/pgadmin-local.txt), [conexiones](docs/pgadmin/servers.json) y [diagrama ER](docs/pgadmin/er-techstore.png). | Metadatos y claves foráneas consultados en PostgreSQL. |
| k6 carga y estrés | Cumple | 36 rutas por perfil a 3, 6 y 8 usuarios. [Detalle por ruta](resultados/latencias-techstore.md) | Se conservaron registros históricos y se midió la revisión. |
| Límite de 500 ms | No cumple | Véase la tabla y las excepciones. | Argon2id mantiene 19 MiB, dos iteraciones y paralelismo uno. La escritura del hash es condicional. |
| Documentos | Cumple | [Artículo IEEE](output/pdf/articulo-ieee.pdf), [guía PDF](output/pdf/guia-ecommerce-frank.pdf) y [auditoría](docs/Auditoria.md). | Actualizados con los resultados del recorrido y las mediciones locales. |

## Rendimiento

| Usuarios | Media ms | p95 ms | p99 ms | Máximo ms | Errores HTTP | req/s |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | 29,01 | 124,80 | 218,55 | 344,08 | 0 | 61,16 |
| 6 | 23,84 | 114,04 | 211,19 | 284,96 | 0 | 104,42 |
| 8 | 38,78 | 192,45 | 463,72 | 1.086,76 | 0 | 96,00 |

- **3 usuarios.** Máximos mayores a 500 ms: ninguna. Cambio de clave: p95 247,55 ms y máximo 257,77 ms.
- **6 usuarios.** Máximos mayores a 500 ms: ninguna. Cambio de clave: p95 244,85 ms y máximo 284,96 ms.
- **8 usuarios.** Máximos mayores a 500 ms: `auth_cambiar` (1.086,76 ms), `auth_registrar` (861,08 ms), `auth_renovar` (629,82 ms), `auth_login` (591,87 ms). Cambio de clave: p95 826,43 ms y máximo 1.086,76 ms.

Cambiar contraseña exige verificar el hash vigente y calcular el nuevo. Cada perfil mide la duración HTTP completa y el máximo depende de la concurrencia y del equipo. Las mediciones no descomponen tiempo de CPU, JVM y base de datos. El primer request de Render Free tras la suspensión puede tardar cerca de un minuto según su documentación. [Render Free](https://render.com/docs/free)

## Pasos del propietario

1. Revoca la clave AWS IAM histórica o rota su par si aún se usa. Rota el token Auth de Twilio expuesto y actualiza las instalaciones que lo consuman. Guarda las credenciales nuevas en el proveedor o en archivos privados.
2. Crea el repositorio de GitHub, comprueba los archivos que vas a subir y publica el código. Se recomienda privado mientras compruebas la configuración.
3. Crea la cuenta Render, conecta GitHub desde **New → Blueprint** y selecciona `render.yaml`.
4. Crea la instancia RabbitMQ Little Lemur en CloudAMQP y pega la URL `amqps://` en `SPRING_RABBITMQ_ADDRESSES` de los servicios Java.
5. Verifica el remitente Brevo y configura `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM`. Conserva 2525 con STARTTLS en Free. Para SMS reales, agrega `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` y `TWILIO_FROM`, cambia `SMS_PROVIDER=twilio` y verifica un destino permitido.
6. Completa URLs HTTPS y CORS, despliega y verifica `/health`, compra, correo, SMS y conexión externa a PostgreSQL mediante pgAdmin con SSL. [Guía detallada](docs/Render.md)
