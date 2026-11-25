import { tituloMarca } from '../marca';
import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { subastasApi as api } from '../api/subastasApi';
import './Subastas.css';
import { categoriasSubastas, nombresCategorias as categorias, categoriaLote, categoriaPorSlug, vitrinas } from '../categorias';
import { Cierre, cop, tituloDelLote, TarjetaMercado, FiltrosMercado, Paginacion, EstadoMercado } from './MercadoUI';
import PagoSimulado from './PagoSimulado';
import { catalogoApi } from '../api/catalogoApi';
import { useMercado } from './ExplorarMercado';
import { Lanzamiento } from './Ediciones';


const estados = { ACTIVA: 'Activa', PROGRAMADA: 'Programada', CERRADA: 'Cerrada', ADJUDICADA: 'Adjudicada', CANCELADA: 'Cancelada' };
const img = categoria => categoriaLote(categoria)?.imagen || '/subastas/electronica.webp';
const fecha = valor => new Date(valor).toLocaleString('es-CO');
const usd = new Intl.NumberFormat('es-CO', { style:'currency', currency:'USD', currencyDisplay:'code' });

function EntregaLote({ estado }) {
  if (estado.cargando) return null;
  const data = estado.datos;
  return <p className="auction-muted">{data
    ? data.fechaEstimada + ' · ' + (data.incluyeFestivos ? 'Nager.Date' : 'calendario parcial')
    : 'Referencia de entrega no disponible.'}</p>;
}

function useConsulta(consulta, sondeo = false) {
  const [estado, actualizar] = useState({ datos: null, error: '', cargando: true });
  const [revision, reintentar] = useState(0);
  const recargar = useCallback(() => reintentar(v => v + 1), []);

  useEffect(() => {
    let activo = true;
    let ocupado = false;
    actualizar({ datos: null, error: '', cargando: true });

    async function consultar() {
      if (ocupado || document.visibilityState === 'hidden') return;
      ocupado = true;
      try {
        const datos = await consulta();
        if (activo) actualizar({ datos, error: '', cargando: false });
      } catch (error) {
        if (activo) actualizar(anterior => ({ ...anterior, error: error.message, cargando: false }));
      } finally {
        ocupado = false;
      }
    }

    consultar();
    const timer = sondeo && window.setInterval(consultar, 5000);
    document.addEventListener('visibilitychange', consultar);
    return () => {
      activo = false;
      if (timer) window.clearInterval(timer);
      document.removeEventListener('visibilitychange', consultar);
    };
  }, [consulta, revision, sondeo]);

  return { ...estado, recargar };
}

function Titulo({ children }) {
  useEffect(() => {
    document.title = tituloMarca(children);
    return () => { document.title = tituloMarca(); };
  }, [children]);
  return <h1>{children}</h1>;
}

function EstadoConsulta({ consulta }) { return <EstadoMercado estado={consulta} reintentar={consulta.recargar} />; }

