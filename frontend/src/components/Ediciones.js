import React, { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { catalogoApi } from '../api/catalogoApi';
import { subastasApi } from '../api/subastasApi';
import { Cierre, cop, EstadoMercado } from './MercadoUI';
import { useMercado } from './ExplorarMercado';

export function Lanzamiento({ pieza }) {
  const fecha = new Date(pieza.apertura);
  const pronto = fecha.getTime() > Date.now();
  return <article className="tt-card tarjeta-edicion">
    <Link className="tt-card-image" to={'/productos/' + pieza.productoId}>
      <img src={pieza.imagen} alt={pieza.titulo} width="640" height="800" loading="lazy" />
    </Link>
    <div className="tt-card-body">
      <span className="etiqueta-edicion">Edición limitada</span>
      <h3><Link to={'/productos/' + pieza.productoId}>{pieza.titulo}</Link></h3>
      <p>{pieza.relato}</p>
      <strong>{cop.format(pieza.precio)} COP</strong>
      <p>Quedan {pieza.restantes} de {pieza.total} · Máximo {pieza.maximoPorCliente} por cliente</p>
      <progress value={pieza.restantes} max={pieza.total}
        aria-label={'Unidades de ' + pieza.titulo + ' disponibles'} />
      {pronto && <Cierre lote={{ inicio: pieza.apertura, estado: 'PROGRAMADA' }} />}
    </div>
  </article>;
}

export function Lanzamientos() {
  const [estado, recargar] = useMercado(catalogoApi.lanzamientos);
  return <main className="auction-page">
    <h1>Lanzamientos</h1>
    <EstadoMercado estado={estado} reintentar={recargar} />
    <div className="ediciones-grid">{estado.datos?.map(pieza => <Lanzamiento key={pieza.productoId} pieza={pieza} />)}</div>
  </main>;
}

const estados = {
  EDICION_REGISTRADA: 'Edición registrada',
  PAGO_SIMULADO_REGISTRADO: 'Pago simulado registrado',
  ACTIVA: 'Subasta activa',
  PROGRAMADA: 'Subasta programada',
  CERRADA: 'Subasta cerrada',
  ADJUDICADA: 'Pieza adjudicada',
  CANCELADA: 'Subasta cancelada',
};

export function Verificador({ codigoInicial = '' }) {
  const [estado, cambiar] = useState({});
  async function verificar(evento) {
    evento.preventDefault();
    const codigo = String(new FormData(evento.currentTarget).get('certificado')).trim().toUpperCase();
    if (!/^VT-[SEC]-[A-Z0-9]{6,24}$/.test(codigo)) {
      cambiar({ error: 'Usa el código que aparece en la ficha o el comprobante.' });
      return;
    }
    cambiar({ cargando: true });
    try {
      const consultar = codigo.startsWith('VT-S-') ? subastasApi.certificado : catalogoApi.certificado;
      cambiar({ pieza: await consultar(codigo) });
    } catch (error) {
      cambiar({ error: error.status === 404 ? 'Código sin registro en VÉRTICE.' : 'No se pudo consultar el registro. Intenta de nuevo.' });
    }
  }
  return <section id="verificar-pieza" className="verificador">
    <div><h2>Verifica una pieza</h2><p>Consulta su registro y trazabilidad en la demostración académica.</p></div>
    <form onSubmit={verificar}>
      <label>Código de certificado<input name="certificado" defaultValue={codigoInicial} placeholder="VT-E-AUR68K" required maxLength={29} /></label>
      <button className="auction-btn primary" disabled={estado.cargando}>{estado.cargando ? 'Consultando…' : 'Verificar'}</button>
    </form>
    {estado.error && <p role="alert">{estado.error}</p>}
    {estado.pieza && <div role="status">
      <strong>Registro académico verificado</strong>
      <p>{estado.pieza.titulo} · {estado.pieza.edicion} · {estados[estado.pieza.estado]}</p>
      <p>{estado.pieza.codigo}{estado.pieza.loteId ? ' · Lote ' + estado.pieza.loteId : ''}</p>
      {(estado.pieza.fechaPagoSimulado || estado.pieza.fecha) &&
        <small>{new Date(estado.pieza.fechaPagoSimulado || estado.pieza.fecha).toLocaleDateString('es-CO')}</small>}
    </div>}
  </section>;
}

export function Certificados() {
  const [params] = useSearchParams();
  return <main className="auction-page"><h1>Certificados</h1><Verificador codigoInicial={params.get('codigo') || ''} /></main>;
}
