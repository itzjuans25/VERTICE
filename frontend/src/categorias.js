const tecnologia = ['Computadores y clones', 'Procesadores', 'Memoria RAM', 'Almacenamiento',
  'Tarjetas gráficas', 'Placas base', 'Fuentes y cajas', 'Periféricos', 'Celulares y tablets', 'Consolas', 'Redes y accesorios'];

export const categoriasMercado = [
  { slug: 'tecnologia', nombre: 'Tecnología', tienda: tecnologia, lotes: ['ELECTRONICA'], imagen: '/subastas/electronica.webp' },
  { slug: 'relojes', nombre: 'Relojes', tienda: ['Relojes'], lotes: ['RELOJES'], imagen: '/mercado/reloj-minimal.webp' },
  { slug: 'joyeria', nombre: 'Joyería y accesorios', tienda: ['Joyería y accesorios'], lotes: ['JOYERIA'], imagen: '/mercado/pendientes-acero.webp' },
  { slug: 'vehiculos', nombre: 'Vehículos', tienda: ['Accesorios de moto'], lotes: ['CARROS','MOTOS'], imagen: '/mercado/casco-urbano.webp' },
  { slug: 'hogar', nombre: 'Hogar y cocina', tienda: ['Hogar y cocina'], lotes: ['HOGAR'], imagen: '/mercado/cafetera-italiana.webp' },
  { slug: 'deportes', nombre: 'Deportes y aire libre', tienda: ['Deportes y aire libre'], lotes: ['DEPORTES'], imagen: '/mercado/mochila-sendero.webp' },
  { slug: 'moda', nombre: 'Moda y bolsos', tienda: ['Moda y bolsos'], lotes: ['MODA','OTROS'], imagen: '/mercado/bolso-lona.webp' },
  { slug: 'coleccionables', nombre: 'Coleccionables y arte', tienda: ['Coleccionables y arte'], lotes: ['COLECCIONABLES'], imagen: '/mercado/miniatura-velero.webp' },
  { slug: 'herramientas', nombre: 'Herramientas', tienda: ['Herramientas'], lotes: ['HERRAMIENTAS'], imagen: '/mercado/llave-ajustable.webp' },
  { slug: 'calzado', nombre: 'Zapatillas y calzado', tienda: [], lotes: ['CALZADO'], imagen: '/mercado/vertice-01.webp' },
];

export const nombresCategorias = Object.fromEntries(categoriasMercado.flatMap(({ lotes,nombre }) => lotes.map(lote => [lote,nombre])));
export const categoriaPorSlug = slug => categoriasMercado.find(c => c.slug === slug);
export const categoriaProducto = nombre => categoriasMercado.find(c => c.tienda.includes(nombre));
export const categoriaLote = lote => categoriasMercado.find(c => c.lotes.includes(lote));

export const vitrinas = [
  { slug:'calzado', nombre:'Zapatillas y calzado', foto:'01' },
  { slug:'moda', nombre:'Ropa y moda', foto:'02' },
  { slug:'relojes', nombre:'Relojes', foto:'06' },
  { slug:'joyeria', nombre:'Joyería', foto:'09' },
  { slug:'vehiculos', nombre:'Motos', grupo:'MOTOS', foto:'17' },
  { slug:'vehiculos', nombre:'Carros', grupo:'CARROS', foto:'16' },
  { slug:'coleccionables', nombre:'Coleccionables y arte', foto:'12' },
];

export const categoriasTienda = categoriasMercado.filter(({ slug }) =>
  ['relojes', 'joyeria', 'moda', 'coleccionables'].includes(slug));

export const categoriasSubastas = categoriasMercado.filter(({ slug }) =>
  vitrinas.some(vitrina => vitrina.slug === slug));

export function destacadosPorCategoria(items, fecha = new Date(), limite = 8) {
  const grupos = categoriasMercado.map(c => items.filter(p => categoriaProducto(p.categoria) === c));
  const turno = Math.floor(fecha.getTime()/86400000) % grupos.length;
  const rotados = [...grupos.slice(turno), ...grupos.slice(0, turno)];
  return [0,1].flatMap(i => rotados.flatMap(g => g[i] ? [g[i]] : [])).slice(0,limite);
}
