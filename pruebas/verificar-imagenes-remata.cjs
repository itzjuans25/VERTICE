const assert = require('node:assert/strict');
const { readFileSync, writeFileSync } = require('node:fs');
const { createHash } = require('node:crypto');
const vistas = require('../frontend/src/vistasTecnologia.json');

const base = 'http://127.0.0.1:9092/api/ecommerce/producto/explorar?tamano=48';
const lotes = 'http://127.0.0.1:9094/api/ecommerce/subastas?tamano=48';
async function json(url) {
  const r = await fetch(url,{signal:AbortSignal.timeout(12000)});
  assert.ok(r.ok,url + ' HTTP ' + r.status);
  return r.json();
}

async function main() {
  const productos = [];
  const subastas = [];
  const primero = await json(base);
  productos.push(...primero.items);
  for (let i=1;i<primero.paginas;i++) productos.push(...(await json(base+'&pagina='+i)).items);
  const activas = await json(lotes);
  subastas.push(...activas.items);
  for (let i=1;i<Math.ceil(activas.total/activas.tamano);i++) subastas.push(...(await json(lotes+'&pagina='+i)).items);
  const visibles = subastas.filter(l => l.estado === 'ACTIVA' && Date.parse(l.fin)>Date.now());
  const rutas = [
    ...productos.map(p => [p.nombre,vistas[p.nombre]?.original === p.imagenUrl ? vistas[p.nombre].archivo : p.imagenUrl]),
    ...visibles.map(l => [l.titulo,l.imagenes[0]]),
  ];
  const existentes = new Map();
  const repetidas = [];
  for (const [nombre,ruta] of rutas) {
    assert.ok(ruta && /^\/(mercado|techtrade|subastas)\//.test(ruta),nombre + ' sin ruta de imagen local');
    const sha256 = createHash('sha256').update(readFileSync('frontend/public'+ruta)).digest('hex');
    if (existentes.has(sha256)) repetidas.push([existentes.get(sha256),nombre]);
    existentes.set(sha256,nombre);
  }
  const salida = {
    productos:productos.length,lotesActivos:visibles.length,imagenes:rutas.length,duplicadas:repetidas,
    categoriasTienda:[...new Set(productos.map(p => p.categoria))].sort(),
    categoriasSubastas:[...new Set(visibles.map(l => l.categoria))].sort(),
  };
  writeFileSync(process.env.IMAGENES_REPORTE || 'resultados/vertice-imagenes.json',JSON.stringify(salida,null,2)+'\n');
  console.log(JSON.stringify(salida));
  assert.equal(repetidas.length,0);
}

main().catch(error => { console.error(error.message); process.exitCode=1; });