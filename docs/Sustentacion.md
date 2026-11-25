# Guion de sustentación de VÉRTICE

Empieza con la portada. Cambia entre **En tienda** y **En subasta** en la misma categoría. Explica por qué un casco puede comprarse al precio publicado y por qué un vehículo completo solo aparece como lote simulado.

**1. Arquitectura, 1 minuto.** Abre la tienda y los cuatro Swagger. Auth emite JWT, Catálogo maneja inventario y pedidos, Subastas valida ofertas y adjudica lotes, y Notificaciones entrega avisos. PostgreSQL conserva los datos y RabbitMQ transporta los mensajes.

**2. Compra, 2 minutos.** Filtra un portátil usado por marca y condición. Abre la ficha y la segunda imagen. Explica el grado y la garantía. Registra una cuenta, compra y muestra el pedido en **Mis compras** y su correo en Mailpit.

**3. Prueba de APIs, 1 minuto.** Abre el reporte Newman. Muestra el JSON de una ficha y cómo la colección guarda el token. Enseña el resultado de JaCoCo y el máximo de k6 con 8 usuarios.

**4. Subasta, 2 minutos.** Abre una subasta con dos cuentas. Puja desde cada una y muestra cómo cambia el precio. Abre el historial enmascarado, la cuenta regresiva y el correo de puja superada. Usa la captura del lote 18 para mostrar adjudicación y pago sin esperar el cierre en la presentación.

**5. Persistencia y mediciones, 1 minuto.** Abre pgAdmin, ejecuta la verificación y muestra los productos, las pujas y el pedido. En el informe explica cobertura, máximo y p95. Presenta las conclusiones locales por separado del plan Vercel, Render, Neon y CloudAMQP.

**¿Qué sucede si ambos pujan por el mismo monto?** PostgreSQL serializa los intentos sobre la fila. La primera cambia el precio, y la segunda falla si ya no llega al mínimo.

**¿Por qué se extiende el plazo?** Una oferta válida durante los últimos dos minutos suma dos minutos para que otra cuenta pueda responder.

**¿Y si se cae RabbitMQ?** La puja ya queda guardada. La bandeja registra el aviso en la misma transacción y reintenta publicar. El consumidor mantiene su cola de errores.

| Pregunta | Respuesta |
| --- | --- |
| ¿Cuándo baja el inventario? | La transacción del pedido descuenta solo si queda stock para todos los ítems. |
| ¿Por qué el envío es asíncrono? | El consumidor puede reintentar el correo sin revertir la compra confirmada. |
| ¿Cuáles son las dos APIs externas? | Frankfurter convierte COP/USD y Nager.Date aporta festivos para estimar entregas. |
| ¿Qué significa grado B? | En el seed representa marcas de uso. La ficha registra garantía, compatibilidad y estado previsto. |
| ¿Por qué importa el máximo de 500 ms? | Un promedio bajo puede ocultar una solicitud lenta. k6 conserva el máximo de cada operación. |
| ¿Por qué puede tardar el cambio de contraseña? | Verifica el hash anterior y deriva otro. En concurrencia esas operaciones compiten por recursos. |

