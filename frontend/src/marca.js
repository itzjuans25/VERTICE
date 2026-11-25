const marca = {
  nombre: 'Remata',
  lema: 'Tienda y subastas',
  descripcion: 'Tienda colombiana con productos nuevos y usados y subastas en pesos colombianos.',
  colores: {
    background: '#0E1014', surface: '#161A21', raised: '#1E232C', border: '#2A313C',
    control: '#7C899A', text: '#EEF1F5', muted: '#A3AEBD', accent: '#F2B134',
    hover: '#D99A1F', ink: '#1A1204', success: '#3DD68C', warn: '#FB923C', error: '#F87171',
  },
  simbolo: {
    trazo: 'M5 19h14M8 15l7-7m-6-3 6 6m-8-4 4-4 7 7-4 4',
    etiqueta: 'M3 12h4l2 2-3 3H3z',
  },
};
const tituloMarca = titulo => titulo ? titulo + ' | ' + marca.nombre : marca.nombre + ' | ' + marca.lema;
module.exports = { marca, tituloMarca };