# Prompt de traspaso de ECOMMERCE_FRANK / TechStore

Eres la IA que continuará este proyecto académico. Te adjunto el repositorio o el ZIP de entrega. Soy estudiante y debo poder explicar cada cambio en español. Revisa el estado real antes de tocar código. La última revisión documentada es del **22 de noviembre de 2026**. Este prompt distingue los resultados de esa revisión de las tareas que aún requieren mi cuenta.

## Cómo trabajar

- Empieza por `REVISION_FINAL.md` y la sección pertinente de `README.md`. Usa `rg`, `git status` y `git diff` para ubicar lo necesario. Evita releer todo el repo, pegar logs largos o repetir pruebas que no correspondan a la tarea.
- Avanza con cambios pequeños y reversibles. Conserva el inventario, los pedidos históricos, los resultados previos y la demo de Windows. Verifica el cambio por la ruta que usa el usuario y corre las pruebas relevantes.
- Mantén credenciales y tokens en el entorno privado. Usa los archivos de ejemplo del repo. Pídeme datos solo cuando bloqueen una acción que dependa de mi cuenta. No me pidas que comparta contraseñas, tokens ni claves SSH privadas.
- Trabaja con resultados medidos. Distingue una compilación, una validación estática, una prueba local y una prueba en la nube. Reporta cualquier fallo, aunque una repetición pase. Al terminar, indica qué quedó listo, cómo se comprobó y qué falta.

## Alcance y requisitos académicos

El repositorio se llama **ECOMMERCE_FRANK** y la tienda visible se llama **TechStore**. Es una tienda colombiana de computadores, componentes y electrónica nueva y usada, con un solo vendedor y un solo inventario. El cliente explora, busca, filtra, consulta fichas, se registra, inicia sesión, agrega al carrito, confirma un pedido y consulta **Mis compras**. La compra académica registra el pedido en COP. El alcance actual no incluye un marketplace, una pasarela de pagos ni logística real.

El profesor pidió verificar 11 puntos: core e-commerce, mínimo tres microservicios, una base de datos, sistema de notificaciones, cinco APIs con lógica propia por microservicio, cobertura mínima del 80 % por servicio, dos APIs externas, despliegue completo, carga y estrés automatizados, máximo de 500 ms por API y artículo IEEE en LaTeX. La última instrucción solicita **6 a 8 páginas en IEEEtran**. El PDF tiene 6. Si el profesor mantiene el límite inicial de 4, verifica ese requisito antes de preparar otra versión.

## Lo que ya está implementado

- **Arquitectura.** React, Java 17 con Spring Boot, tres servicios y PostgreSQL. Auth maneja cuentas y JWT. Catálogo maneja productos, carrito, inventario y pedidos. Notificaciones consume mensajes de RabbitMQ y entrega correo SMTP. Mailpit es la bandeja de desarrollo. Las cinco APIs no CRUD por servicio están explicadas en el README. Los Swagger también están disponibles.
- **Catálogo.** 45 artículos en 11 categorías de tecnología, con seed idempotente y fichas con imágenes locales, descripción corta y larga, especificaciones, marca, modelo, stock, garantía y grado A/B/C solo en usados. Hay filtros de búsqueda, categoría, condición, marca y precio, orden y paginación del lado del servidor. La tienda usa tema oscuro y cian, con diseño responsive.
- **Inventario.** Catálogo, ficha y carrito muestran las unidades disponibles. Las vistas consultan el stock al volver a la pestaña y periódicamente mientras está visible. Cuando se vende la última unidad, aparece **Agotado** y se bloquea la compra de ese artículo incluso si ya estaba en otro carrito. El backend comprueba y descuenta existencias de forma atómica. Dos confirmaciones del mismo carrito solo crean un pedido. La regresión se comprobó con dos compradores y pruebas de integración.
- **APIs externas que realmente se ven.** Frankfurter muestra una referencia COP → USD debajo del precio en la ficha. Catálogo expone `GET /api/ecommerce/producto/{id}/precio-usd`. Nager.Date aporta festivos colombianos para estimar tres días hábiles en el resumen del carrito. Ruta `GET /api/ecommerce/producto/entrega?diasHabiles=3`. En Java, el adaptador es `ProveedoresTienda.java` y la lógica con caché está en `InformacionTienda.java`. En React, ambos resultados se muestran en `frontend/src/components/InformacionCompra.js`. Frankfurter puede devolver 503 si no hay tasa vigente y Nager.Date indica calendario parcial si faltan festivos. La compra sigue disponible en ambos casos.
- **Seguridad.** JWT obligatorio por variable de entorno, CORS con origen permitido, límite de intentos de login y cambio de contraseña desde el menú de cuenta. La derivación nueva usa Argon2id con 19 MiB, 2 iteraciones y paralelismo 1. Se admite BCrypt heredado. Cambiar una contraseña verifica el hash previo y deriva el nuevo fuera de la transacción de escritura. Mantén esos parámetros al optimizar.
- **Notificaciones.** Registro, compra y cambio de estado generan avisos. RabbitMQ desacopla el envío y tiene reintentos y cola de errores. Mailpit recibe el correo local. El SMS local está en modo simulado y se registra en PostgreSQL. Brevo SMTP y Twilio están preparados por variables. El servicio avisa si faltan credenciales SMTP en modo autenticado.

