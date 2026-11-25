INSERT INTO subasta_lotes(semilla,titulo,descripcion,categoria,condicion,atributos,imagenes,precio_inicial,incremento,precio_actual,tienda_email,estado,inicio,fin)
SELECT 'demo-' || v.numero || '-' || CURRENT_DATE, v.titulo,v.descripcion,v.categoria,v.condicion,v.atributos,v.imagenes,
 v.precio,v.incremento,v.precio,'subastas@example.test','ACTIVA',CURRENT_TIMESTAMP - INTERVAL '5 minutes',CURRENT_TIMESTAMP + v.duracion
FROM (VALUES
 (1,'Reloj de pulsera clásico','Esfera clara y correa oscura de cuero sintético. Imagen ilustrativa.','RELOJES','NUEVO','{}','["/subastas/relojes.webp"]',92000,5000,INTERVAL '22 minutes'),
 (2,'Reloj de esfera plateada','Caja redonda y cierre ajustable para uso diario. Imagen ilustrativa.','RELOJES','USADO','{}','["/subastas/relojes.webp"]',76000,4000,INTERVAL '48 minutes'),
 (3,'Anillo de acabado dorado','Anillo liso ilustrativo, talla 7. Adjudicación simulada. Imagen ilustrativa.','JOYERIA','NUEVO','{"material":"Acero con acabado dorado","quilates":"No aplica"}','["/subastas/joyeria.webp"]',54000,4000,INTERVAL '95 minutes'),
 (4,'Alianza lisa de bisutería','Acabado pulido y perfil delgado. Adjudicación simulada. Imagen ilustrativa.','JOYERIA','NUEVO','{"material":"Latón","quilates":"No aplica"}','["/subastas/joyeria.webp"]',43000,3000,INTERVAL '135 minutes'),
 (5,'Automóvil compacto manual','Cinco puertas. Lote ficticio sin traspaso real. Imagen ilustrativa.','CARROS','USADO','{"Año":"2017","Kilometraje":"78000 km"}','["/subastas/carros.webp"]',18000000,200000,INTERVAL '180 minutes'),
 (6,'Automóvil urbano de cinco puertas','Hatchback de uso familiar. Lote ficticio sin traspaso real. Imagen ilustrativa.','CARROS','USADO','{"Año":"2019","Kilometraje":"61000 km"}','["/subastas/carros.webp"]',22000000,250000,INTERVAL '220 minutes'),
 (7,'Moto urbana 150 cc','Moto de transmisión manual. Lote ficticio sin traspaso real. Imagen ilustrativa.','MOTOS','USADO','{"Año":"2020","Kilometraje":"19000 km"}','["/subastas/motos.webp"]',3200000,50000,INTERVAL '280 minutes'),
 (8,'Motocicleta de paseo','Modelo ilustrativo de uso recreativo. Lote ficticio sin traspaso real. Imagen ilustrativa.','MOTOS','USADO','{"Año":"2022","Kilometraje":"12300 km"}','["/subastas/motos.webp"]',4100000,60000,INTERVAL '330 minutes'),
 (9,'Cámara digital compacta','Controles manuales y correa para fotografía de viaje. Imagen ilustrativa.','ELECTRONICA','NUEVO','{}','["/subastas/electronica.webp"]',420000,18000,INTERVAL '400 minutes'),
 (10,'Cámara sin espejo para iniciación','Cuerpo compacto con agarre texturizado. Imagen ilustrativa.','ELECTRONICA','NUEVO','{}','["/subastas/electronica.webp"]',630000,22000,INTERVAL '480 minutes'),
 (11,'Figura de astronauta','Figura decorativa de resina, 18 cm. Imagen ilustrativa.','COLECCIONABLES','USADO','{}','["/subastas/coleccionables.webp"]',58000,5000,INTERVAL '720 minutes'),
 (12,'Miniatura de exploración espacial','Figura de sobremesa con base texturizada. Imagen ilustrativa.','COLECCIONABLES','NUEVO','{}','["/subastas/coleccionables.webp"]',64000,5000,INTERVAL '1080 minutes'),
 (13,'Lámpara de escritorio regulable','Brazo metálico articulado y pantalla orientable. Imagen ilustrativa.','HOGAR','NUEVO','{}','["/subastas/hogar.webp"]',72000,5000,INTERVAL '1440 minutes'),
 (14,'Lámpara de mesa metálica','Pantalla clara para iluminación de lectura. Imagen ilustrativa.','HOGAR','USADO','{}','["/subastas/hogar.webp"]',67000,4000,INTERVAL '2100 minutes'),
 (15,'Bolso de viaje compacto','Compartimiento principal amplio y correa ajustable. Imagen ilustrativa.','OTROS','NUEVO','{}','["/subastas/otros.webp"]',89000,6000,INTERVAL '2880 minutes'),
 (16,'Bolso de fin de semana','Tela resistente y bolsillos exteriores con cremallera. Imagen ilustrativa.','OTROS','NUEVO','{}','["/subastas/otros.webp"]',93000,6000,INTERVAL '4320 minutes')
) AS v(numero,titulo,descripcion,categoria,condicion,atributos,imagenes,precio,incremento,duracion)
WHERE NOT EXISTS (SELECT 1 FROM subasta_lotes WHERE semilla IS NOT NULL AND estado IN ('PROGRAMADA','ACTIVA') AND fin>CURRENT_TIMESTAMP)
ON CONFLICT (semilla) DO NOTHING;