export function Inicio() {
  const consulta = useConsulta(api.inicio);
  const [ediciones, recargar] = useMercado(catalogoApi.lanzamientos);
  const siguiente = ediciones.datos?.find(pieza => Date.parse(pieza.apertura) > Date.now());
  const conteos = new Map(consulta.datos?.categorias.map(({ categoria, total }) => [categoria, total]));

  return <main id="principal" className="auction-page mercado-inicio inicio-editorial">
    <section className="auction-hero">
      <div>
        <Titulo>Piezas que no se repiten.</Titulo>
        <p>Objetos de autor, tiradas cortas y piezas que encontraron la forma de perdurar.</p>
        <div className="auction-row">
          <Link className="auction-btn primary" to="/subastas">Explorar subastas</Link>
          <Link className="auction-btn" to="/lanzamientos">Ver lanzamientos</Link>
        </div>
        {siguiente && <div className="hero-apertura"><strong>Próximo lanzamiento · {siguiente.titulo}</strong>
          <Cierre lote={{ inicio: siguiente.apertura, estado: 'PROGRAMADA' }} />
        </div>}
      </div>
      <figure className="hero-pieza">
        <img src="/mercado/edicion-aureola.webp" alt="Reloj de esfera marfil y correa burdeos sobre papel de taller" width="640" height="800" />
        <figcaption>Serie · N.º 037/120</figcaption>
      </figure>
    </section>
    <section id="inicio-subastas"><h2>Subastas que terminan pronto</h2>
      <EstadoConsulta consulta={consulta} />
      <div className="tt-grid">{consulta.datos?.items.slice(0,4).map(lote => <TarjetaMercado key={lote.id} lote={lote} />)}</div>
      {consulta.datos?.items.length === 0 && <p>No hay lotes activos próximos a cerrar.</p>}
    </section>
    <section id="inicio-lanzamientos"><h2>Lanzamientos</h2>
      <EstadoMercado estado={ediciones} reintentar={recargar} />
      <div className="ediciones-grid">{ediciones.datos?.slice(0,3).map(pieza => <Lanzamiento key={pieza.productoId} pieza={pieza} />)}</div>
    </section>
    <section id="inicio-categorias"><h2>Explora por categoría</h2>
      <div className="cuadricula-categorias">
        {vitrinas.map(({ nombre, slug, grupo, foto }) => {
          const principal = categoriaPorSlug(slug);
          const grupos = grupo ? [grupo] : principal.lotes;
          const cantidad = grupos.reduce((suma, categoria) => suma + (conteos.get(categoria) || 0), 0);
          const filtro = grupo ? { grupo } : { categoria: slug };
          return <Link to={'/subastas?' + new URLSearchParams({ ...filtro, activas: 'true' })} key={nombre}>
            <img src={'/mercado/vertice-' + foto + '-mini.webp'} width="600" height="450" loading="lazy" alt="" />
            <span><strong>{nombre}</strong><small>{consulta.datos ? cantidad + (cantidad === 1 ? ' lote activo' : ' lotes activos') : 'Consultando…'}</small></span>
          </Link>;
        })}
      </div>
    </section>
  </main>;
}

export function Lista() {
  const [params, cambiarParams] = useSearchParams();
  const pagina = Math.max(0, Number(params.get('pagina')) || 0);
  const cargador = useCallback(() => {
    const query = Object.fromEntries(params);
    const principal = categoriaPorSlug(query.categoria);
    return api.listar(principal ? { ...query, curados:true, categoria:undefined, grupo:principal.lotes.join(',') } : {...query,curados:true});
  }, [params]);
  const consulta = useConsulta(cargador, true);

  function filtrar(valores) {
    const filtros = Object.fromEntries(Object.entries(valores).filter(([, valor]) => String(valor).trim()));
    if (params.get('activas') === 'true') filtros.activas = 'true';
    cambiarParams(filtros);
  }

  function mover(nuevo) {
    const siguiente = new URLSearchParams(params);
    siguiente.set('pagina', nuevo);
    cambiarParams(siguiente);
  }

  return <main id="principal" className="auction-page">
    <Titulo>Subastas</Titulo>
    <p className="auction-muted">Ofertas en COP y pagos simulados.</p>
    <div className="mercado-seccion">
      <FiltrosMercado lote valores={Object.fromEntries(params)} categorias={categoriasSubastas.map(c => [c.slug,c.nombre])} alBuscar={filtrar} alLimpiar={() => cambiarParams({})}
        key={[...params].filter(([k]) => k !== 'pagina').join()} />
      <section aria-label="Resultados">
        <EstadoConsulta consulta={consulta} />
        {consulta.datos && <>
          <p className="auction-muted">{consulta.datos.total} {consulta.datos.total === 1 ? 'lote' : 'lotes'}</p>
          <div className="tt-grid">{consulta.datos.items.map(lote => <TarjetaMercado key={lote.id} lote={lote} />)}</div>
          {!consulta.datos.total && <p className="auction-panel">No hay lotes con estos filtros. <Link to="/subastas">Ver todos</Link></p>}
          <Paginacion pagina={pagina} paginas={Math.ceil(consulta.datos.total / consulta.datos.tamano)} cambiar={mover} />
        </>}
      </section>
    </div>
  </main>;
}

