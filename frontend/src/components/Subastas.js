import { marca, tituloMarca } from '../marca';
import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { subastasApi as api } from '../api/subastasApi';
import './Subastas.css';
import { categoriasMercado, nombresCategorias as categorias, categoriaLote, categoriaPorSlug, vitrinas } from '../categorias';
import { Cierre, cop, tituloDelLote, TarjetaMercado, FiltrosMercado, Paginacion, EstadoMercado } from './MercadoUI';


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
  const cupo = new Map();
  const lotes = consulta.datos?.items.filter(l => {
    const categoria = categoriaLote(l.categoria)?.slug || l.categoria;
    const cantidad = cupo.get(categoria) || 0;
    if (l.estado !== 'ACTIVA' || cantidad >= 2) return false;
    cupo.set(categoria, cantidad+1);
    return true;
  });

  return <main id="principal" className="auction-page mercado-inicio">
    <nav className="mercado-anclas" aria-label="Secciones del inicio">
      <a href="#inicio-subastas">Próximos cierres</a><a href="#inicio-categorias">Vitrinas</a><a href="#inicio-como">Cómo pujar</a>
    </nav>
    <section className="auction-hero">
      <div><Titulo>{marca.lema}</Titulo>
        <p>Relojería de colección, moda de autor y piezas de exhibición con un motivo para cada subasta.</p>
        <div className="auction-row">
          <Link className="auction-btn primary" to="/subastas">Explorar subastas</Link>
        </div>
      </div>
      <img src="/mercado/vertice-portada-galeria.webp" alt="Cronógrafo de esfera negra y caja dorada sobre piedra" width="1200" height="900" />
    </section>
    <section id="inicio-subastas" className="mercado-seccion"><h2>Subastas que cierran pronto</h2>
      <EstadoConsulta consulta={consulta} />
      <div className="tt-grid">{lotes?.map(lote => <TarjetaMercado key={lote.id} lote={lote} />)}</div>
      {lotes?.length === 0 && <p>En este momento no hay lotes activos próximos a cerrar.</p>}
    </section>
    <section id="inicio-categorias"><h2>Vitrinas</h2><div className="auction-categories mercado-categorias vertice-vitrinas">
      {vitrinas.map(({ nombre, grupo, foto }) => <Link to={'/subastas?' + new URLSearchParams({ grupo })} key={grupo}>
        <img src={'/mercado/vertice-' + foto + '.webp'} width="400" height="300" loading="lazy" alt="" />
        <strong>{nombre}</strong>
      </Link>)}
    </div></section>
    <section id="inicio-como"><h2>Cómo pujar</h2><ol className="mercado-pasos">
      <li><strong>Elige</strong> Comprueba descripción, condición y plazo del lote.</li>
      <li><strong>Puja</strong> Inicia sesión y ofrece el monto mínimo o uno mayor.</li>
      <li><strong>Gana</strong> Consulta Mis subastas ganadas y confirma el pago simulado.</li>
    </ol></section>
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
    cambiarParams(Object.fromEntries(Object.entries(valores).filter(([, valor]) => String(valor).trim())));
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
      <FiltrosMercado lote valores={Object.fromEntries(params)} categorias={categoriasMercado.map(c => [c.slug,c.nombre])} alBuscar={filtrar} alLimpiar={() => cambiarParams({})}
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

  async function pagar() {
    setEnviando(true);
    setError('');
    try {
      await api.pagar(id);
      setAviso('Pago simulado confirmado.');
      consulta.recargar();
    } catch (fallo) {
      setError(fallo.message);
    } finally {
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
          {lote.imagenes.length > 1 && <div className="auction-thumbs">{lote.imagenes.map((src, index) => <button type="button" key={src} aria-pressed={index === seleccion} onClick={() => setSeleccion(index)}><img src={src} width="68" height="68" alt={'Foto ' + (index + 1)} /></button>)}</div>}
          <div className="auction-tabs" aria-label="Información del lote">
            {['Por qué se subasta', 'Ficha técnica', 'Historial de pujas'].map(nombre => <button key={nombre} aria-pressed={tab === nombre} onClick={() => setTab(nombre)}>{nombre}</button>)}
          </div>
          <div className="auction-panel">
            {tab === 'Por qué se subasta' && <p>{lote.atributos['Por qué se subasta'] || lote.descripcion}</p>}
            {tab === 'Ficha técnica' && (Object.keys(lote.atributos).length ? <dl>{Object.entries(lote.atributos).filter(([nombre]) => nombre !== 'Por qué se subasta' && nombre !== 'Visibilidad').map(([nombre, dato]) => <React.Fragment key={nombre}><dt>{nombre}</dt><dd>{dato}</dd></React.Fragment>)}</dl> : <p>Este lote no tiene atributos adicionales.</p>)}
            {tab === 'Historial de pujas' && (detalle.historial.length ? <ul className="auction-records">{detalle.historial.map((puja, index) => <li key={index}><span>{puja.mia ? 'Tú' : puja.postor} · {fecha(puja.fecha)}</span><strong>{cop.format(puja.monto)}{consultaUsd.datos?.disponible ? ' · ≈ ' + usd.format(puja.monto * consultaUsd.datos.tasa) : ''}</strong></li>)}</ul> : <p>Aún no hay pujas.</p>)}
          </div>
        </section>
        <section id="ofertar" className="auction-panel auction-bid">
          <span className="auction-muted">{categorias[lote.categoria]} · {lote.condicion === 'NUEVO' ? 'Nuevo' : 'Usado'}</span>
          <div className="auction-bid-head"><span>{lote.pujas ? 'Oferta líder' : 'Salida'} · {estados[lote.estado]}</span><Cierre lote={lote} /></div>
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
          {detalle.adjudicacion && (detalle.adjudicacion.pagada ? <p>Pago simulado registrado.</p> : <button className="primary" disabled={enviando} onClick={pagar}>Confirmar pago simulado</button>)}
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

export function MisPujas({ vista }) {
  const consulta = useConsulta(api.mias, true);
  const entrega = useConsulta(api.entrega);
  return <main id="principal" className="auction-page">
    <Titulo>{vista === 'ganadas' ? 'Subastas ganadas' : 'Mis pujas'}</Titulo>
    <EstadoConsulta consulta={consulta} />
    {consulta.datos && <>
      {vista !== 'pujas' && <>
      {!!consulta.datos.adjudicaciones.length && <EntregaLote estado={entrega} />}
      <div className="auction-panel">{consulta.datos.adjudicaciones.length ? <ul className="auction-records">
        {consulta.datos.adjudicaciones.map(orden => <li key={orden.id}><Link to={'/subastas/' + orden.loteId}>Lote {orden.loteId}</Link><span>{cop.format(orden.monto)} · {orden.pagada ? 'Pago simulado confirmado' : 'Pendiente de pago simulado'}</span></li>)}
      </ul> : <p>Aún no tienes adjudicaciones.</p>}</div>
      </>}
      {vista !== 'ganadas' && <><h2>Historial de pujas</h2>
      <div className="auction-panel">{consulta.datos.pujas.length ? <ul className="auction-records">
        {consulta.datos.pujas.map((puja, index) => <li key={index}><Link to={'/subastas/' + puja.loteId}>Lote {puja.loteId}</Link><span>{cop.format(puja.monto)} · {fecha(puja.fecha)}</span></li>)}
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
      <label>Categoría<select name="categoria" value={categoria} onChange={e => setCategoria(e.target.value)}>{Object.entries(categorias).map(([id, label]) => <option value={id} key={id}>{label}</option>)}</select></label>
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
