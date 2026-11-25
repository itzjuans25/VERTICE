import config from '../config';
import { request } from './client';

const base = '/api/ecommerce/subastas';
const call = (path, options) => request(config.SUBASTAS_URL, base + path, options);

export const subastasApi = {
  listar: query => call('', { query }),
  detalle: id => call('/' + id),
  crear: body => call('', { method: 'POST', body }),
  pujar: (id, monto) => call('/' + id + '/pujas', { method: 'POST', body: { monto } }),
  cancelar: id => call('/' + id + '/cancelar', { method: 'POST' }),
  mias: () => call('/mias'),
  pagar: id => call('/' + id + '/pago', { method: 'POST' }),
};