export function Detalle({ user }) {
  const { id } = useParams();
  const cargar = useCallback(() => api.detalle(id), [id]);
  const consulta = useConsulta(cargar, true);
  const [monto, setMonto] = useState('');
  const [seleccion, setSeleccion] = useState(0);
  const [tab, setTab] = useState('Por qué se subasta');
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [nueva, setNueva] = useState(null);
  const [oferta, setOferta] = useState(null);
  const cotizar = useCallback(() => api.precioUsd(id),[id]);
  const consultaUsd = useConsulta(cotizar, true);
  const entrega = useConsulta(api.entrega);
  const clima = useConsulta(api.clima);
  const detalle = consulta.datos;

  const lote = nueva && detalle && nueva.id === detalle.lote.id && nueva.version > detalle.lote.version ? nueva : detalle?.lote;
  const puedePujar = lote?.estado === 'ACTIVA' && Date.parse(lote.fin) > Date.now();
  const yaPujo = detalle?.historial.some(p => p.mia) || lote?.voyGanando;

  function revisar(evento) {
    evento.preventDefault();
    setError('');
    setOferta(Number(monto || lote.pujaMinima));
  }

  async function pujar() {
    setEnviando(true);
    setError('');
    setAviso('');
    try {
      setNueva(await api.pujar(id, oferta));
      setMonto('');
      setAviso('Tu oferta quedó registrada.');
    } catch (fallo) {
      setError(fallo.message);
    } finally {
      setOferta(null);
      setEnviando(false);
    }
  }

  return <main id="principal" className="auction-page">
    <Link to="/subastas">Volver a subastas</Link>
    <EstadoConsulta consulta={consulta} />
    {lote && <>
      <Titulo>{tituloDelLote(lote.titulo)}</Titulo>
      <div className="auction-detail">
        <section>
          <img className="auction-photo" src={lote.imagenes[seleccion] || lote.imagenes[0]} width="424" height="424" alt={tituloDelLote(lote.titulo)} />
          {lote.imagenes.length > 1 && <div className="auction-thumbs">{lote.imagenes.map((src, index) => <button type="button" key={src} aria-pressed={index === seleccion} onClick={() => setSeleccion(index)}><img src={src} width="68" height="68" alt={index === 0 ? 'Vista general del lote' : 'Vista de detalle del lote'} /></button>)}</div>}
          <div className="auction-tabs" aria-label="Información del lote">
            {['Por qué se subasta', 'Ficha técnica', 'Historial de pujas'].map(nombre => <button key={nombre} aria-pressed={tab === nombre} onClick={() => setTab(nombre)}>{nombre}</button>)}
          </div>
          <div className="auction-panel">
            {tab === 'Por qué se subasta' && <div className="relato-lote">
              <p>{lote.descripcion}</p>{lote.atributos['Por qué se subasta'] && <p>{lote.atributos['Por qué se subasta']}</p>}
            </div>}
            {tab === 'Ficha técnica' && (Object.keys(lote.atributos).length ? <dl>{Object.entries(lote.atributos).filter(([nombre]) => nombre !== 'Por qué se subasta' && nombre !== 'Visibilidad' && nombre !== 'Reserva COP').map(([nombre, dato]) => <React.Fragment key={nombre}><dt>{nombre}</dt><dd>{dato}</dd></React.Fragment>)}</dl> : <p>Este lote no tiene atributos adicionales.</p>)}
            {tab === 'Historial de pujas' && (detalle.historial.length ? <ul className="auction-records">{detalle.historial.map((puja, index) => <li key={index}><span>{puja.mia ? 'Tú' : puja.postor} · {fecha(puja.fecha)}</span><strong>{cop.format(puja.monto)}{consultaUsd.datos?.disponible ? ' · ≈ ' + usd.format(puja.monto * consultaUsd.datos.tasa) : ''}</strong></li>)}</ul> : <p>Aún no hay pujas.</p>)}
          </div>
        </section>
        <section id="ofertar" className="auction-panel auction-bid tema-oscuro">
          <span className="auction-muted">{categorias[lote.categoria]} · {lote.condicion === 'NUEVO' ? 'Nuevo' : 'Usado'}</span>
          <div className="auction-bid-head"><span>{lote.pujas ? 'Oferta líder' : 'Salida'} · {estados[lote.estado]} · COP</span><Cierre lote={lote} /></div>
          <span>{lote.reservaAlcanzada ? 'Reserva alcanzada' : 'Reserva no alcanzada'}</span>
          <strong className="auction-price">{cop.format(lote.precioActual)}</strong>
          {!consultaUsd.cargando && <p className="auction-muted">{consultaUsd.datos?.disponible ? '≈ ' + usd.format(lote.precioActual * consultaUsd.datos.tasa) + ' · Frankfurter' : 'Referencia USD no disponible'}</p>}
          <dl className="auction-valores"><dt>Incremento</dt><dd>{cop.format(lote.incremento)}</dd>
            {puedePujar && <><dt>Próxima oferta</dt><dd>{cop.format(lote.pujaMinima)}</dd></>}
          </dl>
          {puedePujar && <p className="auction-muted auction-regla">Últimos 2 min: una puja aceptada añade 2 min.</p>}
          {yaPujo && puedePujar && <p role="status">{lote.voyGanando ? 'Vas ganando' : 'Te superaron'}</p>}
          {puedePujar && (user ? <form onSubmit={revisar} className="auction-form">
            <label>Puja mínima requerida · {cop.format(lote.pujaMinima)} COP
              <input type="number" min={lote.pujaMinima} step="0.01" value={monto} placeholder={String(lote.pujaMinima)} onChange={e => setMonto(e.target.value)} aria-label="Monto de la puja en COP" />
            </label>
            <div className="auction-row">{[1, 2].map(n => <button type="button" key={n} onClick={() => setMonto(String(Number(lote.pujaMinima) + Number(lote.incremento) * n))}>+{cop.format(lote.incremento * n)}</button>)}</div>
            <button disabled={enviando || lote.voyGanando || oferta !== null} className="primary">Revisar oferta</button>
          </form> : <Link className="auction-btn primary" to="/login" state={{ volver: '/subastas/' + lote.id }}>Inicia sesión para pujar</Link>)}
          {oferta !== null && <fieldset className="vertice-confirmacion">
            <legend>Confirmar oferta</legend><p>{tituloDelLote(lote.titulo)} · {cop.format(oferta)}</p>
            <button type="button" onClick={() => setOferta(null)} disabled={enviando}>Editar</button>
            <button type="button" className="primary" onClick={pujar} disabled={enviando || !puedePujar || oferta < lote.pujaMinima}>
              {enviando ? 'Enviando…' : 'Confirmar puja'}
            </button>
            {oferta < lote.pujaMinima && <p role="status">La puja mínima cambió. Edita la oferta.</p>}
          </fieldset>}
          {detalle.adjudicacion && <PagoSubasta orden={detalle.adjudicacion} cotizacion={consultaUsd.datos} />}
          {aviso && <p role="status">{aviso}</p>}
          {error && <p role="alert" className="auction-error">{error}</p>}
        </section>
      </div>
      <section className="auction-referencias" aria-label="Entrega y conservación">
        <div className="auction-panel"><h2>Entrega simulada</h2><EntregaLote estado={entrega} /></div>
        <div className="auction-panel"><h2>Conservación</h2>
          {!clima.cargando && <p className="auction-muted">{clima.datos?.disponible
            ? 'Bogotá · ' + clima.datos.observacion.temperatura + ' °C · ' + clima.datos.observacion.humedad + ' % HR · Open-Meteo'
            : 'Referencia de clima no disponible.'}</p>}
        </div>
      </section>
      {puedePujar && <div className="auction-mobile-bid"><strong>{cop.format(lote.precioActual)}</strong><a className="auction-btn primary" href="#ofertar">Pujar</a></div>}
    </>}
  </main>;
}