## Cómo arrancar y dónde mirar

En Windows, la copia usada estaba en `C:\Users\jumas\Downloads\ECOMMERCE (1)\ECOMMERCE`. Si extraes el ZIP en otro lugar, trabaja desde la carpeta que contiene `iniciar.ps1`. Arranca con `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1` y detén con `powershell -ExecutionPolicy Bypass -File .\detener.ps1`. El inicio normal abre la tienda, Mailpit, Swagger y pgAdmin si está instalado. Los logs nativos están en `%LOCALAPPDATA%\EcommerceDev`.

| Componente | Acceso local |
| --- | --- |
| Tienda | `http://localhost:3000` |
| Auth, Catálogo, Notificaciones | 9090, 9092, 9093. Prueba `/health` y `/swagger-ui.html` |
| PostgreSQL | `127.0.0.1:55432`, base `ecommerce`, usuario `ecommerce_dev`. La clave es `POSTGRES_PASSWORD` de `.env.native` |
| RabbitMQ | 5673 AMQP |
| Mailpit | `http://localhost:8025` y puerto SMTP 1025 |

## Resultados que ya existen

- El pedido **#2151** se confirmó desde el navegador y su correo llegó a Mailpit. Se probaron registro, búsqueda y filtros, ficha, carrito, historial, cambio de contraseña, reingreso y cierre de sesión. Se comprobaron 390, 768 y 1440 px, foco del buscador y favicon. Evidencia en `resultados/e2e-techstore.json` y `docs/capturas/`.
- Pasaron **76 pruebas Java** y **12 React**. JaCoCo de líneas de la suite Java completa, incluidas pruebas de integración: **Auth 85,28 %** (23 pruebas), **Catálogo 88,49 %** (32) y **Notificaciones 82,95 %** (21).
- Newman en caliente pasó **48 peticiones y 145 comprobaciones**, máximo **194 ms**. La primera corrida de revisión tuvo dos fallos de tiempo: alta temporal de producto (**1.354 ms**) y validación de correo (**593 ms**). Ambas corridas se conservan en `postman/` y `resultados/`.

Resultados k6 en Windows con **36 rutas**, tiempos en milisegundos:

| Usuarios virtuales | Media | p95 | p99 | Máximo | Errores HTTP | req/s |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | 29,01 | 124,80 | 218,55 | 344,08 | 0 | 61,16 |
| 6 | 23,84 | 114,04 | 211,19 | 284,96 | 0 | 104,42 |
| 8 | 38,78 | 192,45 | 463,72 | 1.086,76 | 0 | 96,00 |

