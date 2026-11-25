-- Ajusta exclusivamente las rutas de imagen de los lotes ilustrativos originales.
UPDATE subasta_lotes AS l
SET imagenes=v.nueva
FROM (VALUES
    ('Reloj de esfera plateada', '["/subastas/relojes.webp"]','["/mercado/lote-reloj-plateado.webp"]'),
    ('Alianza lisa de bisutería', '["/subastas/joyeria.webp"]','["/mercado/lote-alianza.webp"]'),
    ('Automóvil urbano de cinco puertas', '["/subastas/carros.webp"]','["/mercado/lote-hatchback.webp"]'),
    ('Motocicleta de paseo', '["/subastas/motos.webp"]','["/mercado/lote-moto-paseo.webp"]'),
    ('Cámara sin espejo para iniciación', '["/subastas/electronica.webp"]','["/mercado/lote-camara-sin-espejo.webp"]'),
    ('Miniatura de exploración espacial', '["/subastas/coleccionables.webp"]','["/mercado/lote-miniatura-astronauta.webp"]'),
    ('Lámpara de mesa metálica', '["/subastas/hogar.webp"]','["/mercado/lote-lampara-mesa.webp"]'),
    ('Bolso de fin de semana', '["/subastas/otros.webp"]','["/mercado/lote-bolso-fin.webp"]')
) AS v(titulo,anterior,nueva)
WHERE l.semilla LIKE 'demo-%' AND l.titulo=v.titulo AND l.imagenes=v.anterior;

-- Completa las categorías sin un lote vigente durante al menos un día.
INSERT INTO subasta_lotes(semilla,titulo,descripcion,categoria,condicion,atributos,imagenes,
    precio_inicial,incremento,precio_actual,tienda_email,estado,inicio,fin)
SELECT 'remata-' || v.familia || '-' || CURRENT_DATE,v.titulo,v.descripcion,v.categoria,'USADO',v.atributos,v.imagenes,
    v.precio,v.incremento,v.precio,'subastas@example.test','ACTIVA',CURRENT_TIMESTAMP - INTERVAL '2 minutes',
    CURRENT_TIMESTAMP + INTERVAL '3 days'
FROM (VALUES
    ('relojes','Reloj de pulsera de caja bronce','RELOJES','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Correa":"Textil verde","Movimiento":"Cuarzo"}','["/mercado/lote-reloj.webp"]',87000,5000),
    ('joyeria','Collar decorativo de estilo vintage','JOYERIA','Accesorio ornamental ilustrativo, sin certificación ni material precioso declarado.','{"Material":"Aleación y dije ornamental","Quilates":"No aplica"}','["/mercado/lote-collar-vintage.webp"]',62000,5000),
    ('vehiculos','Automóvil compacto de cinco puertas','CARROS','Vehículo ilustrativo. Lote ficticio, sin traspaso real.','{"Año":"2018","Kilometraje":"72000 km"}','["/mercado/lote-carro-urbano.webp"]',19000000,220000),
    ('hogar','Lámpara articulada de metal oscuro','HOGAR','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Material":"Metal","Uso":"Escritorio"}','["/mercado/lote-lampara-metal.webp"]',76000,6000),
    ('deportes','Esterilla terracota con cinta','DEPORTES','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Espesor":"8 mm","Medida":"175 × 61 cm"}','["/mercado/lote-esterilla.webp"]',59000,5000),
    ('moda','Bolso burdeos de viaje','MODA','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Capacidad":"25 L","Material":"Lona"}','["/mercado/lote-bolso.webp"]',98000,6000),
    ('coleccionables','Figura de cerámica para repisa','COLECCIONABLES','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Material":"Cerámica","Altura":"14 cm"}','["/mercado/lote-figura-ceramica.webp"]',54000,5000),
    ('herramientas','Taladro compacto a batería','HERRAMIENTAS','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Tensión":"12 V","Mandril":"10 mm"}','["/mercado/lote-taladro.webp"]',169000,10000),
    ('tecnologia','Cámara digital compacta plateada','ELECTRONICA','Lote ilustrativo para el recorrido de compra mediante puja y pago simulado.','{"Cuerpo":"Plateado","Accesorio":"Correa"}','["/mercado/lote-camara.webp"]',390000,20000)
) AS v(familia,titulo,categoria,descripcion,atributos,imagenes,precio,incremento)
WHERE NOT EXISTS (
    SELECT 1 FROM subasta_lotes l
    WHERE l.semilla IS NOT NULL AND l.estado='ACTIVA' AND l.fin > CURRENT_TIMESTAMP + INTERVAL '1 day'
      AND (l.categoria=v.categoria OR (v.familia='vehiculos' AND l.categoria='MOTOS') OR (v.familia='moda' AND l.categoria='OTROS'))
)
ON CONFLICT (semilla) DO NOTHING;
