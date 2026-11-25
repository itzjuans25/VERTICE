import config from '../config';
import { request } from './client';

const base = '/api/ecommerce/subastas';
const call = (path, options) => request(config.SUBASTAS_URL, base + path, options);

export const subastasApi = {
  inicio: () => request(config.SUBASTAS_URL, '/api/subastas/inicio', { auth:false }),
  precioUsd: id => request(config.SUBASTAS_URL, '/api/subastas/lote/' + id + '/precio-usd', { auth:false }),
  entrega: () => request(config.SUBASTAS_URL, '/api/subastas/entrega', { auth:false, query:{ diasHabiles:3 } }),
  clima: () => request(config.SUBASTAS_URL, '/api/subastas/clima', { auth:false }),
  listar: query => call('', { query }),
  detalle: id => call('/' + id),
  crear: body => call('', { method: 'POST', body }),
  pujar: (id, monto) => call('/' + id + '/pujas', { method: 'POST', body: { monto } }),
  cancelar: id => call('/' + id + '/cancelar', { method: 'POST' }),
  mias: () => call('/mias', { requiereSesion: true }),
  pagar: id => call('/' + id + '/pago', { method: 'POST', requiereSesion: true }),
};
