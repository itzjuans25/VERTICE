import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { imagenProducto } from '../api/catalogoApi';
import { nombresCategorias } from '../categorias';
import { pesos } from '../marca';

export const cop = pesos;
export const tituloDelLote = titulo => titulo.replace(/^Ensayo curado\s*[·–-]\s*(.+?)(?: 1[0-9]{12})?$/i, '$1');
const estados = { ACTIVA: 'Activa', PROGRAMADA: 'Programada', CERRADA: 'Cerrada', ADJUDICADA: 'Adjudicada', CANCELADA: 'Cancelada' };

export function Cierre({ lote }) {
  const [ahora, setAhora] = useState(Date.now);
  const programada = lote.estado === 'PROGRAMADA';
  const vigente = ['ACTIVA', 'PROGRAMADA'].includes(lote.estado);
  const segundo = Math.max(0, Math.ceil((Date.parse(programada ? lote.inicio : lote.fin) - ahora) / 1000));
  useEffect(() => {
    if (!vigente) return undefined;
    const timer = setInterval(() => {
      if (document.visibilityState !== 'hidden') setAhora(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, [vigente]);
  if (!vigente || !segundo) return <span className="auction-chip">{!vigente ? estados[lote.estado] : programada ? 'Por iniciar' : 'Plazo terminado'}</span>;
  const horas = Math.floor(segundo / 3600);
  const minutos = Math.floor(segundo / 60) % 60;
  const segundos = String(segundo % 60).padStart(2, '0');
  const color = segundo < 3600 ? 'rojo' : segundo < 86400 ? 'ambar' : '';
  return <span className={'auction-chip ' + color}>{programada ? 'Inicia en ' : 'Cierra en '}{horas ? horas + ' h ' : ''}{minutos} min {segundos} s</span>;
}

export function TarjetaMercado({ lote, producto, agregar, ocupado = false }) {
  const item = lote || producto;
  const nombre = lote ? tituloDelLote(lote.titulo) : producto.nombre;
  const enlace = (lote ? '/subastas/' : '/productos/') + item.id;
  const condicion = item.condicion === 'USADO' ? 'Usado' : 'Nuevo';
  const precio = lote?.precioActual ?? producto?.precio;
  const ficha = lote ? nombresCategorias[item.categoria] : producto.categoria;
  return <article className={'tt-card product-card mercado-card ' + (lote ? 'tarjeta-lote' : 'tarjeta-producto')}>
    <Link className="tt-card-image" to={enlace} tabIndex={-1} aria-hidden="true">
      <img src={lote ? lote.imagenes[0] : imagenProducto(producto.imagenUrl,producto.nombre)} alt={'Vista de ' + nombre} width="424" height="318" loading="lazy" />
    </Link>
    <div className="tt-card-body">
      <div className="tt-etiquetas">{lote && <span>Pieza única</span>}<span>{condicion}</span>{producto?.grado && <span>Grado {producto.grado}</span>}</div>
      <h3><Link to={enlace}>{nombre}</Link></h3>
      <p className="tt-detalle-menor">{ficha}{producto?.garantiaDias > 0 ? ' · ' + producto.garantiaDias + ' días de garantía' : ''}</p>
      <div className="tt-card-footer">
        <strong>{cop.format(precio)}</strong>
        {lote ? <span>{lote.pujas} {lote.pujas === 1 ? 'puja' : 'pujas'}</span> : agregar &&
          <button className="tt-button tt-secondary" aria-label={(producto.stock > 0 ? 'Agregar ' : 'Agotado: ') + nombre}
            disabled={ocupado || producto.stock < 1} onClick={() => agregar(producto)}>{producto.stock > 0 ? 'Agregar' : 'Agotado'}</button>}
      </div>
      {lote && <div className="auction-tarjeta-datos">
        <span>{lote.pujas ? 'Oferta líder' : 'Salida'} · {estados[lote.estado]}</span>
        <Cierre lote={lote} />
        <span>Incremento {cop.format(lote.incremento)}{lote.estado === 'ACTIVA' && ' · +2 min al pujar en los últimos 2 min'}</span>
        <span>{lote.reservaAlcanzada ? 'Reserva alcanzada' : 'Reserva no alcanzada'}</span>
        <p><strong>Por qué se subasta</strong><br />{lote.atributos?.['Por qué se subasta'] || 'Consulta el contexto en la ficha.'}</p>
        {['CARROS', 'MOTOS'].includes(lote.categoria) && <span>Demostración académica · Sin traspaso real</span>}
      </div>}
    </div>
  </article>;
}

export function FiltrosMercado({ valores = {}, categorias, marcas, lote = false, alBuscar, alLimpiar }) {
  return <form className={'tt-filtros mercado-filtros' + (lote ? ' mercado-filtros-lote' : '')} onSubmit={e => {
    e.preventDefault();
    alBuscar(Object.fromEntries(new FormData(e.currentTarget)));
  }}>
    <label className="tt-busqueda">{lote ? 'Buscar lotes' : 'Buscar productos'}
      <input name="q" type="search" defaultValue={valores.q} maxLength={lote ? 100 : 120} />
    </label>
    <label>Categoría<select name="categoria" defaultValue={valores.categoria || ''}><option value="">Todas</option>
      {categorias.map(([valor, nombre]) => <option key={valor} value={valor}>{nombre}</option>)}</select></label>
    <label>Condición<select name="condicion" defaultValue={valores.condicion || ''}><option value="">Todas</option>
      <option value="NUEVO">Nuevo</option><option value="USADO">Usado</option></select></label>
    {marcas && <label>Marca<select name="marca" defaultValue={valores.marca || ''}><option value="">Todas</option>
      {marcas.map(marca => <option key={marca}>{marca}</option>)}</select></label>}
    <label>Desde (COP)<input name="minimo" type="number" min="0" defaultValue={valores.minimo} /></label>
    <label>Hasta (COP)<input name="maximo" type="number" min="0" defaultValue={valores.maximo} /></label>
    {!lote && <label>Orden<select name="orden" defaultValue={valores.orden || 'recientes'}>
      <option value="recientes">Más recientes</option><option value="precio-asc">Menor precio</option><option value="precio-desc">Mayor precio</option>
      <option value="relevancia">Mejor disponibilidad</option>
    </select></label>}
    <div className="mercado-filtro-acciones"><button className="tt-button" type="submit">Buscar</button>
      {alLimpiar && <button type="button" className="tt-button tt-secondary" onClick={alLimpiar}>Limpiar</button>}</div>
  </form>;
}

export function Paginacion({ pagina, paginas, cambiar }) {
  if (paginas <= 1) return null;
  return <nav className="tt-paginas" aria-label="Páginas de resultados">
    <button className="tt-button tt-secondary" disabled={pagina === 0} onClick={() => cambiar(pagina - 1)}>Anterior</button>
    <span>Página {pagina + 1} de {paginas}</span>
    <button className="tt-button tt-secondary" disabled={pagina + 1 >= paginas} onClick={() => cambiar(pagina + 1)}>Siguiente</button>
  </nav>;
}

export function EstadoMercado({ estado, reintentar, vacio, accion, enlace = '/' }) {
  if (estado.cargando) return <div className="tt-grid" role="status" aria-label="Cargando resultados">{[1, 2, 3].map(i => <div className="tt-skeleton" key={i} aria-hidden="true" />)}</div>;
  if (estado.error) return <div className="tt-vacio" role="alert"><p>{estado.error}</p><button className="tt-button" onClick={reintentar}>Reintentar</button></div>;
  if (vacio) return <div className="tt-vacio" role="status"><p>{vacio}</p>{accion && <Link to={enlace}>{accion}</Link>}</div>;
  return null;
}
