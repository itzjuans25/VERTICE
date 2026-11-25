import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { marca } from '../marca';
import Navbar from './Navbar';
import Logo from './Logo';

const nombres = {
  productos: 'Tienda', subastas: 'Subastas', buscar: 'Buscar', categoria: 'Categorías',
  carrito: 'Carrito', cuenta: 'Mi cuenta', compras: 'Mi cuenta', 'mis-pujas': 'Mi cuenta',
  login: 'Iniciar sesión', register: 'Registro', admin: 'Administración',
};

export default function Layout({ children, user, onLogout, cartCount }) {
  const { pathname } = useLocation();
  const partes = pathname.split('/').filter(Boolean);
  const actual = nombres[partes[0]] || 'Página';
  const padre = partes.length === 2 && ['productos','subastas'].includes(partes[0]);
  return <>
    <a className="tech-skip" href="#contenido">Saltar al contenido</a>
    <Navbar user={user} onLogout={onLogout} cartCount={cartCount} />
    <div id="contenido" className="mercado-layout" tabIndex={-1}>
      {partes.length > 0 && <nav className="mercado-migas" aria-label="Migas de pan">
        <Link to="/">Inicio</Link><span aria-hidden="true">/</span>
        {padre ? <><Link to={'/' + partes[0]}>{actual}</Link><span aria-hidden="true">/</span><span aria-current="page">Detalle</span></> : <span aria-current="page">{actual}</span>}
      </nav>}
      {children}
    </div>
    <footer className="tt-footer mercado-footer"><Logo /><span>{marca.lema} · Precios en COP</span></footer>
  </>;
}