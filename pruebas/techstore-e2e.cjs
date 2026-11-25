const assert = require('node:assert/strict');
const fs = require('node:fs');
const { randomBytes } = require('node:crypto');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');

const tienda = 'http://localhost:3000';
const catalogo = 'http://localhost:9092/api/ecommerce/producto';
const mailpit = 'http://localhost:8025';
const stamp = Date.now();
const email = 'captura-techstore-' + stamp + '@example.test';
const password = randomBytes(16).toString('hex');
const nueva = 'Nueva-' + randomBytes(16).toString('hex');
const carpeta = 'docs/capturas';

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 940 }, reducedMotion: 'reduce' });
  const prefijo = process.env.TECHSTORE_E2E_CAPTURAS || 'techstore-';
  const captura = nombre => page.screenshot({ path: carpeta + '/' + prefijo + nombre + '.png', animations: 'disabled' });
  try {
    const primeraPagina = await (await fetch(catalogo + '/explorar?tamano=48')).json();
    const productos = [...primeraPagina.items];
    for (let pagina = 1; pagina < primeraPagina.paginas; pagina++) {
      const siguiente = await (await fetch(catalogo + '/explorar?tamano=48&pagina=' + pagina)).json();
      productos.push(...siguiente.items);
    }
    assert.equal(productos.length, primeraPagina.total);
    assert.ok(productos.length >= 45);
    for (const p of productos) {
      assert.ok(p.descripcion?.trim() && p.descripcionLarga?.trim() && Object.keys(p.especificaciones).length, p.nombre);
      assert.ok(p.precio > 0 && p.stock >= 0 && Number.isInteger(p.garantiaDias) && p.garantiaDias >= 0, p.nombre);
      assert.ok(p.condicion === 'NUEVO' ? !p.grado : p.condicion === 'USADO' && ['A','B','C'].includes(p.grado), p.nombre);
      assert.ok(p.imagenUrl, p.nombre);
      for (const foto of [p.imagenUrl,p.imagenSecundariaUrl].filter(Boolean)) {
        assert.match(foto, /^\/(techtrade|mercado)\//);
        const respuesta = await fetch(tienda + foto, { method: 'HEAD' });
        assert.ok(respuesta.ok && respuesta.headers.get('content-type')?.startsWith('image/'), foto);
      }
    }
    const interno = process.env.TECHSTORE_E2E_PRODUCTO;
    if (interno) assert.ok(productos.every(p => String(p.id) !== interno), 'El fixture apareció en el catálogo público');
    const candidato = interno
      ? await (await fetch(catalogo + '/' + interno)).json()
      : productos.find(p => p.modelo === 'ThinkPad T480' && p.stock > 0)
        || productos.find(p => p.stock > 0 && p.condicion === 'USADO' && p.imagenSecundariaUrl);
    assert.ok(candidato);
    await page.goto(tienda + '/productos');
    await page.locator('.tt-card img').first().evaluate(img => img.decode());
    assert.equal(await page.getByRole('link', { name: 'Vender', exact: true }).count(), 0);
    await captura('tienda');
    await page.setViewportSize({ width: 390, height: 844 });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), 390);
    await captura('movil');
    await page.setViewportSize({ width: 768, height: 950 });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), 768);
    await captura('tablet');
    await page.setViewportSize({ width: 1440, height: 940 });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth), 1440);
    const favicon = await page.locator('link[rel="icon"]').getAttribute('href');
    assert.ok((await fetch(new URL(favicon, tienda))).headers.get('content-type').startsWith('image/'));
    const buscador = page.getByLabel('Buscar productos');
    await buscador.focus();
    assert.equal(await buscador.evaluate(e => getComputedStyle(e).outlineStyle), 'solid');
    const contraste = await buscador.evaluate(e => {
      const luminancia = color => color.match(/\d+(?:\.\d+)?/g).slice(0,3)
        .map(Number).map(n => n / 255).map(n => n <= 0.04045 ? n / 12.92 : ((n + 0.055) / 1.055) ** 2.4)
        .reduce((total, n, i) => total + n * [0.2126,0.7152,0.0722][i], 0);
      const css = getComputedStyle(e);
      const valores = [luminancia(css.color),luminancia(css.backgroundColor)].sort((a,b) => b-a);
      return (valores[0] + 0.05) / (valores[1] + 0.05);
    });
    assert.ok(contraste >= 4.5);

    await page.goto(tienda + '/register');
    await page.locator('#contenido form').waitFor();
    await captura('registro');
    const campos = { nombre: 'Comprador de demostración', email, telefono: '303' + String(stamp).slice(-7),
      edad: '30', password, 'confirm-password': password };
    for (const [campo, valor] of Object.entries(campos)) {
      await page.locator('#registro-' + campo).fill(valor);
    }
    await page.getByRole('button', { name: /Registrarse/i }).click();
    await page.waitForURL('**/login');
    await captura('login');
    await page.locator('#login-email').fill(email);
    await page.locator('#login-password').fill(password);
    await page.getByRole('button', { name: /Iniciar Sesión/i }).click();
    await page.waitForURL('**/subastas');

    if (interno) {
      await page.goto(tienda + '/productos/' + candidato.id);
    } else {
      await page.goto(tienda + '/productos');
      await page.locator('select[name="marca"]').selectOption(candidato.marca);
      await page.getByLabel('Condición').selectOption('USADO');
      await page.getByLabel('Buscar productos').fill(candidato.nombre);
      await page.getByRole('button', { name: 'Buscar', exact: true }).click();
      await page.getByRole('heading', { name: candidato.nombre, exact: true }).getByRole('link').click();
    }
    await page.locator('.tt-detail-image').evaluate(img => img.decode());
    await page.getByRole('button', { name: 'Ver foto 2' }).click();
    await page.locator('.tt-detail-image').evaluate(img => img.decode());
    await page.getByText('Especificaciones técnicas').waitFor();
    await page.getByText(/Frankfurter · Tasa/).waitFor();
    assert.equal(await page.title(), candidato.nombre + ' | VÉRTICE');
    await captura('detalle');

    await page.getByRole('button', { name: 'Agregar al carrito' }).click();
    await page.getByRole('status').filter({ hasText: 'Producto agregado' }).waitFor();
    await page.getByRole('link', { name: /Carrito/ }).click();
    await page.getByRole('button', { name: 'Realizar Compra' }).waitFor();
    const entrega = page.getByText(/\d+ días hábiles\./);
    await entrega.waitFor();
    const calendario = await entrega.innerText();
    assert.match(calendario, /Nager.Date|Calendario parcial/);
    await captura('carrito');
    page.once('dialog', dialogo => dialogo.accept());
    const respuesta = page.waitForResponse(r => r.url().endsWith('/compras/realizar'));
    await page.getByRole('button', { name: 'Realizar Compra' }).click();
    const http = await respuesta;
    assert.equal(http.status(), 201);
    const pedido = await http.json();
    const inventario = await (await fetch(catalogo + '/' + candidato.id)).json();
    assert.equal(inventario.stock, candidato.stock - 1);
    await page.getByRole('heading', { name: 'Compra #' + pedido.id + ' confirmada' }).waitFor();
    await captura('compra');
    await page.getByRole('link', { name: 'Ver mis compras' }).click();
    await page.getByRole('heading', { name: 'Compra #' + pedido.id }).waitFor();
    await captura('historial');
    const compraActual = page.locator('.order-card').filter({ has: page.getByRole('heading', { name: 'Compra #' + pedido.id, exact: true }) });
    await compraActual.getByText(/^Pendiente ·/).waitFor();
    const esperarPago = page.waitForResponse(r => r.url().endsWith('/compras/' + pedido.id + '/pago'));
    await compraActual.getByRole('button', { name: 'Confirmar pago simulado' }).click();
    const pagoHTTP = await esperarPago;
    assert.equal(pagoHTTP.status(), 200);
    const pagada = (await pagoHTTP.json()).pagoSimulado;
    assert.equal(pagada.referencia, 'SIM-COMPRA-' + pedido.id);
    assert.ok(pagada.pagadoEn);
    const repetida = await page.evaluate(async id => {
      const { token } = JSON.parse(localStorage.getItem('ecommerce.session'));
      const r = await fetch('http://localhost:9092/api/ecommerce/compras/' + id + '/pago',
        { method: 'POST', headers: { Authorization: 'Bearer ' + token } });
      return { codigo: r.status, pago: (await r.json()).pagoSimulado };
    }, pedido.id);
    assert.deepEqual(repetida, { codigo: 200, pago: pagada });
    await compraActual.getByText('Pago simulado confirmado', { exact: true }).waitFor();
    await captura('pago');
    assert.doesNotMatch(await page.locator('body').innerText(), /\b(undefined|NaN)\b/);

    await page.locator('.mercado-cuenta summary').click();
    await page.getByRole('link', { name: 'Mi cuenta', exact: true }).click();
    await page.getByRole('link', { name: 'Seguridad', exact: true }).click();
    await page.locator('#clave-actual').fill(password);
    await page.locator('#clave-nueva').fill(nueva);
    await page.locator('#clave-confirmacion').fill(nueva);
    const cambio = page.waitForResponse(r => r.url().endsWith('/cambiar-clave') && r.request().method() === 'POST');
    await page.getByRole('button', { name: 'Guardar contraseña' }).click();
    assert.equal((await cambio).status(), 204);
    await page.getByRole('status').filter({ hasText: 'Contraseña actualizada' }).waitFor();
    await page.locator('#login-email').fill(email);
    await page.locator('#login-password').fill(password);
    await page.getByRole('button', { name: /Iniciar Sesión/i }).click();
    await page.getByRole('alert').waitFor();
    await page.locator('#login-password').fill(nueva);
    await page.getByRole('button', { name: /Iniciar Sesión/i }).click();
    await page.waitForURL('**/subastas');
    await page.locator('.mercado-cuenta summary').click();
    await page.getByRole('button', { name: /Cerrar sesión/i }).click();
    await page.goto(tienda + '/compras');
    await page.waitForURL('**/login');

    let mensaje;
    for (let i = 0; i < 25; i++) {
      const res = await fetch(mailpit + '/api/v1/search?query=' + encodeURIComponent('to:' + email));
      assert.ok(res.ok);
      const datos = await res.json();
      mensaje = datos.messages.find(m => m.Subject === 'Confirmacion de Compra #' + pedido.id + ' · VÉRTICE');
      if (mensaje) break;
      await new Promise(resolve => setTimeout(resolve, 700));
    }
    assert.ok(mensaje, 'No se recibió el correo de compra');
    await page.goto(mailpit + '/view/' + mensaje.ID);
    await page.getByText(mensaje.Subject, { exact: false }).first().waitFor();
    await captura('correo');
    const resultado = { pedido: pedido.id, totalCop: pedido.total, estado: pedido.estado, correo: mensaje.Subject,
      fixtureFueraDelCatalogo: Boolean(interno),
      pagoSimulado: pagada, pagoIdempotente: true,
      fechaCorreo: mensaje.Created, anchosVerificados: [390,768,1440], productosVerificados: productos.length,
      producto: pedido.items[0].nombreProducto, cambioClaveYReingreso: true, cierreYRutaProtegida: true,
      calendario, stockAntes: candidato.stock, stockDespues: inventario.stock,
      contrasteBuscador: Number(contraste.toFixed(2)) };
    fs.writeFileSync(process.env.TECHSTORE_E2E_RESULTADO || 'resultados/e2e-techstore.json',
      JSON.stringify(resultado, null, 2) + '\n');
    console.log(JSON.stringify(resultado));
  } finally {
    await browser.close();
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
