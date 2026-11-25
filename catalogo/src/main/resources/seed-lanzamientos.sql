INSERT INTO products (
    sku_demo, nombre, descripcion, precio, stock, categoria, imagen_url, condicion,
    marca, modelo, garantia_dias, descripcion_larga, incluye, especificaciones, estado_publicacion, tipo_vendedor
) VALUES
('VERTICE-ED-01', 'Grabado Sal de Bruma',
 'Dos líneas de tinta ciruela recuerdan el paso de una marea baja. El taller imaginario Umbral trabajó cada estampa sobre papel de algodón.',
 310000, 24, 'Coleccionables y arte', '/mercado/edicion-sal.webp', 'NUEVO',
 'Taller Umbral', 'Sal de Bruma', 0, 'La plancha ficticia se cortó en madera blanda para que el veteado quedara a la vista.',
 'Grabado en marco y ficha de edición', '{"Papel":"Algodón","Superficie":"28 × 35 cm","Técnica ilustrativa":"Xilografía"}', 'ACTIVA', 'TIENDA'),
('VERTICE-ED-02', 'Reloj Aureola 68',
 'Una esfera marfil rodea un segundero delgado y sin inscripciones. La caja del estudio ficticio Aureola conserva la huella circular del cepillado.',
 1350000, 120, 'Relojes', '/mercado/edicion-aureola.webp', 'NUEVO',
 'Estudio Aureola', 'Aureola 68', 180, 'Los índices en relieve dejan pasar la luz sin ocultar el grano de la esfera.',
 'Reloj con correa y ficha de edición', '{"Caja":"38 mm","Correa":"Cuero color burdeos","Movimiento ilustrativo":"Mecánico manual"}', 'ACTIVA', 'TIENDA'),
('VERTICE-ED-03', 'Bolso Pliegue Claro',
 'El cierre del bolso queda oculto bajo un pliegue de cuero tabaco claro. La artesana ficticia Iris Caldera dibujó el patrón con una sola costura lateral.',
 590000, 36, 'Moda y bolsos', '/mercado/edicion-pliegue.webp', 'NUEVO',
 'Taller Caldera', 'Pliegue Claro', 90, 'La correa ajustable se ancla a ambos lados y mantiene libre el frente.',
 'Bolso, correa y ficha de edición', '{"Material":"Cuero ilustrativo","Medidas":"22 × 16 cm","Herrajes":"Latón mate"}', 'ACTIVA', 'TIENDA')
ON CONFLICT (sku_demo) DO NOTHING;

INSERT INTO catalogo_ediciones(producto_id, total, maximo_por_cliente, apertura, codigo, serie)
SELECT p.id, e.total, 1, CURRENT_TIMESTAMP + e.desplazamiento, e.codigo, e.serie
FROM (VALUES
    ('VERTICE-ED-01', 24, INTERVAL '-1 day', 'VT-E-SAL8K2', '001/024'),
    ('VERTICE-ED-02', 120, INTERVAL '2 days', 'VT-E-AUR68K', '037/120'),
    ('VERTICE-ED-03', 36, INTERVAL '4 days', 'VT-E-PLG36Q', '001/036')
) AS e(sku, total, desplazamiento, codigo, serie)
JOIN products p ON p.sku_demo=e.sku
ON CONFLICT (producto_id) DO NOTHING;
