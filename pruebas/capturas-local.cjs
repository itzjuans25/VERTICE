const assert = require('node:assert/strict');
const fs = require('node:fs');
const { execFileSync } = require('node:child_process');
const { randomBytes } = require('node:crypto');
const { pathToFileURL } = require('node:url');
const { resolve } = require('node:path');
const { chromium } = require(process.env.PLAYWRIGHT_PATH || 'playwright');

const base = 'http://localhost:3000';
const auth = 'http://localhost:9090/api/ecommerce/usuario';
const catalogo = 'http://localhost:9092/api/ecommerce';
const env = Object.fromEntries(fs.readFileSync('.env.native', 'utf8').trim().split(/\r?\n/).filter(line => /^\w+=/.test(line)).map(line => [line.split('=')[0], line.slice(line.indexOf('=') + 1)]));
const stamp = Date.now();
const email = 'captura-' + stamp + '@example.test';
const admin = 'captura-admin-' + stamp + '@example.test';
const password = randomBytes(16).toString('hex');
const phone = '300' + String(stamp).slice(-7);
const out = 'docs/capturas';
fs.mkdirSync(out, { recursive: true });

function psql(sql) {
  return execFileSync(process.env.PSQL_PATH || 'psql', ['-X', '-w', '-h', '127.0.0.1', '-p', env.POSTGRES_PORT,
    '-U', env.POSTGRES_USER, '-d', env.POSTGRES_DB, '-v', 'ON_ERROR_STOP=1', '-c', sql], {
    env: { ...process.env, PGPASSWORD: env.POSTGRES_PASSWORD, PGCLIENTENCODING: 'UTF8' },
    encoding: 'utf8', windowsHide: true,
  });
}

async function api(url, method = 'GET', body, token) {
  const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json',
    ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: JSON.stringify(body) });
  assert.ok(res.ok, method + ' ' + url + ' ' + res.status + ' ' + (res.ok ? '' : await res.text()));
  return res.json();
}

