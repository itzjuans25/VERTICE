const assert = require('node:assert/strict');
const { randomBytes } = require('node:crypto');
const { readFileSync, writeFileSync } = require('node:fs');
const { setTimeout: espera } = require('node:timers/promises');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');

const web = 'http://localhost:3000';
const auth = 'http://localhost:9090/api/ecommerce/usuario';
const catalogo = 'http://localhost:9092/api/ecommerce';
const listado = 'http://localhost:9094/api/ecommerce/subastas';
const mailpit = 'http://localhost:8025/api/v1/search';

async function api(url, { token, body, method = 'GET', status = 200 } = {}) {
  const headers = {};
  if (token) headers.Authorization = 'Bearer ' + token;
  if (body) headers['Content-Type'] = 'application/json';
  const response = await fetch(url, { method, headers, body: JSON.stringify(body), signal: AbortSignal.timeout(15000) });
  assert.equal(response.status, status, url);
  return response.headers.get('content-type')?.includes('json') ? response.json() : null;
}

async function cliente() {
  const email = 'edicion-' + randomBytes(8).toString('hex') + '@example.test';
  const datos = { email, password: 'Demo-' + randomBytes(16).toString('hex') };
  const usuario = await api(auth + '/save', { method: 'POST', body: { ...datos, nombre: 'Visitante de galería' }, status: 201 });
  const login = await api(auth + '/login', { method: 'POST', body: datos });
  return { id: usuario.id, email, token: login.token };
}

async function correoUnico(email, asunto) {
  const url = mailpit + '?query=' + encodeURIComponent('to:' + email);
  for (let i = 0; i < 30; i++) {
    const bandeja = await api(url);
    const mensajes = bandeja.messages.filter(m => m.Subject === asunto);
    if (mensajes.length) {
      assert.equal(mensajes.length, 1);
      return mensajes[0].ID;
    }
    await espera(500);
  }
  throw new Error('El correo de pago todavía no aparece en Mailpit.');
}

async function visual(salida) {
  const browser = await chromium.launch({ channel: 'msedge' });
  try {
    const lote = (await api(listado + '?curados=true&activas=true')).items[0];
    assert.ok(lote);
    salida.vistas = [];
    for (const ancho of [390,768,1280,1440,1920]) {
      const page = await browser.newPage({ viewport: { width: ancho, height: 940 }, reducedMotion: 'reduce' });
      page.setDefaultTimeout(45000);
      await page.goto(web);
      await page.getByRole('heading', { name: 'Piezas que no se repiten.' }).waitFor();
      await page.locator('#inicio-lanzamientos article').last().waitFor();
      await page.locator('#inicio-subastas article').last().waitFor();
      assert.equal(await page.locator('#inicio-subastas article').count(), 4);
      assert.equal(await page.locator('#inicio-lanzamientos article').count(), 3);
      const links = await page.locator('#inicio-categorias a').evaluateAll(items => items.map(a => ({
        url: a.href, texto: a.innerText,
        forma: Math.abs(a.getBoundingClientRect().height - a.getBoundingClientRect().width),
      })));
      assert.equal(links.length, 7);
      assert.ok(links.every(x => x.forma < 2));
      if (ancho === 1440) {
        const categorias = {'calzado':'CALZADO','moda':'MODA,OTROS','relojes':'RELOJES','joyeria':'JOYERIA','coleccionables':'COLECCIONABLES'};
        for (const enlace of links) {
          const { searchParams } = new URL(enlace.url);
          const grupo = searchParams.get('grupo') || categorias[searchParams.get('categoria')];
          const { total } = await api(listado + '?' + new URLSearchParams({ grupo, curados: true, activas: true }));
          assert.match(enlace.texto, new RegExp('\\b' + total + ' lote'));
        }
        assert.equal(await page.locator('#verificar-pieza, a[href^="/verificar"]').count(), 0);
        assert.equal(await page.getByText('ediciones curadas / pagos simulados', {exact:true}).count(), 1);
        await page.screenshot({path:'docs/capturas/vertice-curado-portada.png'});
        await page.screenshot({path:'docs/capturas/vertice-curado-inicio.png', fullPage:true});
      }
      const audit = [];
      async function medir(pantalla) {
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), ancho, pantalla);
        if (process.env.TECHSTORE_AXE_JS && [390, 768, 1440].includes(ancho)) {
          await page.addScriptTag({path:process.env.TECHSTORE_AXE_JS});
          const a = await page.evaluate(() => window.axe.run({runOnly:['wcag2a','wcag2aa','wcag21aa']}));
          audit.push({pantalla,fallos:a.violations.map(x=>({regla:x.id,nodos:x.nodes.map(n=>n.target)}))});
        }
      }
      await medir('inicio');
      await page.goto(web + '/subastas/' + lote.id);
      await page.getByRole('heading',{name:lote.titulo,exact:true}).waitFor();
      await page.getByRole('button',{name:'Vista de detalle del lote'}).click();
      await medir('ficha');
      if (ancho === 1440) {
        await page.screenshot({path:'docs/capturas/vertice-curado-ficha.png'});
        await page.goto(web + '/productos');
        await page.getByText('11 resultados', {exact:true}).waitFor();
        const compraFutura = page.getByRole('button', {name:'Próximamente: Reloj Aureola 68'});
        await compraFutura.waitFor();
        assert.ok(await compraFutura.isDisabled());
        await page.screenshot({path:'docs/capturas/vertice-curado-tienda.png'});
      }
      salida.vistas.push({ancho,axe:audit});
      console.log(`Vista editorial verificada a ${ancho} px.`);
      await page.close();
    }
  } finally {
    await browser.close();
  }
}

