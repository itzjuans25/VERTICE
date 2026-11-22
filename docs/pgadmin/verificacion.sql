-- Consultas de solo lectura. Ejecuta en la base ecommerce.
SELECT categoria, condicion, count(*) AS productos
FROM products WHERE estado_publicacion = 'ACTIVA' AND sku_demo LIKE 'STORE-%'
GROUP BY categoria, condicion ORDER BY categoria, condicion;

SELECT count(*) AS productos_demo,
       count(*) FILTER (WHERE coalesce(imagen_url, '') = '') AS sin_imagen,
       count(*) FILTER (WHERE coalesce(descripcion, '') = '' OR coalesce(descripcion_larga, '') = '') AS sin_descripcion,
       count(*) FILTER (WHERE imagen_secundaria_url IS NOT NULL) AS con_segunda_vista
FROM products WHERE estado_publicacion = 'ACTIVA' AND sku_demo LIKE 'STORE-%';

SELECT count(*) AS activos,
       count(*) FILTER (WHERE coalesce(imagen_url, '') NOT LIKE '/techtrade/%'
                         OR coalesce(descripcion, '') = '' OR coalesce(descripcion_larga, '') = ''
                         OR coalesce(especificaciones, '{}'::jsonb) = '{}'::jsonb
                         OR condicion NOT IN ('NUEVO','USADO')
                         OR (condicion = 'USADO' AND coalesce(grado, '') NOT IN ('A','B','C'))
                         OR (condicion = 'NUEVO' AND grado IS NOT NULL)
                         OR coalesce(stock, -1) < 0 OR coalesce(precio, 0) <= 0
                         OR coalesce(garantia_dias, -1) < 0) AS fichas_incompletas
FROM products WHERE estado_publicacion = 'ACTIVA';

SELECT nombre, stock FROM products
WHERE estado_publicacion = 'ACTIVA' AND sku_demo LIKE 'STORE-%' AND stock <= 3
ORDER BY stock, nombre;

SELECT id, fecha_compra, estado, total FROM compras ORDER BY id DESC LIMIT 5;

SELECT count(*) AS sms_simulados, max(creado_en) AS ultimo_sms FROM sms_simulado;
SELECT count(*) FILTER (WHERE intentos < 5) AS en_bandeja,
       count(*) FILTER (WHERE intentos >= 5) AS agotados FROM notificaciones_pendientes;
-- SMTP se comprueba en Mailpit o en la actividad transaccional de Brevo.
-- Se elimina la fila de notificaciones_pendientes después de confirmar RabbitMQ.
