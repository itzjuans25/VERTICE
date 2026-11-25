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
];

export const nombresCategorias = Object.fromEntries(categoriasMercado.flatMap(({ lotes,nombre }) => lotes.map(lote => [lote,nombre])));
export const categoriaPorSlug = slug => categoriasMercado.find(c => c.slug === slug);
export const categoriaProducto = nombre => categoriasMercado.find(c => c.tienda.includes(nombre));
export const categoriaLote = lote => categoriasMercado.find(c => c.lotes.includes(lote));

export const vitrinas = [
  { nombre:'Calzado y moda de autor', grupo:'MODA', foto:'01' },
  { nombre:'Relojería', grupo:'RELOJES', foto:'06' },
  { nombre:'Joyería', grupo:'JOYERIA', foto:'09' },
  { nombre:'Arte y colección', grupo:'COLECCIONABLES', foto:'12' },
  { nombre:'Vehículos de exhibición', grupo:'CARROS,MOTOS', foto:'16' },
];

export function destacadosPorCategoria(items, fecha = new Date(), limite = 8) {
  const grupos = categoriasMercado.map(c => items.filter(p => categoriaProducto(p.categoria) === c));
  const turno = Math.floor(fecha.getTime()/86400000) % grupos.length;
  const rotados = [...grupos.slice(turno), ...grupos.slice(0, turno)];
  return [0,1].flatMap(i => rotados.flatMap(g => g[i] ? [g[i]] : [])).slice(0,limite);
}
