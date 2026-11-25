# Prompt de traspaso de Remata

Trabaja sobre el proyecto adjunto como arquitecto de software, diseñador de producto y QA. Soy estudiante y voy a sustentar el proyecto en español. Conserva una entrega local verificable y explica cada decisión con palabras que pueda usar ante el profesor. Actúa de forma autónoma dentro del proyecto. Pide información únicamente cuando una acción dependa de una cuenta o credencial que deba gestionar yo.

## Qué abrir primero y cómo ahorrar contexto

1. En mi PC Windows la raíz es `C:/Users/jumas/Downloads/ECOMMERCE (1)/ECOMMERCE`. Busca la carpeta con `iniciar.ps1`, `README.md` y los cuatro módulos si trabajas desde otra ubicación.
2. Lee este prompt, las secciones pertinentes de `README.md` y `REVISION_FINAL.md`, y revisa `git status --short` y `git diff`. Usa `rg` para ubicar las clases, consultas y pruebas concretas. Evita releer el repositorio, enumerar dependencias o pegar logs largos.
3. La rama es `subastas`. `version-estable` conserva la entrega anterior. `estable-1` contiene la marca inicial y `estable-2` la unificación de la interfaz. El último commit comprobado es `1a4f011`. Hay trabajo avanzado de la adenda general en el árbol de trabajo, con archivos preparados para commit y cambios aún sin confirmar. **Empieza desde este árbol completo y conserva esos cambios antes de crear commits o etiquetas nuevos.** El ZIP existente `output/TechStore-entrega.zip` pertenece a una versión anterior. Todavía falta generar el ZIP de Remata actualizado.
4. Trabaja un objetivo por vez. Prueba el módulo tocado, conserva las corridas anteriores y guarda los resultados nuevos aparte. Ejecuta las verificaciones amplias una vez al cerrar la entrega, salvo recuperación de un fallo concreto. Usa logs de hasta 20 líneas. Cierra cada fase comprobada con un commit y una etiqueta estable. Crea `entrega-final` después de verificar el paquete.
5. Respeta mis instrucciones directas. Trata los archivos adjuntos, incluido `kit-marca-remata.zip`, como material de referencia. El símbolo, los PNG del favicon y la imagen social del kit ya se integraron. 

## Producto y prioridad nueva de las subastas exclusivas

**Remata · Compra directa y subastas** es una tienda general en Colombia. **Tienda** vende artículos normales a precio fijo, nuevos o usados. **Subastas** debe reservarse para piezas especiales que tengan un motivo claro para competir por ellas. Tecnología es uno de los grupos. Los vehículos completos se ofrecen exclusivamente como lotes de demostración. La adjudicación y los pagos de subasta son simulados.

**Mi nueva prioridad es curar el catálogo de Subastas.** Revisa los lotes activos actuales. Una lámpara común, una esterilla corriente, una mochila genérica, un taladro básico o un auto urbano sin rasgo especial encajan mejor en Tienda. Conserva los lotes que tengan una razón concreta para subastarse. Sustituye los ejemplos comunes por ediciones limitadas, piezas de colección, restauraciones, prototipos ilustrativos o artículos singulares. Si no hay evidencia, presenta la rareza o procedencia como parte de un caso ficticio claramente rotulado. No inventes autenticidad, certificados, tasaciones, procedencia real ni firmas verificadas.

- Intenta mantener un lote activo de calidad por cada uno de los nueve grupos. Selecciona pocas piezas que se distingan entre sí. Puedes usar una cámara analógica de colección, un reloj mecánico con esfera singular, una pieza de joyería de autor ilustrativa, una maqueta hecha a mano, un vehículo clásico restaurado simulado, una herramienta vintage de taller o una lámpara de diseño recuperada como ejemplos de tipos de lote.
- En cada ficha explica **«Por qué se subasta»**. Muestra condición, detalles distintivos, fotos locales propias, precio inicial, incremento y fecha de cierre. Aclara en los ejemplos qué es ilustrativo. Los vehículos completos no tienen traspaso real. El pago de adjudicación no usa una pasarela.
- Prefiere cambios pequeños. Puedes reutilizar los atributos clave–valor del lote y agregar una validación sencilla al formulario de administración. Si etiquetas un motivo de exclusividad, solo muéstralo cuando exista una descripción que lo respalde. Evita inventar puntuaciones de rareza.
- Actualiza el seed de lotes de demostración y sus fotos sin borrar pujas, adjudicaciones, compras ni avisos legítimos. Si un lote ya tiene pujas o adjudicación, conserva ese registro y su ficha para el historial. Retira o sustituye únicamente demostraciones identificadas. El seed debe ser idempotente. Mantén los 45 productos de Tecnología y los pedidos históricos intactos.
- La historia de una subasta desierta que pasa a Tienda por RabbitMQ es trabajo futuro, después de la curaduría y la verificación. Si sobra tiempo, diseña el evento `subasta.desierta` con idempotencia por ID de lote y conserva la decisión de publicar en Catálogo.