function ReferenciaUsd({ monto, cotizacion }) {
  return <small className="auction-muted">{cotizacion?.disponible
    ? '≈ ' + usd.format(monto * cotizacion.tasa) + ' · Frankfurter'
    : 'Referencia USD no disponible'}</small>;
}

function PagoSubasta({ orden, cotizacion }) {
  return <PagoSimulado referencia={'SIM-SUBASTA-' + orden.id} monto={orden.monto}
    referenciaUsd={cotizacion?.disponible ? orden.monto * cotizacion.tasa : null}
    pagadoEn={orden.pagada} confirmar={async () => (await api.pagar(orden.loteId)).pagada} />;
}

export function MisPujas({ vista }) {
  const consulta = useConsulta(api.mias, true);
  const entrega = useConsulta(api.entrega);
  const primerLote = vista === 'ganadas' ? consulta.datos?.adjudicaciones[0]?.loteId : consulta.datos?.pujas[0]?.loteId;
  const cotizar = useCallback(() => primerLote ? api.precioUsd(primerLote) : Promise.resolve(null), [primerLote]);
  const cotizacion = useConsulta(cotizar);
  return <main id="principal" className="auction-page">
    <Titulo>{vista === 'ganadas' ? 'Subastas ganadas' : 'Mis pujas'}</Titulo>
    <EstadoConsulta consulta={consulta} />
    {consulta.datos && <>
      {vista !== 'pujas' && <>
      {!!consulta.datos.adjudicaciones.length && <EntregaLote estado={entrega} />}
      <div className="auction-panel">{consulta.datos.adjudicaciones.length ? <ul className="auction-records">
        {consulta.datos.adjudicaciones.map(orden => <li key={orden.id}>
          <Link to={'/subastas/' + orden.loteId}>Lote {orden.loteId}</Link>
          <PagoSubasta orden={orden} cotizacion={cotizacion.datos} /></li>)}
      </ul> : <p>Aún no tienes adjudicaciones.</p>}</div>
      </>}
      {vista !== 'ganadas' && <><h2>Historial de pujas</h2>
      <div className="auction-panel">{consulta.datos.pujas.length ? <ul className="auction-records">
        {consulta.datos.pujas.map((puja, index) => <li key={index}>
          <Link to={'/subastas/' + puja.loteId}>Lote {puja.loteId}</Link>
          <div className="auction-adjudicacion"><strong>{cop.format(puja.monto)} COP</strong>
            <ReferenciaUsd monto={puja.monto} cotizacion={cotizacion.datos} />
            <span>{fecha(puja.fecha)}</span>
          </div></li>)}
      </ul> : <p>Todavía no has pujado. <Link to="/subastas">Ver subastas</Link></p>}</div></>}
    </>}
  </main>;
}