function escape(value) {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

async function main() {
  assert.equal(env.SMS_PROVIDER, 'log', 'La prueba usa SMS simulados');
  assert.ok(!env.MAIL_HOST || ['localhost', '127.0.0.1'].includes(env.MAIL_HOST), 'La prueba usa Mailpit local');
  const products = await api(catalogo + '/producto');
  assert.ok(products.filter(x => x.categoria && x.imagenUrl?.startsWith('/productos/')).length >= 20);
  const adminAccount = await api(auth + '/save', 'POST', { nombre: 'Administrador de demostración', email: admin, password });
  psql("UPDATE usuario SET role='ADMIN' WHERE id=" + Number(adminAccount.id));
  const adminSession = await api(auth + '/login', 'POST', { email: admin, password });
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 850 }, reducedMotion: 'reduce' });
  const capture = name => page.screenshot({ path: out + '/' + name + '.png', animations: 'disabled' });
  const consoleProof = async (name, title, evidence) => {
    await page.goto('about:blank');
    await page.setContent('<meta charset="utf-8"><main style="padding:32px"><h1>' + escape(title) + '</h1><pre style="white-space:pre-wrap;font-size:16px">' + escape(evidence) + '</pre></main>');
    await capture(name);
  };
  try {
    await page.goto(base + '/register', { waitUntil: 'domcontentloaded' });
    await page.locator('#registro-nombre').waitFor();
    await capture('02-registro');
    for (const [key, value] of Object.entries({ nombre: 'Cliente de demostración', email, telefono: phone, edad: '30', password, 'confirm-password': password })) {
      await page.locator('#registro-' + key).fill(value);
    }
    await page.getByRole('button', { name: /Registrarse/i }).click();
    await page.waitForURL('**/login');
    await page.locator('#login-email').waitFor();
    await capture('03-inicio-sesion');
    await page.locator('#login-email').fill(email);
    await page.locator('#login-password').fill(password);
    await page.getByRole('button', { name: /Iniciar Sesión/i }).click();
    await page.waitForURL('**/productos');
    await page.locator('.product-card img').first().waitFor();
    await page.locator('.product-card img').first().evaluate(image => image.decode());
    await capture('01-tienda-productos');
    await page.setViewportSize({ width: 390, height: 844 });
    await capture('01-tienda-movil');
    await page.setViewportSize({ width: 1280, height: 850 });
    await page.getByLabel('Categoría').selectOption(products[0].categoria);
    await page.getByLabel('Buscar productos').fill(products[0].nombre);
    assert.equal(await page.locator('.product-card').count(), 1);
    await page.getByLabel('Buscar productos').clear();
    await page.getByLabel('Categoría').selectOption('');
    await page.getByRole('button', { name: /^Agregar$/i }).first().click();
    await page.locator('.cart-badge').waitFor();
    await page.getByRole('link', { name: /Carrito/ }).click();
    await page.getByRole('button', { name: 'Realizar Compra' }).waitFor();
    await capture('04-carrito');
    page.once('dialog', dialog => dialog.accept());
    const compraHttp = page.waitForResponse(res => res.url().endsWith('/compras/realizar'));
    await page.getByRole('button', { name: 'Realizar Compra' }).click();
    const compraRes = await compraHttp;
    assert.equal(compraRes.status(), 201);
    const compra = await compraRes.json();
    await page.getByText(/Compra #.*confirmada/).waitFor();
    await capture('05-compra-confirmada');
    await page.getByRole('link', { name: 'Mis Compras' }).click();
    await page.getByRole('heading', { name: /^Compra #/ }).waitFor();
    await capture('06-historial');
    const enviada = await api(catalogo + '/compras/' + compra.id + '/estado', 'PATCH', { estado: 'ENVIADA' }, adminSession.token);
    assert.equal(enviada.estado, 'ENVIADA');
    let recibidos = [];
    for (let i = 0; i < 20; i++) {
      const data = await api('http://localhost:8025/api/v1/messages');
      recibidos = data.messages.filter(m => m.To.some(to => to.Address === email));
      if (recibidos.length >= 3) break;
      await new Promise(r => setTimeout(r, 750));
    }
    assert.ok(recibidos.some(m => /Bienvenido/.test(m.Subject)));
    assert.ok(recibidos.some(m => /Confirmacion de Compra/.test(m.Subject)));
    assert.ok(recibidos.some(m => /Estado del pedido/.test(m.Subject)));
    await page.goto('http://localhost:8025/');
    await page.getByText('Estado del pedido #' + compra.id, { exact: false }).first().waitFor();
    await capture('07-correo-mailpit');
    const sms = psql("SELECT id, mensaje FROM sms_simulado WHERE destinatario = '+57" + phone + "' ORDER BY id");
    assert.match(sms, /ENVIADA/);
    assert.match(sms, /3 rows|3 filas/);
    await consoleProof('08-sms-simulado', 'PostgreSQL · mensajes simulados de esta prueba', sms);
    await consoleProof('09-base-postgresql', 'PostgreSQL · tablas públicas', psql("SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename"));
    if (process.env.RABBIT_CTL) {
      const rabbitResult = execFileSync('powershell.exe', ['-NoProfile', '-Command',
        '& $env:RABBIT_CTL -n ecommerce_dev list_queues name messages consumers'],
      { encoding: 'utf8', windowsHide: true });
      await consoleProof('10-rabbitmq-colas', 'RabbitMQ · salida de rabbitmqctl', rabbitResult);
    }
    for (const [service, port] of Object.entries({ auth: 9090, catalogo: 9092, notificaciones: 9093 })) {
      await page.goto('http://localhost:' + port + '/swagger-ui.html');
      await page.locator('.opblock').first().waitFor();
      await capture('11-swagger-' + service);
    }
    for (const [service, path] of Object.entries({ auth: 'auth', catalogo: 'catalogo', notificaciones: 'notificaciones' })) {
      await page.goto(pathToFileURL(resolve(path, 'target/site/jacoco/index.html')).href);
      await capture('12-cobertura-' + service);
    }
    for (const name of ['carga', 'estres-6vu', 'estres']) {
      const { endpoints, ...summary } = JSON.parse(fs.readFileSync('resultados/' + name + '.json', 'utf8'));
      await consoleProof('13-k6-' + name, 'k6 · resultados medidos', JSON.stringify(summary, null, 2));
    }
    console.log('Compra ' + compra.id + ': tres correos en Mailpit y tres SMS simulados en PostgreSQL.');
  } finally {
    await browser.close();
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
