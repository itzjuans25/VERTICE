import React from 'react';
import { Link } from 'react-router-dom';
import { catalogoApi } from '../api/catalogoApi';
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
