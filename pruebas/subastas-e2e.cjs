const assert = require('node:assert/strict');
const { randomBytes } = require('node:crypto');
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');

const tienda = 'http://localhost:3000';
const auth = 'http://127.0.0.1:9090/api/ecommerce/usuario';
const subastas = 'http://127.0.0.1:9094/api/ecommerce/subastas';
const mailpit = 'http://127.0.0.1:8025';
const marca = 'Cronógrafo de archivo · Serie Boreal';
const prefijo = 'e2e-subasta-' + Date.now();
const captura = (page, nombre) => page.screenshot({ path: 'docs/capturas/' + (process.env.TECHSTORE_SUBASTAS_PREFIJO || 'subastas-') + nombre + '.png', animations: 'disabled' });
const estado = { prueba: marca, vistas: [], correos: [] };

async function pedir(url, { body, token, method = 'GET' } = {}) {
  const headers = body ? { 'Content-Type': 'application/json' } : {};
  if (token) headers.Authorization = 'Bearer ' + token;
  const response = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : undefined, signal: AbortSignal.timeout(25000) });
  assert.ok(response.ok, method + ' ' + url + ' HTTP ' + response.status + ' ' + (response.ok ? '' : await response.text()));
  return response.status === 204 ? null : response.json();
}

function sql(query) {
  assert.ok(process.env.TECHSTORE_E2E_PSQL, 'Ejecuta con el wrapper local para acceder a PostgreSQL.');
  return execFileSync(process.env.TECHSTORE_E2E_PSQL, [
    '-X', '-w', '-h', '127.0.0.1', '-p', process.env.TECHSTORE_E2E_DB_PORT, '-U', process.env.TECHSTORE_E2E_DB_USERNAME,
    '-d', process.env.TECHSTORE_E2E_DB, '-v', 'ON_ERROR_STOP=1', '-tAc', query,
  ], { encoding: 'utf8', timeout: 15000 }).trim();
}

async function cuenta(nombre) {
  const body = { nombre, email: prefijo + '-' + nombre.toLowerCase() + '@example.test', password: 'Demo-' + randomBytes(16).toString('hex') };
  const usuario = await pedir(auth + '/save', { method: 'POST', body });
  return { ...body, id: usuario.id };
}

async function obtenerLogin(cuenta) {
  return pedir(auth + '/login', { method: 'POST', body: { email: cuenta.email, password: cuenta.password } });
}

async function abrir(page, ruta) {
  await page.goto(tienda + ruta, { waitUntil: 'domcontentloaded' });
  await page.locator('main h1').waitFor();
}

async function iniciar(page, login) {
  const store = require('node:fs').readFileSync('frontend/src/session.js', 'utf8');
  const clave = store.match(/const STORAGE_KEY = ['"]([^'"]+)['"]/);
  assert.ok(clave, 'La clave de almacenamiento de sesión debe estar definida.');
  const { token, id, nombre, email, role, expiraEnSegundos } = login;
  const session = { token, expiresAt: Date.now() + Number(expiraEnSegundos || 3600) * 1000, user: { id, nombre, email, role } };
  await page.addInitScript(({ key, session }) => localStorage.setItem(key, JSON.stringify(session)), { key: clave[1], session });
}

async function esperar(fn, timeout = 65000) {
  const limite = Date.now() + timeout;
  do {
    const resultado = await fn();
    if (resultado) return resultado;
    await new Promise(resolve => setTimeout(resolve, 1500));
  } while (Date.now() < limite);
  throw new Error('Se agotó el tiempo de espera');
}

async function correo(destino, asunto) {
  return esperar(async () => {
    const bandeja = await pedir(mailpit + '/api/v1/search?query=' + encodeURIComponent('to:' + destino));
    return bandeja.messages?.find(m => m.Subject === asunto);
  });
}

