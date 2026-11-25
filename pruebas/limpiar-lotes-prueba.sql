-- Solo limpia títulos sintéticos generados por las pruebas. Los lotes con semilla se conservan.
BEGIN;
CREATE TEMP TABLE lotes_prueba ON COMMIT DROP AS
SELECT id FROM subasta_lotes
WHERE semilla IS NULL AND titulo ~ '^(Subasta Newman [0-9]{13}|Validación de subasta [0-9]{13}|k6-[0-9]{13}-(preflight|[0-9]+-[0-9]+))$';
DELETE FROM subasta_avisos a USING lotes_prueba p WHERE split_part(a.clave,':',2)=p.id::text;
DELETE FROM subasta_ordenes a USING lotes_prueba p WHERE a.lote_id=p.id;
DELETE FROM subasta_pujas a USING lotes_prueba p WHERE a.lote_id=p.id;
DELETE FROM subasta_lotes l USING lotes_prueba p WHERE l.id=p.id;
COMMIT;