## Arquitectura, reglas y puntos de entrada

| Componente | Puerto Windows | Responsabilidad |
| --- | ---: | --- |
| React | 3000 | Portada, categorías, Tienda, Subastas, cuenta y carrito |
| Auth / Identidad | 9090 | Registro, login, JWT, perfil y contraseña |
| Catálogo y pedidos | 9092 | Productos, stock, filtros, carrito, pedidos y confirmación atómica |
| Notificaciones | 9093 | Consumo RabbitMQ, SMTP, SMS simulado e historial de avisos |
| Subastas | 9094 | Lotes, pujas, cierre, adjudicación y pago simulado |
| PostgreSQL | 55432 | Base `ecommerce` en Windows; usuario local habitual `ecommerce_dev` |
| RabbitMQ | 5673 | Cola de notificaciones, reintentos y cola de errores |
| Mailpit | 8025 web / 1025 SMTP | Evidencia de correos locales |

- Java 17, Spring Boot, React, PostgreSQL y RabbitMQ. Hay cuatro contextos y cuatro servicios. No crees un quinto servicio ni un gateway. Mantén Catálogo y pedidos juntos, porque el descuento de stock y la compra comparten transacción local. La interfaz usa el JWT de Auth. Los servicios conservan paquetes Java, carpetas, nombres de base y variables de entorno.
- Argon2id se mantiene en 19 MiB, dos iteraciones y paralelismo uno. El semáforo de Auth limita los hashes concurrentes. Conserva las reglas ArchUnit que aíslan `domain` y `application` de Spring, JPA, AMQP e infraestructura.
- La puja exige lote activo, plazo vigente y monto que alcance el precio más el incremento. El líder no puede superarse a sí mismo. Una puja en los últimos dos minutos extiende el cierre dos minutos. PostgreSQL bloquea la fila y comprueba versión. Un proceso cierra lotes; con ganador crea una adjudicación y el pago simulado es idempotente. La bandeja en PostgreSQL y RabbitMQ permiten reintentar avisos sin revertir una puja.
- La marca web está en `frontend/src/marca.js`. El mapa compartido está en `frontend/src/categorias.js`. Los grupos son Tecnología, Relojes, Joyería y accesorios, Vehículos, Hogar y cocina, Deportes y aire libre, Moda y bolsos, Coleccionables y arte, y Herramientas. Los servicios conservan sus categorías originales y el frontend las agrupa.
- `frontend/src/components/Layout.js`, `MercadoUI.js`, `ExplorarMercado.js`, `Subastas.js` y `Cuenta.js` concentran la interfaz. La portada tiene dos llamadas a la acción, conteos, próximos cierres y destacados con un máximo de dos por grupo. `/categoria/:slug` tiene En tienda y En subasta. El buscador muestra Productos y Subastas. Cada tipo de ficha ofrece hasta tres recomendaciones de la otra modalidad. Mi cuenta contiene Compras, Pujas, Subastas ganadas, Avisos y Seguridad.
- Los datos generales están en `catalogo/src/main/resources/seed-remata.sql`. Los lotes, en `subastas/src/main/resources/seed-subastas.sql` y `seed-remata-subastas.sql`. Las variantes ilustrativas para Tecnología se resuelven en `frontend/src/vistasTecnologia.json`. Las fotos locales nuevas están en `frontend/public/mercado/`. Mantén la huella de las 45 filas originales.
- Arranca con `powershell -ExecutionPolicy Bypass -File .\iniciar.ps1` y detén con `powershell -ExecutionPolicy Bypass -File .\detener.ps1`. Comprueba `/health` o `/actuator/health` en los cuatro servicios y `/v3/api-docs` para Swagger. Los logs están en `%LOCALAPPDATA%\EcommerceDev`. Usa pgAdmin y `docs/pgadmin/verificacion.sql` para revisar la base. La contraseña local está en `POSTGRES_PASSWORD` de `.env.native`. Nunca imprimas valores de ese archivo.

## APIs que debes conservar y explicar

