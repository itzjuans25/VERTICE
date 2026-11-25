# Modelo de datos de VÉRTICE

PostgreSQL usa una base `ecommerce` y cuatro contextos. El recorrido de compra actualiza inventario y pedidos en la misma transacción de Catálogo. La relación por ID de usuario entre servicios se valida en aplicación.

![Modelo de datos de VÉRTICE](vertice-mer.svg)

```mermaid
erDiagram
  USUARIO ||..o{ COMPRAS : "usuario_id lógico"
  USUARIO ||..o| CARRITO : "usuario_id lógico"
  CARRITO ||--o{ ITEM_CARRITO : FK
  COMPRAS ||--o{ ITEM_CARRITO : FK
  PRODUCTS ||..o{ ITEM_CARRITO : "producto_id lógico"
  PRODUCTS ||--o{ FOTOS_PUBLICACION : FK
  USUARIO ||..o{ SUBASTA_PUJAS : "usuario_id lógico"
  SUBASTA_LOTES ||--o{ SUBASTA_PUJAS : FK
  SUBASTA_LOTES ||--o| SUBASTA_ORDENES : "FK única por lote"
  USUARIO ||..o{ SUBASTA_ORDENES : "ganador lógico"
  USUARIO ||..o{ NOTIFICACION_AVISOS : "destinatario lógico"
```

`subasta_avisos` guarda publicaciones pendientes por clave de evento, mientras `notificaciones_pendientes` respalda el encolado de avisos y `sms_simulado` registra SMS simulados. Los índices principales de Subastas cubren estado y cierre, categoría y condición, pujas por lote y usuario, y órdenes por usuario.

[Consultas para inspeccionar PostgreSQL](vertice-inspeccion.sql) · [Claves foráneas inspeccionadas](../../resultados/vertice-datos.json)
