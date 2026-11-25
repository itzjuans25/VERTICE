import { tituloMarca } from '../marca';
import React, { useCallback, useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { subastasApi as api } from '../api/subastasApi';
import './Subastas.css';

export const categorias = {
  RELOJES: 'Relojes', JOYERIA: 'Joyería', CARROS: 'Carros', MOTOS: 'Motos',
  ELECTRONICA: 'Electrónica', COLECCIONABLES: 'Coleccionables', HOGAR: 'Hogar', OTROS: 'Otros',
};
const estados = { ACTIVA: 'Activa', PROGRAMADA: 'Programada', CERRADA: 'Cerrada', ADJUDICADA: 'Adjudicada', CANCELADA: 'Cancelada' };
const img = categoria => '/subastas/' + categoria.toLowerCase() + '.webp';
const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2, minimumFractionDigits: 0 });
const fecha = valor => new Date(valor).toLocaleString('es-CO');
const vacio = { q: '', categoria: '', condicion: '', minimo: '', maximo: '' };

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

function Cierre({ lote }) {
  const [ahora, setAhora] = useState(Date.now);
  const programada = lote.estado === 'PROGRAMADA';
  const segundo = Math.max(0, Math.ceil((Date.parse(programada ? lote.inicio : lote.fin) - ahora) / 1000));

  useEffect(() => {
    if (lote.estado !== 'ACTIVA' && lote.estado !== 'PROGRAMADA') return undefined;
    const timer = setInterval(() => {
      if (document.visibilityState !== 'hidden') setAhora(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, [lote.estado]);

  if (!['ACTIVA', 'PROGRAMADA'].includes(lote.estado)) return <span className="auction-chip">{estados[lote.estado]}</span>;
  if (!segundo) return <span className="auction-chip">{programada ? 'Por iniciar' : 'Plazo terminado'}</span>;
  const horas = Math.floor(segundo / 3600);
  const minutos = Math.floor(segundo / 60) % 60;
  const segundos = String(segundo % 60).padStart(2, '0');
  const color = segundo < 3600 ? 'rojo' : segundo < 86400 ? 'ambar' : '';
  return <span className={'auction-chip ' + color}>{programada ? 'Inicia en ' : 'Cierra en '}{horas ? horas + ' h ' : ''}{minutos} min {segundos} s</span>;
}

function Tarjeta({ lote }) {
  return <article className="auction-card">
    <Link to={'/subastas/' + lote.id}>
      <img src={lote.imagenes[0]} width="424" height="424" loading="lazy" alt={lote.titulo} />
      <div className="auction-card-body">
        <span className="auction-muted">{categorias[lote.categoria]} · {lote.condicion === 'NUEVO' ? 'Nuevo' : 'Usado'}</span>
        <h2>{lote.titulo}</h2>
        <div className="auction-row"><strong>{cop.format(lote.precioActual)}</strong><span>{lote.pujas} {lote.pujas === 1 ? 'puja' : 'pujas'}</span></div>
        <Cierre lote={lote} />
      </div>
    </Link>
  </article>;
}

function EstadoConsulta({ consulta }) {
  return <>
    {consulta.cargando && <div className="auction-skeleton" role="status">Cargando…</div>}
    {consulta.error && <div className="auction-error" role="alert">{consulta.error} <button onClick={consulta.recargar}>Reintentar</button></div>}
  </>;
}

export function Inicio() {
  const cargar = useCallback(() => api.listar({ tamano: 3 }), []);
  const consulta = useConsulta(cargar);
  const lotes = consulta.datos?.items.filter(l => l.estado === 'ACTIVA');

  return <main id="principal" className="auction-page">
    <section className="auction-hero">
      <div><Titulo>Tienda y subastas</Titulo>
        <p>Compra tecnología nueva y usada o presenta tu oferta en los lotes de la tienda.</p>
        <div className="auction-row">
          <Link className="auction-btn primary" to="/productos">Ver tienda</Link>
          <Link className="auction-btn" to="/subastas">Ver subastas</Link>
        </div>
      </div>
      <img src={img('ELECTRONICA')} alt="Cámara digital" width="424" height="424" />
    </section>
    {lotes?.length > 0 && <section><h2>Subastas que terminan pronto</h2><div className="auction-grid">{lotes.map(lote => <Tarjeta key={lote.id} lote={lote} />)}</div></section>}
    <section><h2>Categorías de subastas</h2><div className="auction-categories">
      {Object.entries(categorias).map(([id, label]) => <Link to={'/subastas?categoria=' + id} key={id}><img src={img(id)} width="424" height="424" loading="lazy" alt="" />{label}</Link>)}
    </div></section>
  </main>;
}

export function Lista() {
  const [params, cambiarParams] = useSearchParams();
  const pagina = Math.max(0, Number(params.get('pagina')) || 0);
  const cargador = useCallback(() => api.listar(Object.fromEntries(params)), [params]);
  const consulta = useConsulta(cargador, true);

  function filtrar(evento) {
    evento.preventDefault();
    const entries = [...new FormData(evento.currentTarget)].filter(([, valor]) => String(valor).trim());
    cambiarParams(Object.fromEntries(entries));
  }

  function mover(nuevo) {
    const siguiente = new URLSearchParams(params);
    siguiente.set('pagina', nuevo);
    cambiarParams(siguiente);
  }

  return <main id="principal" className="auction-page">
    <Titulo>Subastas</Titulo>
    <p className="auction-muted">Ofertas en COP y pagos simulados.</p>
    <div className="auction-layout">
      <form className="auction-panel auction-form" onSubmit={filtrar} key={[...params].filter(([k]) => k !== 'pagina').join()}>
        <label>Buscar<input name="q" placeholder="Nombre del lote" defaultValue={params.get('q') || ''} maxLength="100" /></label>
        <label>Categoría<select name="categoria" defaultValue={params.get('categoria') || ''}><option value="">Todas</option>{Object.entries(categorias).map(([id, label]) => <option key={id} value={id}>{label}</option>)}</select></label>
        <label>Condición<select name="condicion" defaultValue={params.get('condicion') || ''}><option value="">Todas</option><option value="NUEVO">Nuevo</option><option value="USADO">Usado</option></select></label>
        <label>Precio mínimo<input name="minimo" type="number" min="0" defaultValue={params.get('minimo') || ''} /></label>
        <label>Precio máximo<input name="maximo" type="number" min="0" defaultValue={params.get('maximo') || ''} /></label>
        <button className="primary">Aplicar filtros</button><Link to="/subastas">Limpiar filtros</Link>
      </form>
      <section aria-label="Resultados">
        <EstadoConsulta consulta={consulta} />
        {consulta.datos && <>
          <p className="auction-muted">{consulta.datos.total} {consulta.datos.total === 1 ? 'lote' : 'lotes'}</p>
          <div className="auction-grid">{consulta.datos.items.map(lote => <Tarjeta key={lote.id} lote={lote} />)}</div>
          {!consulta.datos.total && <p className="auction-panel">No hay lotes con estos filtros. <Link to="/subastas">Ver todos</Link></p>}
          {consulta.datos.total > consulta.datos.tamano && <div className="auction-pager">
            <button disabled={!pagina} onClick={() => mover(pagina - 1)}>Anterior</button>
            <span>Página {pagina + 1}</span>
            <button disabled={(pagina + 1) * consulta.datos.tamano >= consulta.datos.total} onClick={() => mover(pagina + 1)}>Siguiente</button>
          </div>}
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
  const [tab, setTab] = useState('Descripción');
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [nueva, setNueva] = useState(null);
  const detalle = consulta.datos;

  const lote = nueva && detalle && nueva.id === detalle.lote.id && nueva.version > detalle.lote.version ? nueva : detalle?.lote;
  const puedePujar = lote?.estado === 'ACTIVA' && Date.parse(lote.fin) > Date.now();
  const yaPujo = detalle?.historial.some(p => p.mia) || lote?.voyGanando;

  async function pujar(evento) {
    evento.preventDefault();
    setEnviando(true);
    setError('');
    setAviso('');
    try {
      setNueva(await api.pujar(id, Number(monto || lote.pujaMinima)));
      setMonto('');
      setAviso('Tu oferta quedó registrada.');
    } catch (fallo) {
      setError(fallo.message);
    } finally {
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
      <Titulo>{lote.titulo}</Titulo>
      <div className="auction-detail">
        <section>
          <img className="auction-photo" src={lote.imagenes[seleccion] || lote.imagenes[0]} width="424" height="424" alt={lote.titulo} />
          {lote.imagenes.length > 1 && <div className="auction-thumbs">{lote.imagenes.map((src, index) => <button type="button" key={src} aria-pressed={index === seleccion} onClick={() => setSeleccion(index)}><img src={src} width="68" height="68" alt={'Foto ' + (index + 1)} /></button>)}</div>}
          <div className="auction-tabs" aria-label="Información del lote">
            {['Descripción', 'Atributos', 'Historial'].map(nombre => <button key={nombre} aria-pressed={tab === nombre} onClick={() => setTab(nombre)}>{nombre}</button>)}
          </div>
          <div className="auction-panel">
            {tab === 'Descripción' && <p>{lote.descripcion}</p>}
            {tab === 'Atributos' && (Object.keys(lote.atributos).length ? <dl>{Object.entries(lote.atributos).map(([nombre, dato]) => <React.Fragment key={nombre}><dt>{nombre}</dt><dd>{dato}</dd></React.Fragment>)}</dl> : <p>Este lote no tiene atributos adicionales.</p>)}
            {tab === 'Historial' && (detalle.historial.length ? <ul className="auction-records">{detalle.historial.map((puja, index) => <li key={index}><span>{puja.mia ? 'Tú' : puja.postor} · {fecha(puja.fecha)}</span><strong>{cop.format(puja.monto)}</strong></li>)}</ul> : <p>Aún no hay pujas.</p>)}
          </div>
        </section>
        <section id="ofertar" className="auction-panel auction-bid">
          <span className="auction-muted">{categorias[lote.categoria]} · {lote.condicion === 'NUEVO' ? 'Nuevo' : 'Usado'}</span>
          <strong className="auction-price">{cop.format(lote.precioActual)}</strong>
          <p><Cierre lote={lote} /></p>
          {yaPujo && puedePujar && <p role="status">{lote.voyGanando ? 'Vas ganando' : 'Te superaron'}</p>}
          {puedePujar && (user ? <form onSubmit={pujar} className="auction-form">
            <label>Puja mínima {cop.format(lote.pujaMinima)}
              <input type="number" min={lote.pujaMinima} step="0.01" value={monto} placeholder={String(lote.pujaMinima)} onChange={e => setMonto(e.target.value)} aria-label="Monto de la puja en COP" />
            </label>
            <div className="auction-row">{[1, 2].map(n => <button type="button" key={n} onClick={() => setMonto(String(Number(lote.pujaMinima) + Number(lote.incremento) * n))}>+{cop.format(lote.incremento * n)}</button>)}</div>
            <button disabled={enviando || lote.voyGanando} className="primary">{enviando ? 'Enviando…' : 'Confirmar puja'}</button>
          </form> : <Link className="auction-btn primary" to="/login">Inicia sesión para pujar</Link>)}
          {detalle.adjudicacion && (detalle.adjudicacion.pagada ? <p>Pago simulado registrado.</p> : <button className="primary" disabled={enviando} onClick={pagar}>Confirmar pago simulado</button>)}
          {aviso && <p role="status">{aviso}</p>}
          {error && <p role="alert" className="auction-error">{error}</p>}
        </section>
      </div>
      {puedePujar && <div className="auction-mobile-bid"><strong>{cop.format(lote.precioActual)}</strong><a className="auction-btn primary" href="#ofertar">Pujar</a></div>}
    </>}
  </main>;
}

export function MisPujas() {
  const consulta = useConsulta(api.mias, true);
  return <main id="principal" className="auction-page">
    <Titulo>Mis pujas</Titulo>
    <EstadoConsulta consulta={consulta} />
    {consulta.datos && <>
      <h2>Adjudicaciones</h2>
      <div className="auction-panel">{consulta.datos.adjudicaciones.length ? <ul className="auction-records">
        {consulta.datos.adjudicaciones.map(orden => <li key={orden.id}><Link to={'/subastas/' + orden.loteId}>Lote {orden.loteId}</Link><span>{cop.format(orden.monto)} · {orden.pagada ? 'Pago simulado confirmado' : 'Pendiente de pago simulado'}</span></li>)}
      </ul> : <p>Aún no tienes adjudicaciones.</p>}</div>
      <h2>Historial de pujas</h2>
      <div className="auction-panel">{consulta.datos.pujas.length ? <ul className="auction-records">
        {consulta.datos.pujas.map((puja, index) => <li key={index}><Link to={'/subastas/' + puja.loteId}>Lote {puja.loteId}</Link><span>{cop.format(puja.monto)} · {fecha(puja.fecha)}</span></li>)}
      </ul> : <p>Todavía no has pujado. <Link to="/subastas">Ver subastas</Link></p>}</div>
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