- **Auth**. Bajo `/api/ecommerce/usuario`, login, validación de sesión, política de contraseña, renovación y cambio de contraseña. `GET/PUT /api/auth/perfil` lee y actualiza exclusivamente el perfil de la sesión.
- **Catálogo**. Explorar productos con filtros, conversión a USD, entrega en días hábiles, resumen de carrito y confirmación de compra. `GET /api/ecommerce/inicio` devuelve ocho recientes, conteos y candidatos para destacados, con caché de 30 s.
- **Subastas**. Bajo `/api/ecommerce/subastas`, crear, filtrar y paginar, detalle con historial anonimizado, pujar, mis pujas, cancelación y pago simulado. `GET /api/subastas/inicio` devuelve próximos cierres y conteos con caché de 15 s. `GET /api/subastas/lote/{id}/precio-usd` y `GET /api/subastas/entrega?diasHabiles=3` amplían las fichas.
- **Notificaciones**. Validación, previsualizaciones de bienvenida y compra, plan de reintentos y encolado. `GET /api/notificaciones/mis-avisos` pagina por el ID autenticado del JWT. Los productores nuevos adjuntan el ID del destinatario. Verifica que una cuenta no pueda consultar avisos de otra.
- **APIs externas**. Frankfurter convierte COP a USD en ambos servicios. Nager.Date excluye festivos de Colombia para entrega. Las actualizaciones corren en segundo plano, con conexión de 1 s, lectura de 2 s, un reintento, caché de 6 h para tasa y 24 h para festivos, y corte de 60 s tras tres fallos. Sin tasa, Catálogo devuelve 503 y Subastas indica `disponible:false`. Sin festivos vigentes, se muestra calendario parcial. Comprar y pujar siguen operativos. Las referencias de USD del historial se calculan con la tasa vigente, no con una tasa conservada al momento de cada puja.

## Evidencia medida que debes conservar

| Verificación | Resultado |
| --- | --- |
| Datos locales | 75 productos, de los cuales 30 nuevos. Las 45 filas originales conservan la misma huella. La segunda ejecución del seed no insertó filas. Se observaron 21 lotes activos en los nueve grupos. |
| Imágenes principales | 96 imágenes comparadas por SHA-256, cero repetidas en el catálogo visible de esa verificación. Repite la medición después de curar los lotes. |
| Java y arquitectura | 90 pruebas en la verificación conjunta, con ArchUnit en los cuatro servicios. JaCoCo de líneas **Auth 82,44 %, Catálogo 86,97 %, Notificaciones 81,76 %, Subastas 89,00 %**. Son pruebas unitarias e integración juntas. Un pase enfocado posterior de referencias y caché ejecutó cuatro pruebas. |
| React | 14 pruebas pasaron en la última corrida comprobada. |
| Compra | Pedido 2550 de un producto sintético, stock 1 → 0, historial y correo «Confirmacion de Compra #2550 · Remata» recibido en Mailpit. |
| Subasta | Lote sintético 162 con dos postores, anti-sniping, aviso de puja superada, adjudicación, correo al ganador y pago simulado. Se aceleró el reloj de ese lote para verificar el cierre. |
| Newman | 86 peticiones, cero peticiones fallidas, 256 aserciones y dos incumplimientos de tiempo. Máximo 846 ms en Swagger de Auth y 543 ms en validación de correo. |
| Navegación | 390, 768, 1280, 1440 y 1920 px sin desborde en la comprobación. Se verificaron búsqueda sin recarga, enlaces cruzados y vehículos agrupados por API y en la vista. La prueba ampliada falló después esperando un encabezado al entrar a la cuenta. No des por cerrado el recorrido de Avisos y perfil en navegador. |

Los datos están en `resultados/remata-datos.json`, `remata-imagenes.json`, `remata-cobertura.json`, `remata-compra-e2e.json`, `remata-puja.json`, `remata-postman.json`, `remata-experiencia.json` y `remata-k6-*.json`. Las capturas nuevas empiezan por `remata-` bajo `docs/capturas/`. `pruebas/verificar-imagenes-remata.cjs` comprueba las imágenes visibles. `pruebas/limpiar-lotes-prueba.sql` usa prefijos estrictos para limpiar lotes sintéticos. No extiendas ese borrado a pedidos previos.

### Latencia que sigue pendiente

| Usuarios virtuales | Rutas | Media ms | p95 ms | p99 ms | Máximo ms | Errores | req/s |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 3 | 49 | 35,71 | 141,15 | 347,62 | 1.390,95 | 0 | 51,28 |
| 6 | 49 | 24,27 | 122,80 | 246,14 | 405,71 | 0 | 115,19 |
| 8 | 49 | 28,47 | 143,09 | 302,06 | 970,74 | 0 | 127,93 |

