SELECT conrelid::regclass AS tabla, confrelid::regclass AS referencia, pg_get_constraintdef(oid) AS restriccion
FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace ORDER BY 1;
SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY tablename,indexname;
SELECT categoria,count(*) AS lotes FROM subasta_lotes WHERE semilla LIKE 'vertice-%' GROUP BY categoria ORDER BY 1;
SELECT id,titulo,estado,pujas,version FROM subasta_lotes WHERE semilla LIKE 'vertice-%' ORDER BY id;
SELECT count(*) FROM products WHERE sku_demo LIKE 'STORE-%';