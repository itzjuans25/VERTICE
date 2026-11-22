# Guion de sustentación de TechStore

**1. Arquitectura, 1 minuto.** Abre la tienda y los Swagger. Explica que Auth emite JWT, Catálogo maneja inventario y pedidos, y Notificaciones envía los avisos con RabbitMQ. PostgreSQL persiste el estado.

**2. Compra, 2 minutos.** Filtra un portátil usado por marca y condición. Abre la ficha y la segunda imagen. Explica el grado y la garantía. Registra una cuenta, compra y muestra el pedido en **Mis compras** y su correo en Mailpit.

**3. Prueba de APIs, 1 minuto.** Abre el reporte Newman. Muestra el JSON de una ficha y cómo la colección guarda el token. Enseña el resultado de JaCoCo y el máximo de k6 con 8 usuarios.

**4. Persistencia, 1 minuto.** Abre pgAdmin, carga la verificación y muestra los productos agrupados y el pedido. Explica el diagrama ER. Render se usa si hace falta compartir un enlace público.

| Pregunta | Respuesta |
| --- | --- |
| ¿Cuándo baja el inventario? | La transacción del pedido descuenta solo si queda stock para todos los ítems. |
| ¿Por qué el envío es asíncrono? | El consumidor puede reintentar el correo sin revertir la compra confirmada. |
| ¿Cuáles son las dos APIs externas? | Frankfurter convierte COP/USD y Nager.Date aporta festivos para estimar entregas. |
| ¿Qué significa grado B? | En el seed representa marcas de uso. La ficha registra garantía, compatibilidad y estado previsto. |
| ¿Por qué importa el máximo de 500 ms? | Un promedio bajo puede ocultar una solicitud lenta. k6 conserva el máximo de cada operación. |
| ¿Por qué puede tardar el cambio de contraseña? | Verifica el hash anterior y deriva otro. En concurrencia esas operaciones compiten por recursos. |

