SELECT conrelid::regclass AS tabla, confrelid::regclass AS referencia, pg_get_constraintdef(oid) AS restriccion
FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace ORDER BY 1;
SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname;
SELECT categoria,count(*) AS lotes FROM subasta_lotes WHERE semilla LIKE 'vertice-%' GROUP BY categoria ORDER BY 1;
SELECT id,titulo,estado,pujas,version FROM subasta_lotes WHERE semilla LIKE 'vertice-%' ORDER BY id;
SELECT count(*) FROM products WHERE sku_demo LIKE 'STORE-%';

-- La liquidación conserva el importe y los estados de despacho.
SELECT id, total, estado, requiere_pago_simulado, pago_simulado_en
FROM compras ORDER BY id DESC LIMIT 8;
SELECT id, lote_id, monto, pagada
FROM subasta_ordenes ORDER BY id DESC LIMIT 8;

-- Curaduría pública y plan real de la portada.
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM subasta_lotes
WHERE estado = 'ACTIVA' AND inicio <= now() AND fin > now()
  AND NULLIF(BTRIM(CAST(atributos AS jsonb)->>'Por qué se subasta'), '') IS NOT NULL
  AND (CAST(atributos AS jsonb)->>'Visibilidad') IS DISTINCT FROM 'Interna'
  AND titulo !~* '^(k6-|qa-|ensayo-|ensayo curado[[:space:]]*[·–-])'
ORDER BY fin, id
LIMIT 8;
