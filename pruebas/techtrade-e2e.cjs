const assert = require('node:assert/strict');
const fs = require('node:fs');
const { randomBytes } = require('node:crypto');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');

const tienda = 'http://localhost:3000';
const catalogo = 'http://localhost:9092/api/ecommerce';
const mailpit = 'http://localhost:8025';
const marca = Date.now();
const vendedor = 'vendedor-tech-' + marca + '@example.test';
const comprador = 'comprador-tech-' + marca + '@example.test';
const titulo = 'Cámara web Full HD · Demo ' + marca;
const password = randomBytes(16).toString('hex');
const carpeta = 'docs/capturas';
const archivo = 'resultados/marketplace-techtrade.json';

async function consultar(url) {
  const respuesta = await fetch(url);
  assert.ok(respuesta.ok, url + ' devolvió ' + respuesta.status);
  return respuesta.json();
}

async function entrar(pagina, email, nombre, registrar = true) {
  if (registrar) {
    await pagina.goto(tienda + '/register');
    const telefono = (email === vendedor ? '301' : '302') + String(marca).slice(-7);
    for (const [campo, valor] of Object.entries({ nombre, email, telefono, edad: '30', password, 'confirm-password': password })) {
      await pagina.locator('#registro-' + campo).fill(valor);
    }
    await pagina.getByRole('button', { name: /Registrarse/i }).click();
    await pagina.waitForURL('**/login');
  } else {
    await pagina.goto(tienda + '/login');
  }
  await pagina.locator('#login-email').fill(email);
  await pagina.locator('#login-password').fill(password);
  await pagina.getByRole('button', { name: /Iniciar Sesión/i }).click();
  await pagina.waitForURL('**/productos');
}

async function main() {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const opciones = { viewport: { width: 1440, height: 940 }, reducedMotion: 'reduce' };
  const venta = await browser.newPage(opciones);
  const compra = await browser.newPage(opciones);
  const captura = (pagina, nombre) => pagina.screenshot({ path: carpeta + '/techtrade-' + nombre + '.png', animations: 'disabled' });
  try {
    for (const [ruta, etiqueta] of [['register', 'registro'], ['login', 'login']]) {
      await compra.goto(tienda + '/' + ruta);
      await compra.locator('form').waitFor();
      await captura(compra, etiqueta);
    }
    await compra.goto(tienda);
    await compra.locator('.tt-card img').first().evaluate(img => img.decode());
    await captura(compra, 'tienda');
    await compra.setViewportSize({ width: 390, height: 844 });
    assert.equal(await compra.evaluate(() => document.documentElement.scrollWidth), 390);
    await captura(compra, 'movil');
    await compra.setViewportSize(opciones.viewport);

    await entrar(venta, vendedor, 'Andrea Vendedora');
    await venta.goto(tienda + '/vender');
    for (const [campo, valor] of Object.entries({ titulo, precio: '87000', ciudad: 'Medellín', descripcion: 'Cámara USB con montaje para monitor.', marca: 'Genérica' })) {
      await venta.locator('[name=' + campo + ']').fill(valor);
    }
    await venta.getByLabel('Categoría').selectOption('Periféricos');
    await venta.getByLabel('Condición').selectOption('USADO');
    await venta.locator('input[type=file]').setInputFiles('pruebas/fixtures/marketplace.jpg');
    await captura(venta, 'formulario');
    const creada = venta.waitForResponse(r => r.url().endsWith('/publicaciones') && r.request().method() === 'POST');
    await venta.getByRole('button', { name: 'Publicar artículo' }).click();
    const publicacionHttp = await creada;
    assert.equal(publicacionHttp.status(), 201);
    const producto = await publicacionHttp.json();
    await venta.getByRole('button', { name: 'Pausar', exact: true }).click();
    await venta.getByRole('button', { name: 'Reactivar', exact: true }).waitFor();
    const buscar = catalogo + '/producto/explorar?q=' + encodeURIComponent(titulo);
    assert.equal((await consultar(buscar)).total, 0);
    await venta.getByRole('button', { name: 'Reactivar', exact: true }).click();
    await venta.getByRole('button', { name: 'Pausar', exact: true }).waitFor();

    await entrar(compra, comprador, 'Carlos Comprador');
    await compra.getByLabel('Buscar productos').fill(titulo);
    await compra.getByLabel('Vendedor').selectOption('PARTICULAR');
    await compra.getByRole('button', { name: 'Buscar', exact: true }).click();
    await compra.getByRole('heading', { name: titulo }).getByRole('link').click();
    await compra.locator('.tt-detail-image').evaluate(img => img.decode());
    await captura(compra, 'detalle');
    await compra.getByRole('button', { name: 'Agregar al carrito' }).click();
    await compra.getByRole('status').filter({ hasText: 'Producto agregado' }).waitFor();
    await compra.getByRole('link', { name: /Carrito/ }).click();
    await compra.getByRole('button', { name: 'Realizar Compra' }).waitFor();
    await captura(compra, 'carrito');
    compra.once('dialog', dialogo => dialogo.accept());
    const confirmada = compra.waitForResponse(r => r.url().endsWith('/compras/realizar'));
    await compra.getByRole('button', { name: 'Realizar Compra' }).click();
    const pedidoHttp = await confirmada;
    assert.equal(pedidoHttp.status(), 201);
    const pedido = await pedidoHttp.json();
    await compra.getByRole('heading', { name: 'Compra #' + pedido.id + ' confirmada' }).waitFor();
    await captura(compra, 'compra');
    await compra.getByRole('link', { name: 'Ver mis compras' }).click();
    await compra.getByRole('heading', { name: 'Compra #' + pedido.id }).waitFor();
    await captura(compra, 'historial');
    const final = await consultar(catalogo + '/producto/' + producto.id);
    assert.equal(final.estadoPublicacion, 'VENDIDA');
    assert.equal(final.stock, 0);

    const buscarCorreo = email => consultar(mailpit + '/api/v1/search?query=' + encodeURIComponent('to:' + email));
    let avisos;
    for (let i = 0; i < 25; i++) {
      const vb = (await buscarCorreo(vendedor)).messages;
      const cb = (await buscarCorreo(comprador)).messages;
      avisos = [vb.find(m => m.Subject.includes('Publicación activa #' + producto.id)),
        vb.find(m => /vent/i.test(m.Subject) && m.Subject.includes(String(pedido.id))),
        cb.find(m => /confirmaci/i.test(m.Subject) && m.Subject.includes(String(pedido.id)))];
      if (avisos.every(Boolean)) break;
      await new Promise(resolve => setTimeout(resolve, 900));
    }
    assert.ok(avisos.every(Boolean), 'Faltaron correos de publicación o compra');
    await venta.goto(mailpit + '/view/' + avisos[1].ID);
    await venta.getByText(avisos[1].Subject, { exact: false }).first().waitFor();
    await captura(venta, 'correo-vendedor');
    await compra.goto(mailpit + '/view/' + avisos[2].ID);
    await compra.getByText(avisos[2].Subject, { exact: false }).first().waitFor();
    await captura(compra, 'correo-comprador');
    const evidencia = { publicacion: producto.id, pedido: pedido.id, totalCop: pedido.total, estado: final.estadoPublicacion,
      stockFinal: final.stock, correos: avisos.map(m => ({ asunto: m.Subject, fecha: m.Created })) };
    fs.writeFileSync(archivo, JSON.stringify(evidencia, null, 2) + '\n');
    console.log(JSON.stringify(evidencia));
  } finally {
    await browser.close();
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