export function AdminSubastas() {
  const [categoria, setCategoria] = useState('RELOJES');
  const [estado, setEstado] = useState({ ocupado: false, aviso: '', error: '' });
  const consulta = useConsulta(api.listar);

  async function ejecutar(operacion) {
    setEstado({ ocupado: true, aviso: '', error: '' });
    try {
      const aviso = await operacion();
      setEstado({ ocupado: false, aviso, error: '' });
      consulta.recargar();
    } catch (error) {
      setEstado({ ocupado: false, aviso: '', error: error.message });
    }
  }

  function crear(evento) {
    evento.preventDefault();
    const formulario = evento.currentTarget;
    const datos = Object.fromEntries(new FormData(formulario));
    ejecutar(async () => {
      const atributos = Object.fromEntries(
        ['Año', 'Kilometraje', 'Material', 'Quilates'].filter(campo => datos[campo]?.trim()).map(campo => [campo, datos[campo].trim()])
      );
      const lote = await api.crear({
        titulo: datos.titulo, descripcion: datos.descripcion, categoria: datos.categoria, condicion: datos.condicion,
        imagenes: [img(datos.categoria)], atributos, precioInicial: Number(datos.precioInicial),
        incremento: Number(datos.incremento), inicio: new Date(datos.inicio).toISOString(),
        fin: new Date(datos.fin).toISOString(),
      });
      formulario.reset();
      setCategoria('RELOJES');
      return 'Lote ' + lote.id + ' creado.';
    });
  }

  function cancelar(id) {
    if (window.confirm('¿Cancelar el lote ' + id + '?')) {
      ejecutar(async () => { await api.cancelar(id); return 'Lote ' + id + ' cancelado.'; });
    }
  }

  return <main id="principal" className="auction-page">
    <Titulo>Administrar subastas</Titulo>
    <form className="auction-panel auction-form admin-auction" onSubmit={crear}>
      <label>Título<input name="titulo" required maxLength="150" /></label>
      <label>Descripción<textarea name="descripcion" required maxLength="4000" /></label>
      <label>Categoría<select name="categoria" value={categoria} onChange={e => setCategoria(e.target.value)}>
        {categoriasSubastas.flatMap(({ lotes }) => lotes).filter(id => id !== 'OTROS').map(id =>
          <option value={id} key={id}>{id === 'CARROS' ? 'Carros de colección' : id === 'MOTOS' ? 'Motos de colección' : categorias[id]}</option>)}
      </select></label>
      <label>Condición<select name="condicion"><option value="NUEVO">Nuevo</option><option value="USADO">Usado</option></select></label>
      {['CARROS', 'MOTOS'].includes(categoria) && <><label>Año<input name="Año" required maxLength="4" /></label><label>Kilometraje<input name="Kilometraje" required maxLength="100" /></label></>}
      {categoria === 'JOYERIA' && <><label>Material<input name="Material" required maxLength="100" /></label><label>Quilates<input name="Quilates" required maxLength="100" /></label></>}
      <label>Precio inicial (COP)<input name="precioInicial" type="number" required min="1" step="0.01" /></label>
      <label>Incremento (COP)<input name="incremento" type="number" required min="1" step="0.01" /></label>
      <label>Inicio<input name="inicio" type="datetime-local" required /></label>
      <label>Fin<input name="fin" type="datetime-local" required /></label>
      <button className="primary" disabled={estado.ocupado}>{estado.ocupado ? 'Guardando…' : 'Crear lote'}</button>
    </form>
    {estado.aviso && <p role="status">{estado.aviso}</p>}
    {estado.error && <p className="auction-error" role="alert">{estado.error}</p>}
    <h2>Cancelar un lote</h2>
    <EstadoConsulta consulta={consulta} />
    <form className="auction-panel auction-form" onSubmit={evento => {
      evento.preventDefault();
      cancelar(new FormData(evento.currentTarget).get('lote'));
    }}>
      <label>ID del lote<input type="number" name="lote" min="1" required /></label>
      <button className="danger" disabled={estado.ocupado}>Cancelar lote</button>
    </form>
    <ul className="auction-records">{consulta.datos?.items.map(lote => <li key={lote.id}><Link to={'/subastas/' + lote.id}>#{lote.id} · {lote.titulo}</Link><span>{estados[lote.estado]}</span></li>)}</ul>
  </main>;
}

export function NoEncontrado() {
  return <main id="principal" className="auction-page"><Titulo>Página no encontrada</Titulo><Link className="auction-btn primary" to="/">Volver al inicio</Link></main>;
}
