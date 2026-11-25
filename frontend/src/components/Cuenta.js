import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { tituloMarca } from '../marca';

const pestanas = [
  ['/cuenta', 'Compras', ['/compras']],
  ['/mis-pujas', 'Pujas', []],
  ['/cuenta/ganadas', 'Subastas ganadas', []],
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
  React.useEffect(() => { document.title = tituloMarca('Avisos'); }, []);
  return <main className="tt-shell mercado-pagina"><h1>Avisos</h1><p>Los avisos de compras y subastas se envían al correo asociado a tu cuenta.</p></main>;
}

export function Seguridad({ children, user }) {
  return <><section className="tt-shell mercado-pagina"><h1>Perfil y seguridad</h1>
    <dl><dt>Nombre</dt><dd>{user.nombre}</dd><dt>Correo</dt><dd>{user.email}</dd></dl></section>
    {children}</>;
}
