import http from 'k6/http';
import { sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import exec from 'k6/execution';

const auth = (__ENV.AUTH_URL || 'http://127.0.0.1:9090') + '/api/ecommerce/usuario';
const catalogo = (__ENV.CATALOGO_URL || 'http://127.0.0.1:9092') + '/api/ecommerce';
const notificaciones = (__ENV.NOTIFICACIONES_URL || 'http://127.0.0.1:9093') + '/api/ecommerce/notificaciones';
const modo = __ENV.MODO || 'carga';
const maxVus = modo === 'estres' ? Number(__ENV.K6_VUS_ESTRES || 8) : 3;
const password = 'Solo-K6-Local-2026';
const otraClave = 'Solo-K6-Local-2026-b';
const prefijo = __ENV.K6_PREFIJO || ('k6-' + Date.now());
let registrarMetricas = true;
const correo = { tipo: 'EMAIL', email: { destinatario: 'carga@example.test', asunto: 'K6 E-Commerce', cuerpo: 'Mensaje de prueba local' } };
const operaciones = {
  'auth_registrar': ['POST', '/save', 201],
  'auth_login': ['POST', '/login', 200],
  'auth_perfil': ['GET', '/:id', 200],
  'auth_existe': ['GET', '/:id/existe', 200],
  'auth_actualizar': ['PUT', '/update', 200],
  'auth_eliminar': ['DELETE', '/:id', 204],
  'auth_sesion': ['GET', '/sesion', 200],
  'auth_evaluar': ['POST', '/evaluar-clave', 200],
  'auth_renovar': ['POST', '/renovar', 200],
  'auth_cambiar': ['POST', '/cambiar-clave', 204],
  'cat_productos': ['GET', '/producto', 200],
  'cat_producto': ['GET', '/producto/:id', 200],
  'cat_explorar': ['GET', '/producto/explorar?q=monitor&tamano=12&orden=precio-asc', 200],
  'cat_categorias': ['GET', '/producto/categorias', 200],
  'cat_marcas': ['GET', '/producto/marcas', 200],
  'cat_crear': ['POST', '/producto', 201],
  'cat_actualizar': ['PUT', '/producto/:id', 200],
  'cat_eliminar': ['DELETE', '/producto/:id', 204],
  'cat_precio': ['GET', '/producto/:id/precio-usd', 200],
  'cat_entrega': ['GET', '/producto/entrega?diasHabiles=3', 200],
  'cat_carrito': ['GET', '/carrito', 200],
  'cat_resumen': ['GET', '/carrito/resumen', 200],
  'cat_agregar': ['POST', '/carrito/agregar', 200],
  'cat_cantidad': ['PUT', '/carrito/actualizar/:id?cantidad=2', 200],
  'cat_quitar': ['DELETE', '/carrito/eliminar/:id', 200],
  'cat_vaciar': ['DELETE', '/carrito/vaciar', 204],
  'cat_comprar': ['POST', '/compras/realizar', 201],
  'cat_historial': ['GET', '/compras/usuario/:id', 200],
  'cat_compra': ['GET', '/compras/:id', 200],
  'cat_estado': ['PATCH', '/compras/:id/estado', 200],
  'cat_compras': ['GET', '/compras', 200],
  'notif_validar': ['POST', '/validar', 200],
  'notif_bienvenida': ['POST', '/previsualizar/bienvenida', 200],
  'notif_compra': ['POST', '/previsualizar/compra', 200],
  'notif_reintentos': ['GET', '/plan-reintentos', 200],
  'notif_encolar': ['POST', '/encolar', 202],
};

const latencias = new Trend('api_latencia', true);
const errores = new Rate('api_error');
const completadas = new Counter('flujos_completados');
const series = Object.fromEntries(Object.keys(operaciones).map(nombre => [nombre, {
  latencia: new Trend(nombre + '_ms', true),
  error: new Rate(nombre + '_error'),
  peticiones: new Counter(nombre + '_reqs'),
}]));

export const options = {
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
  thresholds: { api_latencia: ['max<500'], api_error: ['rate==0'], flujos_completados: ['count>0'] },
  scenarios: {
    tienda: modo === 'estres'
      ? { executor: 'ramping-vus', startVUs: 1, stages: [
          { duration: '8s', target: 3 }, { duration: '8s', target: maxVus },
          { duration: '16s', target: maxVus }, { duration: '4s', target: 0 },
        ], gracefulRampDown: '12s', gracefulStop: '12s' }
      : { executor: 'constant-vus', vus: maxVus, duration: '24s', gracefulStop: '12s' },
  },
};

function base(nombre) {
  if (nombre.startsWith('auth_')) return auth;
  if (nombre.startsWith('cat_')) return catalogo;
  return notificaciones;
}

function enviar(nombre, { id, body, token, clave, medir = registrarMetricas } = {}) {
  const [metodo, ruta, esperado] = operaciones[nombre];
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  if (clave) headers['X-Notification-Key'] = clave;
  const respuesta = http.request(metodo, base(nombre) + ruta.replace(':id', String(id)),
    body === undefined ? null : JSON.stringify(body), { headers, timeout: '8s', tags: { name: nombre } });
  const fallo = respuesta.status !== esperado;
  if (medir) {
    const latencia = respuesta.timings.duration;
    latencias.add(latencia);
    errores.add(fallo);
    series[nombre].latencia.add(latencia);
    series[nombre].error.add(fallo);
    series[nombre].peticiones.add(1);
  }
  if (fallo) throw new Error(nombre + ' devolvió ' + respuesta.status + ', se esperaba ' + esperado);
  return esperado === 204 ? undefined : respuesta.json();
}

function registro(email) {
  return { nombre: 'Cuenta k6', email, password };
}

export function setup() {
  registrarMetricas = false;
  const admin = enviar('auth_login', { body: { email: __ENV.K6_ADMIN_USER, password: __ENV.K6_ADMIN_PASSWORD }, medir: false });
  if (!__ENV.NOTIFICACIONES_API_KEY) throw new Error('Falta NOTIFICACIONES_API_KEY');
  const clientes = [];
  for (let i = 0; i < maxVus; i++) {
    const email = prefijo + '-vu' + i + '@example.test';
    const cuenta = enviar('auth_registrar', { body: registro(email), medir: false });
    clientes.push({ id: cuenta.id, email });
  }
  const producto = enviar('cat_crear', {
    token: admin.token,
    body: { nombre: prefijo + '-proveedores', descripcion: 'Preparación k6', precio: 12000, stock: 1 },
  });
  try {
    const tasa = enviar('cat_precio', { id: producto.id, medir: false });
    const entrega = enviar('cat_entrega', { medir: false });
    if (tasa.fuente !== 'Frankfurter' || entrega.fuente !== 'Nager.Date') throw new Error('Las integraciones externas aún no están listas');
  } finally {
    enviar('cat_eliminar', { token: admin.token, id: producto.id });
  }
  const email = prefijo + '-preflight@example.test';
  const previo = enviar('auth_registrar', { body: registro(email) });
  try {
    recorrer(admin.token, { id: previo.id, email }, prefijo + '-preflight', true);
  } finally {
    enviar('auth_eliminar', { token: admin.token, id: previo.id });
  }
  return { admin: admin.token, clientes, prefijo };
}

export default function ({ admin, clientes, prefijo }) {
  const cliente = clientes[exec.vu.idInTest - 1];
  const nombre = prefijo + '-' + exec.vu.idInTest + '-' + exec.vu.iterationInScenario;
  const mitad = exec.vu.iterationInScenario % 2 === 0;
  recorrer(admin, cliente, nombre, mitad);
  completadas.add(1);
  sleep(0.55);
}

function recorrer(admin, cliente, nombre, mitad) {
  const id = cliente.id;
  const nuevaCuenta = enviar('auth_registrar', { body: registro(nombre + '-alta@example.test') });
  const actual = mitad ? password : otraClave;
  const nueva = mitad ? otraClave : password;
  const { token } = enviar('auth_login', { body: { email: cliente.email, password: actual } });
  enviar('auth_perfil', { token, id });
  enviar('auth_existe', { id });
  enviar('auth_sesion', { token });
  enviar('auth_evaluar', { body: { password: nueva } });
  enviar('auth_renovar', { token, body: { password: actual } });
  enviar('auth_cambiar', { token, body: { actual, nueva } });
  enviar('auth_actualizar', { token, body: { id, nombre: 'Cliente k6' } });
  enviar('auth_eliminar', { token: admin, id: nuevaCuenta.id });

  const datosProducto = { nombre, descripcion: 'Producto efímero k6', precio: 12000, stock: 4 };
  const producto = enviar('cat_crear', { token: admin, body: datosProducto });
  enviar('cat_actualizar', { token: admin, id: producto.id, body: datosProducto });
  enviar('cat_productos');
  enviar('cat_producto', { id: producto.id });
  enviar('cat_explorar');
  enviar('cat_categorias');
  enviar('cat_marcas');
  enviar('cat_precio', { id: producto.id });
  enviar('cat_entrega');
  enviar('cat_carrito', { token });
  enviar('cat_agregar', { token, body: { productoId: producto.id, cantidad: 1 } });
  enviar('cat_cantidad', { token, id: producto.id });
  enviar('cat_quitar', { token, id: producto.id });
  enviar('cat_agregar', { token, body: { productoId: producto.id, cantidad: 2 } });
  enviar('cat_resumen', { token });
  const compra = enviar('cat_comprar', { token });
  enviar('cat_estado', { token: admin, id: compra.id, body: { estado: 'ENVIADA' } });
  enviar('cat_compra', { token, id: compra.id });
  enviar('cat_historial', { token, id });
  enviar('cat_compras', { token: admin });
  enviar('cat_vaciar', { token });
  enviar('cat_eliminar', { token: admin, id: producto.id });

  enviar('notif_validar', { body: correo });
  enviar('notif_bienvenida', { body: { nombre: 'Cliente k6' } });
  enviar('notif_compra', { body: { nombre: 'Cliente k6', orden: compra.id, totalCop: compra.total } });
  enviar('notif_reintentos');
  enviar('notif_encolar', { clave: __ENV.NOTIFICACIONES_API_KEY, body: correo });
}

export function teardown({ admin, clientes, prefijo }) {
  for (const { id } of clientes) enviar('auth_eliminar', { token: admin, id, medir: false });
  for (const producto of enviar('cat_productos', { medir: false })) {
    if (producto.nombre.startsWith(prefijo + '-')) {
      enviar('cat_eliminar', { token: admin, id: producto.id, medir: false });
    }
  }
}

export function handleSummary(datos) {
  const metrica = (nombre) => datos.metrics[nombre]?.values || {};
  const endpoints = Object.entries(operaciones).map(([nombre, [metodo, ruta]]) => ({
    nombre, metodo, ruta, ...metrica(nombre + '_ms'),
    errores: metrica(nombre + '_error').passes || 0,
    peticiones: metrica(nombre + '_reqs').count || 0,
    rps: metrica(nombre + '_reqs').rate || 0,
  }));
  const summary = {
    modo,
    prefijoPrueba: prefijo,
    vusMaximos: maxVus,
    duracionSegundos: datos.state.testRunDurationMs / 1000,
    ...metrica('api_latencia'),
    errores: metrica('api_error').passes || 0,
    peticiones: endpoints.reduce((sum, endpoint) => sum + endpoint.peticiones, 0),
    rps: endpoints.reduce((sum, endpoint) => sum + endpoint.rps, 0),
    flujosCompletados: metrica('flujos_completados').count || 0,
    endpoints,
  };
  return {
    [__ENV.RESULTADO || ('resultados/' + modo + '.json')]: JSON.stringify(summary, null, 2) + '\n',
    stdout: JSON.stringify({ ...summary, endpoints: undefined }, null, 2) + '\n',
  };
}
