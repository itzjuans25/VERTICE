-- Mantiene los pedidos históricos y retira del catálogo los seeds anteriores y los anuncios de particulares.
ALTER TABLE products ADD COLUMN IF NOT EXISTS sku_demo VARCHAR(40) UNIQUE;
UPDATE products SET estado_publicacion = 'ARCHIVADA'
WHERE (tipo_vendedor = 'PARTICULAR' OR sku_demo LIKE 'TECH-%' OR sku_demo LIKE 'DEMO-%')
  AND estado_publicacion IS DISTINCT FROM 'ARCHIVADA';
UPDATE products SET estado_publicacion = 'ACTIVA' WHERE estado_publicacion IS NULL;
UPDATE products SET condicion = 'USADO', grado = COALESCE(grado, 'B'),
       nota_estado = COALESCE(nota_estado, 'Equipo reacondicionado del catálogo anterior. Consulta el estado antes de comprar.')
WHERE estado_publicacion = 'ACTIVA' AND condicion = 'REACONDICIONADO';
UPDATE products SET condicion = 'NUEVO' WHERE condicion IS NULL;
UPDATE products SET marca = 'Genérica' WHERE marca IS NULL;
UPDATE products SET garantia_dias = 0 WHERE garantia_dias IS NULL;
CREATE INDEX IF NOT EXISTS ix_products_publicacion_recientes ON products (estado_publicacion, id DESC);
CREATE INDEX IF NOT EXISTS ix_products_publicacion_precio ON products (estado_publicacion, precio, id);
CREATE INDEX IF NOT EXISTS ix_products_categoria_estado ON products (categoria, estado_publicacion);
CREATE INDEX IF NOT EXISTS ix_products_condicion_estado ON products (condicion, estado_publicacion);
CREATE INDEX IF NOT EXISTS ix_products_marca_estado ON products (marca, estado_publicacion);
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS ix_products_nombre_busqueda ON products USING gin (lower(nombre) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_products_descripcion_busqueda ON products USING gin (lower(descripcion) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_products_modelo_busqueda ON products USING gin (lower(modelo) gin_trgm_ops);

