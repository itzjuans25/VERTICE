const marca = {
  nombre: 'Remata',
  lema: 'Compra directa y subastas',
  descripcion: 'Tienda general en Colombia. Compra a precio fijo o participa en subastas de tecnología, hogar, moda, herramientas y más.',
  colores: {
    background: '#0E1014', surface: '#161A21', raised: '#1E232C', border: '#2A313C',
    control: '#7C899A', text: '#EEF1F5', muted: '#A3AEBD', accent: '#F2B134',
    hover: '#D99A1F', ink: '#1A1204', success: '#3DD68C', warn: '#FB923C', error: '#F87171',
  },
  simbolo: {
    vista: '0 0 64 64',
    martillo: [[14,9,36,17,4.5],[9.5,11.5,6,12,2.5],[48.5,11.5,6,12,2.5],[29,26,6,30,3]],
    base: [9,54,46,6,3],
  },
};
const tituloMarca = titulo => titulo ? titulo + ' | ' + marca.nombre : marca.nombre + ' | ' + marca.lema;
const pesos = new Intl.NumberFormat('es-CO', { style:'currency', currency:'COP', maximumFractionDigits:2, minimumFractionDigits:0 });
module.exports = { marca, tituloMarca, pesos };
