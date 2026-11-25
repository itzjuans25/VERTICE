-- Archiva los lotes de inventario genérico sin borrar referencias de pedidos.
UPDATE products SET estado_publicacion = 'ARCHIVADA'
WHERE estado_publicacion = 'ACTIVA' AND (
  sku_demo ~ '^STORE-[0-9]{3}$' OR (
    sku_demo ~ '^REMATA-[0-9]{3}$' AND sku_demo NOT IN (
      'REMATA-004', 'REMATA-005', 'REMATA-006', 'REMATA-021', 'REMATA-024', 'REMATA-025', 'REMATA-026', 'REMATA-027'
    )
  )
);

UPDATE products p
SET nombre = ficha.nombre, descripcion = ficha.relato, descripcion_larga = ficha.detalle,
    marca = ficha.estudio, modelo = ficha.modelo, compatibilidad = NULL
FROM (VALUES
  ('REMATA-004', 'Reloj de mesa Umbral', 'Un disco de madera oscura sostiene la esfera clara del reloj Umbral. El taller ficticio Vado abrió una serie breve de objetos para bibliotecas.', 'La base plana permite exhibir el reloj sobre un estante o escritorio.', 'Taller Vado', 'Umbral'),
  ('REMATA-005', 'Pendientes Órbita de escuadra', 'Dos medias lunas pulidas forman un par de contornos abiertos. La diseñadora ficticia Nara Olmo trabajó el acero como un dibujo doblado.', 'Cada pieza tiene cierre de presión. El acabado deja visible el metal.', 'Nara Olmo', 'Órbita'),
  ('REMATA-006', 'Collar Estuario', 'Una piedra verde oscura se suspende de una cadena corta. El estudio ficticio Junco imaginó el dije como un fragmento de orilla.', 'El dije es ornamental. La cadena ajusta con un broche de resorte.', 'Estudio Junco', 'Estuario'),
  ('REMATA-021', 'Bolso Trama Serena', 'La trama de lona deja ver el cruce irregular de las fibras. El taller ficticio Pórtico conservó el tono del tejido para esta pequeña colección.', 'Las asas y el cuerpo forman una silueta con base flexible.', 'Taller Pórtico', 'Trama Serena'),
  ('REMATA-024', 'Cartera Pliegue Menor', 'Un borde redondeado cambia de tono al plegarse la cartera. La marroquinera ficticia Alba Torcal eligió una forma compacta para su serie de bolsillo.', 'El interior tiene compartimientos para tarjetas y billetes doblados.', 'Alba Torcal', 'Pliegue Menor'),
  ('REMATA-025', 'Baliza de Invierno', 'Un faro pequeño capta una línea de luz en el relieve de su cuerpo. El taller ficticio Astilla preparó la figura para una colección de costas imaginadas.', 'La pieza decorativa tiene base firme para exhibición en repisa.', 'Taller Astilla', 'Baliza de Invierno'),
  ('REMATA-026', 'Lámina Herbarium 05', 'Una hoja alargada y un nervio tenue ocupan el centro de la lámina. La ilustradora ficticia Lía Prado dibujó la planta para un cuaderno de estudio imaginario.', 'La lámina se entrega en marco y está destinada a exhibición interior.', 'Lía Prado', 'Herbarium 05'),
  ('REMATA-027', 'Velero Carta de Marea', 'Dos velas recortadas se alzan sobre un casco de madera clara. El estudio ficticio Cauce armó esta miniatura a partir de un plano imaginario.', 'El soporte estabiliza la miniatura para exhibirla en una vitrina.', 'Estudio Cauce', 'Carta de Marea')
) AS ficha(sku, nombre, relato, detalle, estudio, modelo)
WHERE p.sku_demo = ficha.sku AND p.modelo IS DISTINCT FROM ficha.modelo;