async function main() {
  const evidencia = {};
  const ediciones = await api(catalogo + '/lanzamientos');
  assert.equal(ediciones.length, 3);
  if (process.env.VERTICE_VISUAL_ONLY === 'true') {
    const previa = JSON.parse(readFileSync('resultados/vertice-curado-e2e.json','utf8'));
    await visual(previa);
    writeFileSync('resultados/vertice-curado-e2e.json',JSON.stringify(previa,null,2)+'\n');
    return;
  }
  const oferta = ediciones.find(e => e.titulo === 'Grabado Sal de Bruma');
  assert.ok(oferta.restantes >= 2);
  assert.ok(ediciones.every(e => e.serie && !Object.hasOwn(e, 'codigo')));
  const productos = await api(catalogo + '/producto/explorar?tamano=24');
  const lotes = await api(listado + '?curados=true&activas=true&tamano=24');
  assert.equal(productos.total, 11);
  assert.equal(lotes.total, 18);
  assert.ok(lotes.items.every(lote => !Object.hasOwn(lote.atributos, 'Certificado')));
  const fichas = [...productos.items, ...lotes.items];
  const primeras = fichas.map(f => f.descripcion.split('. ')[0]);
  assert.equal(new Set(primeras).size, fichas.length);
  assert.ok(fichas.every(f => [2,3,4].includes(f.descripcion.split('. ').length)));
  const { createHash } = require('node:crypto');
  const imagenes = fichas.map(f => {
    const url = f.imagenUrl || f.imagenes[0];
    return createHash('sha256').update(readFileSync('frontend/public' + url)).digest('hex');
  });
  assert.equal(new Set(imagenes).size, fichas.length);
  evidencia.curaduria = { productos: productos.total, lotes: lotes.total, relatos: fichas.length, imagenesUnicas: imagenes.length };
  const uno = await cliente();
  const dos = await cliente();
  await api(catalogo + '/carrito/agregar', { token: uno.token, method:'POST', body:{productoId:oferta.productoId,cantidad:2} });
  await api(catalogo + '/compras/realizar', { token: uno.token, method:'POST', status:400 });
  const stockTrasRechazo = await api(catalogo + '/producto/' + oferta.productoId);
  assert.equal(stockTrasRechazo.stock, oferta.restantes);
  await api(catalogo + '/carrito/actualizar/' + oferta.productoId + '?cantidad=1', { token: uno.token, method:'PUT' });
  const compra = await api(catalogo + '/compras/realizar', { token: uno.token, method:'POST', status:201 });
  const ruta = catalogo + '/compras/' + compra.id;
  await api(ruta + '/pago', { token: dos.token, method:'POST', status:404 });
  const pagos = await Promise.all(Array.from({length:2},()=>api(ruta+'/pago',{token:uno.token,method:'POST'})));
  assert.deepEqual(pagos[0].pagoSimulado,pagos[1].pagoSimulado);
  const confirmacion = pagos[0].pagoSimulado;
  await api(catalogo + '/carrito/agregar', { token: uno.token, method:'POST', body:{productoId:oferta.productoId,cantidad:1} });
  await api(catalogo + '/compras/realizar', { token: uno.token, method:'POST', status:400 });
  assert.equal((await api(catalogo + '/producto/' + oferta.productoId)).stock, oferta.restantes-1);
  evidencia.edicion = { compra:compra.id, pago:confirmacion, limiteVerificado:true, pagoConcurrente:true, aislamiento:true,
    correoPago:await correoUnico(uno.email,'Pago simulado registrado · '+confirmacion.referencia) };
  try {
    await visual(evidencia);
    evidencia.ok = true;
  } finally {
    writeFileSync('resultados/vertice-curado-e2e.json',JSON.stringify(evidencia,null,2)+'\n');
  }
}
main().catch(error=>{ console.error(error.message); process.exitCode=1; });
