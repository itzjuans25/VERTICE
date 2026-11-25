import config from '../config';
import { clearSession, getAccessToken } from '../session';

export const SESSION_EXPIRED_EVENT = 'session:expired';
export const SERVIDOR_LENTO_EVENT = 'servidor:lento';
let solicitudesLentas = 0;
let calentado = false;

function expirarSesion() {
  clearSession();
  if (typeof window !== 'undefined') window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
}

function publicarDemora(cambio) {
  solicitudesLentas += cambio;
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(SERVIDOR_LENTO_EVENT,{ detail:solicitudesLentas > 0 }));
  }
}

export function despertarServicios() {
  if (calentado) return;
  calentado = true;
  [config.AUTH_URL,config.CATALOGO_URL,config.SUBASTAS_URL,config.NOTIFICACIONES_URL].forEach(base => {
    fetch(base + '/health',{ method:'GET', mode:'no-cors', cache:'no-store' }).catch(() => {});
  });
}

export class ApiError extends Error {
  constructor(message, { status = 0, payload = null } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

function appendQuery(path, query) {
  if (!query) {
    return path;
  }

  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.append(key, value);
    }
  });

  const serialized = params.toString();
  return serialized ? `${path}?${serialized}` : path;
}

function extractMessage(payload, status) {
  if (typeof payload === 'string' && payload.trim()) {
    return payload;
  }

  if (payload && typeof payload === 'object') {
    if (payload.message) {
      return payload.message;
    }

    if (payload.fieldErrors && typeof payload.fieldErrors === 'object') {
      const errores = Object.values(payload.fieldErrors).filter(Boolean);
      if (errores.length > 0) {
        return errores.join('. ');
      }
    }

    if (payload.error) {
      return payload.error;
    }
  }

  if (status === 401) {
    return 'Tu sesion expiro. Inicia sesion nuevamente.';
  }
  if (status === 403) {
    return 'No tienes permisos para realizar esta accion.';
  }
  if (status === 404) {
    return 'El recurso solicitado no existe.';
  }
  if (status >= 500) {
    return 'El servidor no esta disponible. Intenta de nuevo mas tarde.';
  }
  return 'No se pudo completar la solicitud.';
}

async function parseResponse(response) {
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      return await response.json();
    } catch (error) {
      return null;
    }
  }
  return response.text();
}

/**
 * Cliente HTTP comun para auth y catalogo.
 *
 * - Antepone la URL base correcta.
 * - Adjunta el JWT cuando existe.
 * - Convierte cualquier fallo en ApiError con un mensaje util.
 * - Si el backend responde 401 con un token enviado, cierra la sesion y
 *   emite SESSION_EXPIRED_EVENT para que App redirija al login.
 */
export async function request(baseUrl, path, options = {}) {
  const {
    method = 'GET',
    body,
    query,
    auth = true,
    requiereSesion = false,
    timeoutMs = config.REQUEST_TIMEOUT_MS,
  } = options;

  const url = `${baseUrl}${appendQuery(path, query)}`;
  const headers = { Accept: 'application/json' };

  const multipart = body instanceof FormData;
  if (body !== undefined && !multipart) {
    headers['Content-Type'] = 'application/json';
  }

  const token = auth ? getAccessToken() : null;
  if (requiereSesion && !token) {
    expirarSesion();
    throw new ApiError('Inicia sesión para continuar.', { status: 401 });
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let lenta = false;
  const indicador = setTimeout(() => { lenta = true; publicarDemora(1); },3000);
  const segura = ['GET','HEAD'].includes(method.toUpperCase());
  try {
    for (let intento=0;intento<=Number(segura);intento++) {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(),timeoutMs);
      let response;
      try {
        response = await fetch(url,{
          method, headers, body:multipart ? body : body !== undefined ? JSON.stringify(body) : undefined,
          signal:controller.signal,
        });
      } catch (error) {
        if (error.name === 'AbortError' && segura && intento === 0) continue;
        throw new ApiError(error.name === 'AbortError' ? 'La solicitud tardó demasiado. Intenta de nuevo.'
          : 'No se pudo conectar con el servidor.');
      } finally {
        clearTimeout(timeoutId);
      }
      if (response.status === 503 && segura && intento === 0) continue;
      const payload = await parseResponse(response);
      if (!response.ok) {
        if (response.status === 401 && token) {
          expirarSesion();
        }
        throw new ApiError(extractMessage(payload,response.status),{ status:response.status, payload });
      }
      return payload;
    }
  } finally {
    clearTimeout(indicador);
    if (lenta) publicarDemora(-1);
  }
}
