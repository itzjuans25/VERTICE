# Revisión final de TechStore

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