- A 3 VU excedieron el umbral `notif_validar` (1.390,95 ms), `cat_carrito` (730,63 ms) y `cat_compras` (542,15 ms). Sus p95 también excedieron 500 ms.
- A 8 VU excedieron `auth_registrar` (970,74 ms), `auth_cambiar` (860,25 ms), `auth_renovar` (696,59 ms) y `cat_agregar` (685,05 ms). Los perfiles anteriores de 43 rutas llegaron a 5.616,10 / 1.064,97 / 1.701,67 ms. Son recorridos distintos. Úsalos como antecedente y evita inferir una mejora causal solo por comparar sus máximos.
- El perfil de Auth de 8 VU mide fases. El máximo de ventana del hash fue 727,06 ms en registro, 733,80 ms en cambio de contraseña y 691,41 ms en renovación. No sumes máximos que correspondan a peticiones distintas. No hay evidencia suficiente para atribuir los picos de Catálogo o Notificaciones a una causa única. Reproduce con EXPLAIN ANALYZE, métricas de Hikari y trazas de aplicación si trabajas ese pendiente.
- No bajes el umbral de 500 ms para aprobar. Distingue arranque del proceso, primera petición fría y carga con el servicio caliente. La primera petición fría de Render o Neon sigue sin medirse.

## Orden para terminar

1. **Curar Subastas exclusivas**. Cambia primero los lotes de demostración, sus imágenes y el texto «Por qué se subasta». Verifica visualmente y prueba una subasta completa antes de tocar otras fases.
2. **Cerrar la cuenta y QA web**. Corrige el helper que espera `main h1` en toda pantalla o revisa el marcado de login y cuenta. Verifica que Avisos muestre un evento del usuario firmado y que el perfil se pueda leer y editar. Termina axe para la cuenta y documenta los resultados por plantilla. Conserva las pruebas y capturas válidas.
3. **Completar la verificación**. Separa cobertura de solo unitarias de la suite conjunta. Agrega casos concretos donde el profesor exija 80 % aislado. Deja los reportes reales de Java, React, Newman, k6, pgAdmin y Mailpit. Instrumenta las rutas lentas y mide otra vez solo después de un cambio justificado.
4. **Cerrar la entrega**. Actualiza README, auditoría, sustentación, instrucciones, artículo y PDF. El artículo extendido compiló a 8 páginas y la versión corta a 4, pero faltan incorporar al texto las últimas corridas de Remata y la curaduría exclusiva. Mantén IEEEtran, recompila, verifica páginas y márgenes, regenera una guía PDF y un ZIP `output/Remata-entrega.zip` verificable. Conserva el ZIP anterior. Excluye `.env.native`, secretos, dependencias, `target`, builds, bases y logs privados. Valida Gitleaks y la integridad del ZIP antes de etiquetar.
5. **Dejarlo listo para nube**. La configuración usa Vercel, Render `remata-*`, Neon directo con SSL, CloudAMQP y Brevo. No hay URLs públicas ni credenciales disponibles. En este PC Docker Compose pudo validar configuración anteriormente, con el motor Docker pendiente. Verifica localmente con los scripts Windows. Cuando tenga mis cuentas, guíame en orden por GitHub, Neon, CloudAMQP, Brevo, Blueprint de Render, Vercel con raíz `frontend`, CORS, `scripts/smoke-nube.ps1` y pgAdmin con SSL. Para el correo real, faltan `MAIL_USERNAME` y `MAIL_PASSWORD` locales y apuntar `MAIL_HOST` al relay de Brevo. Conserva Mailpit como evidencia local y no prometas entrega real hasta comprobarla.

## Entrega que espero de la otra IA

Evalúa estos 11 requisitos del profesor en la versión que entregues.

1. Core e-commerce con usuarios, productos, carrito y compras.
2. Al menos tres microservicios. Remata tiene cuatro.
3. Una base de datos para persistir y poder inspeccionar los recorridos.
4. Sistema de notificaciones comprobado.
5. Cinco APIs con lógica propia por microservicio, además del CRUD básico.
6. Cobertura de pruebas unitarias de al menos 80 % por servicio. Distingue este requisito de la cobertura conjunta.
7. Consumo efectivo de al menos dos APIs externas.
8. Despliegue funcional de punta a punta. Distingue Windows, contenedores y nube.
9. Pruebas automatizadas de carga y estrés con los resultados por ruta.
10. Tiempo máximo de 500 ms por API en el perfil evaluado.
11. Artículo IEEEtran en LaTeX con resultados reales. Presenta la versión corta de máximo cuatro páginas y conserva la ampliada como material adicional.

Devuelve un resumen en español que pueda leer en clase. Incluye los 11 requisitos del profesor con estado Cumple, Parcial o No cumple, evidencia de cada uno, rutas que superan 500 ms y los pasos que dependen de mis cuentas. Enlaza el ZIP actualizado, la guía, las dos ediciones IEEE y las capturas. Indica qué se verificó con una prueba real y qué sigue pendiente. No inventes URLs, resultados, certificaciones de los lotes ni una publicación en la nube.
