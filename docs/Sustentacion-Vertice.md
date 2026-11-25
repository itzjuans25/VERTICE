# Sustentar VÉRTICE

## Guion

1. Abre la portada y compara el número de lotes en una casilla con los resultados de su filtro. Enseña las dos vistas de un lote, **Por qué se subasta**, su ficha y el historial anonimizado.
2. En dos sesiones, revisa y confirma una oferta. Explica el precio mínimo, el bloqueo de fila y la extensión de dos minutos. Abre el correo de Mailpit y muestra la adjudicación en **Actividad → Subastas ganadas**. Repetir el pago conserva su referencia.
3. Abre **Lanzamientos**. Muestra cupo, fecha y máximo por cliente. PostgreSQL descuenta stock al confirmar el pedido. Paga desde **Actividad → Compras** y abre Mailpit para enseñar el comprobante.
4. Abre el MER. Explica los cuatro contextos, RabbitMQ y las comprobaciones de rendimiento. Distingue las medidas localmente ejecutadas de las pendientes.

## IntelliJ IDEA

1. Abre **File → Open**, selecciona el `pom.xml` de la raíz y elige **Open as Project**. Elige JDK 17 como Project SDK.
2. Recarga Maven desde su panel. Debe listar Auth, Catálogo, Notificaciones y Subastas. Para compilar puedes abrir el lifecycle de Maven y ejecutar `compile`.
   La compilación del POM raíz pasó con el Maven incluido en IntelliJ. [Salida del reactor](../resultados/vertice-galeria-intellij.txt)
3. Desde la terminal del proyecto ejecuta `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1`. El script maneja PostgreSQL, RabbitMQ, Mailpit, React y los cuatro JAR. Evita arrancar una segunda instancia en el mismo puerto.
4. Para depurar un servicio individual, mantenlo fuera del arranque y configura el entorno local de esa aplicación en IntelliJ. Usa `.env.example` como referencia. Las credenciales privadas viven fuera de Git.

## pgAdmin y PostgreSQL

Conecta a `127.0.0.1:55432`, base `ecommerce` y usuario habitual `ecommerce_dev`. Consulta la contraseña en tu `.env.native` sin compartirla. Abre Query Tool, ejecuta las consultas y comprueba tablas, índices, lotes y pujas. [Modelo e inspección](pgadmin/vertice-mer.md)

## Postman

Importa la colección y el entorno Local. Ejecuta registro e inicio de sesión y conserva el JWT del cliente. Usa ese token para carrito, pedidos, pujas, Avisos y perfil. Repite `POST /api/ecommerce/compras/{id}/pago` con el propietario y compara fecha y referencia. Lista piezas con `GET /api/ecommerce/subastas?curados=true` y usa un ID real para detalles. Prueba USD y entrega en ambos canales y el clima de Bogotá en `GET /api/subastas/clima`. Revisa los correos locales en Mailpit. [Colección](../postman/TechStore.postman_collection.json)

## Publicación

Sube el repositorio a GitHub sin datos privados. Crea Neon y toma la URL JDBC directa con SSL. Crea CloudAMQP y copia el URI AMQPS como secreto. Configura un remitente verificado en Brevo. Importa el Blueprint de Render y configura JWT, datos y servicios. En Vercel usa `frontend` como raíz, añade las URLs públicas obtenidas y configura CORS en Render. Ejecuta `scripts/smoke-nube.ps1` con una cuenta y fixtures de demostración. [Guía detallada](Render.md)
## Evidencia que puedes mostrar

- Compra 3356 por COP 95.000. Se descontó la última unidad y se confirmó `SIM-COMPRA-3356` desde la cuenta. Repetir el pago conservó el instante.
- Dos solicitudes de pago concurrentes devolvieron el mismo comprobante. El sistema rechazó el exceso de cupo antes de descontar inventario. [Prueba del lanzamiento](../resultados/vertice-curado-e2e.json)
- Lote 1411, Cronógrafo de archivo · Serie Boreal. Dos postores probaron la extensión. Mailpit recibió «Puja superada», «Subasta ganada» y el comprobante de `SIM-SUBASTA-48`. [Prueba de subasta](../resultados/vertice-curado-puja.json)
- Abre las respuestas guardadas de USD, festivos y clima, y separa cobertura exclusivamente unitaria de la suite conjunta.
- Usa los [resultados de rendimiento](../resultados/vertice-curado-rendimiento.md) para explicar los picos medidos.

Para la presentación, usa las [capturas de la galería curada](capturas/) cuyo nombre empieza por `vertice-curado-`. Las capturas numeradas de PostgreSQL, RabbitMQ, SMS, JaCoCo y k6 corresponden a corridas previas. Para citar cifras, usa la primera tabla de [Auditoría](Auditoria.md).

## Preguntas típicas

**¿Cómo se evita vender la última unidad dos veces?** PostgreSQL comprueba y descuenta el stock en la transacción de compra.

**¿Qué hace RabbitMQ?** Desacopla los emisores de los avisos. El servicio de Notificaciones consume los mensajes y envía el correo a Mailpit en el laboratorio.

**¿Cómo evita una puja al último segundo?** En los últimos dos minutos, la oferta aceptada extiende el plazo dos minutos. El historial público oculta los IDs de los compradores.

**¿De qué depende el cambio de contraseña?** Requiere verificar el hash anterior y derivar el nuevo. El informe muestra las fases medidas por Micrometer a 8 VU. Argon2id conserva sus parámetros de seguridad.

**¿Qué necesito para publicar?** Repositorio propio, base persistente, broker, SMTP verificado y alojamiento de backend/frontend. Sigue las variables de la guía de publicación y prueba cuando tengas las URLs.
