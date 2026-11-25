const marca = {
  nombre: 'VÉRTICE',
  lema: 'Piezas singulares y subastas curadas',
  descripcion: 'Casa de subastas de demostración en Colombia. Descubre moda de autor, relojería, joyería, arte y vehículos de exhibición.',
  simbolo: { vista:'0 0 32 32', trazo:'M4 6h7l5 12 5-12h7L16 27 4 6Zm7 0 5 21 5-21' },
  taller: 'Demostración académica. Los pagos, las entregas, las procedencias ilustrativas y los traspasos de vehículos son simulados.',
};
const tituloMarca = titulo => titulo ? titulo + ' | ' + marca.nombre : marca.nombre + ' | ' + marca.lema;
const pesos = new Intl.NumberFormat('es-CO', { style:'currency', currency:'COP', maximumFractionDigits:2, minimumFractionDigits:0 });
module.exports = { marca, tituloMarca, pesos };