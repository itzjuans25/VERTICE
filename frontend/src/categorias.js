const electronica = ['Computadores y clones', 'Procesadores', 'Memoria RAM', 'Almacenamiento',
  'Tarjetas gráficas', 'Placas base', 'Fuentes y cajas', 'Periféricos', 'Celulares y tablets', 'Redes y accesorios'];

export const categoriasMercado = [
  { slug: 'relojes', nombre: 'Relojes', lote: 'RELOJES', tienda: ['Relojes'] },
  { slug: 'joyeria', nombre: 'Joyería', lote: 'JOYERIA', tienda: ['Joyería', 'Accesorios'] },
  { slug: 'carros', nombre: 'Carros', lote: 'CARROS', tienda: ['Accesorios de carro'] },
  { slug: 'motos', nombre: 'Motos', lote: 'MOTOS', tienda: ['Accesorios de moto'] },
  { slug: 'electronica', nombre: 'Electrónica', lote: 'ELECTRONICA', tienda: electronica },
  { slug: 'coleccionables', nombre: 'Coleccionables', lote: 'COLECCIONABLES', tienda: ['Consolas', 'Coleccionables'] },
  { slug: 'hogar', nombre: 'Hogar', lote: 'HOGAR', tienda: ['Hogar', 'Cocina'] },
  { slug: 'otros', nombre: 'Otros', lote: 'OTROS', tienda: ['Deportes', 'Otros'] },
];

export const nombresCategorias = Object.fromEntries(categoriasMercado.map(({ lote, nombre }) => [lote, nombre]));
export const categoriaProducto = nombre => categoriasMercado.find(c => c.tienda.includes(nombre));
export const categoriaLote = lote => categoriasMercado.find(c => c.lote === lote);