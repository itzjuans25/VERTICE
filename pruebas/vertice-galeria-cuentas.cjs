const assert = require('node:assert/strict');
const { randomBytes } = require('node:crypto');
const { writeFileSync } = require('node:fs');
const { setTimeout: esperar } = require('node:timers/promises');

const auth = 'http://localhost:9090/api/ecommerce/usuario';
const perfil = 'http://localhost:9090/api/auth/perfil';
const avisos = 'http://localhost:9093/api/notificaciones/mis-avisos';

async function json(url, { token, method = 'GET', body } = {}) {
  const headers = {};
  if (token) headers.Authorization = 'Bearer ' + token;
  if (body) headers['Content-Type'] = 'application/json';
  const r = await fetch(url, { method, headers, body: JSON.stringify(body), signal: AbortSignal.timeout(10000) });
  assert.ok(r.ok, `${url} HTTP ${r.status}`);
  return r.json();
}

async function registrar(nombre) {
  const email = 'galeria-jwt-' + randomBytes(7).toString('hex') + '@example.test';
  const password = 'QA-' + randomBytes(16).toString('hex');
  const { id } = await json(auth + '/save', { method: 'POST', body: { nombre, email, password } });
  const { token } = await json(auth + '/login', { method: 'POST', body: { email, password } });
  return { id, token };
}

async function aviso(token) {
  for (let i = 0; i < 20; i++) {
    const { items } = await json(avisos, { token });
    const reciente = items.find(item => item.asunto === 'Bienvenido a VÉRTICE');
    if (reciente) return { id: reciente.id, estado: reciente.estado };
    await esperar(500);
  }
  throw new Error('El aviso de registro no apareció.');
}

async function verificar() {
  const uno = await registrar('Galería uno');
  const dos = await registrar('Galería dos');
  const cambio = await json(perfil, { token: uno.token, method: 'PUT', body: { nombre: 'Galería uno editado' } });
  assert.equal(cambio.id, uno.id);
  const lecturaUno = await json(perfil, { token: uno.token });
  const lecturaDos = await json(perfil + '?usuarioId=' + uno.id, { token: dos.token });
  assert.equal(lecturaUno.nombre, 'Galería uno editado');
  assert.equal(lecturaDos.id, dos.id);
  assert.equal(lecturaDos.nombre, 'Galería dos');
  const ambos = await Promise.all([aviso(uno.token), aviso(dos.token)]);
  assert.notEqual(ambos[0].id, ambos[1].id);
  const sinJwt = await Promise.all([perfil, avisos].map(async url => (await fetch(url)).status));
  assert.deepEqual(sinJwt, [401, 401]);
  const evidencia = { perfiles: [uno.id, dos.id], avisos: ambos, sinJwt, aislamiento: true };
  const salida = process.env.VERTICE_CUENTAS_JSON || 'resultados/vertice-galeria-cuentas.json';
  writeFileSync(salida, JSON.stringify(evidencia, null, 2) + '\n');
  console.log(JSON.stringify(evidencia));
}

verificar().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