Con 8 usuarios se excedieron 500 ms en cambio de contraseña (máximo 1.086,76 ms, p95 826,43 ms), registro (861,08 ms), renovación (629,82 ms) e inicio de sesión (591,87 ms). El cambio de contraseña pasó de 715,10 ms en la referencia histórica de 6 usuarios a 284,96 ms en la revisión. Son corridas históricas distintas. No afirmes que el límite de 500 ms ya se cumple ni atribuyas todos los máximos al hash sin perfilar. Los JSON y el desglose están en `resultados/latencias-techstore.md`.

## Pendientes prioritarios y acciones que dependen de mí

1. **Seguridad antes de publicar.** Hay constancia de una clave AWS IAM y un token Twilio expuestos en un historial anterior. Debo revocarlos o rotarlos desde mis cuentas. El historial actual se revisó con Gitleaks. Sus coincidencias incluían ejemplos, pruebas y archivos privados ignorados. La revisión de npm redujo alertas de 65 a 29, con cero críticas. Las 29 restantes corresponden a desarrollo/compilación y `npm audit --omit=dev` devolvió 0. Si se moderniza la herramienta de compilación, prueba todo el flujo.
2. **Docker y Render.** En Windows se verificaron los JAR, el build React y la configuración estática de Compose. El motor Docker y WSL no estaban disponibles, así que el build y arranque de contenedores siguen pendientes. Instala Docker Desktop con WSL 2 si vas a probarlo en este equipo. El Blueprint `render.yaml` pasó validación de esquema, pero falta publicar. Yo debo crear el repositorio GitHub, conectar mi cuenta Render y crear el Blueprint. No hay URLs públicas verificadas.
3. **Credenciales de nube.** Yo debo crear RabbitMQ **Little Lemur** en CloudAMQP y cargar la URL AMQPS en `SPRING_RABBITMQ_ADDRESSES`. Debo verificar un remitente en Brevo y definir `MAIL_USERNAME`, `MAIL_PASSWORD` y `MAIL_FROM`. El Blueprint usa `smtp-relay.brevo.com:2525` con STARTTLS porque Render Free bloquea 587. Para SMS real, debo habilitar remitente y destino en Twilio y configurar `SMS_PROVIDER=twilio`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` y `TWILIO_FROM`. Render genera su JWT separado del local. Completa las URLs HTTPS y CORS con los valores reales del panel. La guía detallada está en `docs/Render.md`. Tras publicar, verifica los tres `/health`, una compra, correo Brevo, SMS si aplica y pgAdmin externo con SSL require e IP permitida. Revisa los límites vigentes de Render antes de usar la modalidad Free. En la última verificación, los servicios se suspendían tras 15 minutos de inactividad y PostgreSQL Free vencía a los 30 días.
4. **Latencia.** Si la tarea es optimizar, empieza por medir las fases del cambio de clave y la concurrencia de Auth. Mantén Argon2id. Repite k6 secuencialmente con 3, 6 y 8 usuarios, con servicios calientes y sin compilaciones simultáneas. Conserva las corridas anteriores y reporta promedio, p95, p99, máximo, errores y req/s por perfil y ruta.

## Archivos para orientarte

- `REVISION_FINAL.md`, `docs/Auditoria.md` y `README.md` explican el estado y el checklist del profesor.
- `Instrucciones de arranque.md`, `docs/Render.md` y `docs/Sustentacion.md` son las guías operativas y de exposición.
- `postman/` contiene colección completa, entornos Local y Render sin secretos, ejecutor Newman e informes HTML.
- `docs/pgadmin/` contiene `servers.json`, consultas de verificación y el diagrama ER generado desde el esquema real. El resultado local está en `resultados/pgadmin-local.txt`.
- `pruebas/techstore-e2e.cjs` es el recorrido de navegador. `pruebas/ejecutar-k6.ps1` ejecuta los perfiles. `resultados/` conserva las mediciones.
- `docs/articulo/articulo-ieee.tex` es la fuente del artículo de dos columnas. Los PDF están en `output/pdf/` y el paquete de entrega está en `output/TechStore-entrega.zip`.

Confirma el estado del repositorio y toma el primer pendiente autorizado por mi siguiente mensaje. Entrega un resumen corto en español con resultados, evidencia y pasos precisos para lo que dependa de mí.