async function main() {
  const browser = await chromium.launch({ channel: 'msedge' });
  try {
    const admin = await cuenta('Admin');
    const uno = await cuenta('Ana');
    const dos = await cuenta('Luis');
    estado.postores = [uno.id, dos.id];
    sql('UPDATE usuario SET role=' + "'ADMIN'" + ' WHERE id=' + Number(admin.id));
    const { token } = await obtenerLogin(admin);
    const ahora = Date.now();
    const lote = await pedir(subastas, { method: 'POST', token, body: {
      titulo: marca, descripcion: 'Cronógrafo ilustrativo de cuerda manual. Pago y entrega simulados.', categoria: 'RELOJES',
      condicion: 'USADO', atributos: { 'Por qué se subasta': 'Caso ficticio de un calibre de archivo con diseño bicompax.', Visibilidad: 'Interna' },
      imagenes: ['/mercado/vertice-06.webp'],
      precioInicial: 100000, incremento: 5000, inicio: new Date(ahora - 4000), fin: new Date(ahora + 600000),
    } });
    assert.ok(Number.isInteger(lote.id));
    estado.lote = lote.id;
    for (const ancho of [390, 768, 1440]) {
      const page = await browser.newPage({ viewport: { width: ancho, height: 920 }, reducedMotion: 'reduce' });
      await abrir(page, '/subastas');
      await page.locator('main .mercado-card').first().waitFor();
      assert.equal(await page.getByRole('link', {name: marca, exact: true}).count(), 0);
      const dimensiones = await page.evaluate(() => ({
        viewport: innerWidth, documento: document.documentElement.scrollWidth,
        imagenes: [...document.images].every(i => i.alt !== undefined),
      }));
      assert.ok(dimensiones.documento <= ancho, 'Contenido se sale del viewport ' + ancho);
      const axe = process.env.TECHSTORE_AXE_JS;
      let accesibilidad;
      if (axe) {
        await page.addScriptTag({ path: axe });
        const auditoria = await page.evaluate(() => window.axe.run({ runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21aa'] } }));
        accesibilidad = { pasa: auditoria.passes.length, fallos: auditoria.violations.map(v => ({ regla: v.id, elementos: v.nodes.map(n => n.target) })) };
      }
      await captura(page, 'lista-' + ancho);
      estado.vistas.push({ ancho, ...dimensiones, accesibilidad });
      await page.close();
    }

    const ana = await browser.newPage();
    const luis = await browser.newPage();
    await iniciar(ana, await obtenerLogin(uno));
    await iniciar(luis, await obtenerLogin(dos));
    const ruta = '/subastas/' + lote.id;
    sql("UPDATE subasta_lotes SET fin=now()+interval '110 seconds' WHERE id=" + lote.id);
    const finInicial = (await pedir(subastas + '/' + lote.id)).lote.fin;
    await abrir(ana, ruta);
    await ana.getByRole('button', { name: 'Revisar oferta' }).click();
    await ana.getByRole('button', { name: 'Confirmar puja' }).click();
    await ana.getByText('Vas ganando', { exact: true }).waitFor();
    await abrir(luis, ruta);
    await luis.getByRole('button', { name: 'Revisar oferta' }).click();
    await luis.getByRole('button', { name: 'Confirmar puja' }).click();
    await luis.getByText('Vas ganando', { exact: true }).waitFor();
    await ana.getByText('Te superaron', { exact: true }).waitFor();
    await captura(ana, 'puja-superada');
    const detalle = await pedir(subastas + '/' + lote.id);
    assert.equal(detalle.historial.length, 2);
    assert.ok(Date.parse(detalle.lote.fin) > Date.parse(finInicial));
    estado.antiSniping = true;
    const superada = await correo(uno.email, 'Puja superada');
    estado.correos.push({ asunto: superada.Subject, id: superada.ID });
    sql("UPDATE subasta_lotes SET inicio=now()-interval '5 minutes', fin=now()-interval '1 second' WHERE id=" + lote.id);
    estado.relojAcelerado = true;
    await esperar(async () => (await pedir(subastas + '/' + lote.id)).lote.estado === 'ADJUDICADA');
    await abrir(ana, '/cuenta/ganadas');
    await ana.getByText('Aún no tienes adjudicaciones.', { exact: true }).waitFor();
    await abrir(luis, '/cuenta/ganadas');
    const premio = luis.getByRole('link', { name: 'Lote ' + lote.id, exact: true });
    await premio.waitFor();
    await luis.getByText(/^Pendiente ·/).waitFor();
    await captura(luis, 'ganadas');
    await luis.getByRole('button', { name: 'Confirmar pago simulado' }).click();
    await luis.getByText('Pago simulado confirmado', { exact: true }).waitFor();
    await captura(luis, 'ganadas-pagada');
    await premio.click();
    await luis.getByText('Pago simulado confirmado', { exact: true }).waitFor();
    const loginLuis = await obtenerLogin(dos);
    const orden = await pedir(subastas + '/' + lote.id + '/pago', {method:'POST',token:loginLuis.token});
    const repetida = await pedir(subastas + '/' + lote.id + '/pago', {method:'POST',token:loginLuis.token});
    assert.deepEqual(repetida,orden);
    estado.pagoIdempotente = true;
    estado.adjudicacion = orden.id;
    await captura(luis, 'adjudicada');
    await abrir(luis, '/cuenta/ganadas');
    await luis.getByText('Pago simulado confirmado', { exact: true }).waitFor();
    estado.ganadas = { visibleParaGanador: true, visibleParaPerdedor: false, pagoVisible: true };
    const ganada = await correo(dos.email, 'Subasta ganada');
    estado.correos.push({ asunto: ganada.Subject, id: ganada.ID });
    const bandeja = await browser.newPage();
    await bandeja.goto(mailpit + '/view/' + ganada.ID);
    await bandeja.getByText('Subasta ganada', { exact: true }).first().waitFor();
    await captura(bandeja, 'correo-ganada');
    await luis.goto('http://localhost:9094/swagger-ui/index.html');
    await luis.getByText('subastas-controller', { exact: true }).first().waitFor();
    await captura(luis, 'swagger');
    const activos = (await pedir(subastas + '?curados=true&tamano=48')).items
      .filter(item => item.estado === 'ACTIVA' && item.pujas === 0 && item.imagenes[0]?.startsWith('/mercado/vertice-')).slice(0,3);
    estado.lotesActivos = [];
    for (const activo of activos) {
      const viva = await pedir(subastas + '/' + activo.id + '/pujas',
        {method:'POST',token:loginLuis.token,body:{monto:activo.pujaMinima}});
      estado.lotesActivos.push({id:viva.id,titulo:viva.titulo,precio:viva.precioActual,pujas:viva.pujas});
    }
    estado.ok = true;
  } finally {
    await browser.close();
    fs.writeFileSync(process.env.TECHSTORE_SUBASTAS_RESULTADO || 'resultados/subastas-e2e.json', JSON.stringify(estado, null, 2));
  }
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
