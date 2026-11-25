import React, { useCallback, useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { tituloMarca } from '../marca';
import config from '../config';
import { request } from '../api/client';
import { authApi } from '../api/authApi';
import { EstadoMercado } from './MercadoUI';
import { useMercado } from './ExplorarMercado';

const pestanas = [
  ['/cuenta', 'Actividad', ['/mis-pujas', '/cuenta/ganadas', '/compras']],
  ['/cuenta/avisos', 'Avisos', []],
  ['/cuenta/seguridad', 'Seguridad', ['/cuenta/contrasena']],
];

export default function Cuenta({ children }) {
  const { pathname } = useLocation();
  return <div className="mercado-cuenta-pagina">
    <header><p>Mi cuenta</p><nav className="mercado-tabs" aria-label="Secciones de mi cuenta">
      {pestanas.map(([ruta, nombre, alias]) => <Link to={ruta} key={ruta}
        aria-current={pathname === ruta || alias.includes(pathname) ? 'page' : undefined}>{nombre}</Link>)}
    </nav></header>
    {children}
  </div>;
}

export function Avisos() {
  const [pagina, cambiar] = useState(0);
  const cargar = useCallback(() => request(config.NOTIFICACIONES_URL, '/api/notificaciones/mis-avisos',
    { query: { pagina, tamano: 20 }, requiereSesion: true }), [pagina]);
  const [estado, reintentar] = useMercado(cargar);
  useEffect(() => { document.title = tituloMarca('Avisos'); }, []);
  return <main className="tt-shell mercado-pagina"><h1>Avisos</h1>
    <EstadoMercado estado={estado} reintentar={reintentar} vacio={estado.datos?.items.length === 0 ? 'Aún no tienes avisos.' : ''} />
    {estado.datos?.items.length > 0 && <ul className="auction-records">{estado.datos.items.map(a => <li key={a.id}>
      <strong>{a.asunto}</strong><span>{a.tipo} · {a.estado === 'ENVIADO' ? 'Enviado' : 'Intento fallido'} · {new Date(a.fecha).toLocaleString('es-CO')}</span>
    </li>)}</ul>}
    {(pagina > 0 || estado.datos?.siguiente) && <nav aria-label="Páginas de avisos" className="tt-paginas">
      <button className="tt-button tt-secondary" disabled={pagina === 0} onClick={() => cambiar(p => p-1)}>Anterior</button>
      <span>Página {pagina + 1}</span>
      <button className="tt-button tt-secondary" disabled={!estado.datos?.siguiente} onClick={() => cambiar(p => p+1)}>Siguiente</button>
    </nav>}
  </main>;
}

export function Seguridad({ children }) {
  const [estado, reintentar] = useMercado(authApi.perfil);
  const [guardando, setGuardando] = useState(false);
  const [resultado, setResultado] = useState({});
  async function guardar(e) {
    e.preventDefault();
    setGuardando(true);
    setResultado({});
    try {
      await authApi.editarPerfil(Object.fromEntries(new FormData(e.currentTarget)));
      setResultado({ mensaje:'Perfil actualizado.' });
    } catch (error) {
      setResultado({ error:error.message });
    } finally {
      setGuardando(false);
    }
  }
  return <><section className="tt-shell mercado-pagina"><h1>Perfil y seguridad</h1>
    <EstadoMercado estado={estado} reintentar={reintentar} />
    {estado.datos && <form className="tt-sell-form" onSubmit={guardar}>
      <p>{estado.datos.email}</p>
      <label>Nombre<input name="nombre" defaultValue={estado.datos.nombre} required maxLength={100} /></label>
      <label>Teléfono<input name="telefono" type="tel" defaultValue={estado.datos.telefono || ''} maxLength={30} /></label>
      <button className="tt-button" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar perfil'}</button>
      {resultado.mensaje && <p role="status">{resultado.mensaje}</p>}
      {resultado.error && <p role="alert">{resultado.error}</p>}
    </form>}
  </section>
  {children}</>;
}
