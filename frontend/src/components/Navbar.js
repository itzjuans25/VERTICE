import React, { useEffect, useState } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { FaSearch, FaShoppingCart } from 'react-icons/fa';
import Logo from './Logo';
import { isAdmin } from '../session';
import './Navbar.css';

export default function Navbar({ user, onLogout, cartCount }) {
  const [abierto, abrir] = useState(false);
  const [busqueda, setBusqueda] = useState('');
  const ubicacion = useLocation();
  const navegar = useNavigate();
  useEffect(() => abrir(false), [ubicacion.pathname, ubicacion.search]);

  function buscar(evento) {
    evento.preventDefault();
    const q = busqueda.trim();
    if (q) navegar('/buscar?' + new URLSearchParams({ q }));
  }

  return <header className="navbar">
    <div className="nav-container mercado-nav" onKeyDown={e => { if (e.key === 'Escape') abrir(false); }}>
      <Link to="/" className="nav-logo"><Logo /></Link>
      <form className="mercado-busqueda" onSubmit={buscar} role="search">
        <input type="search" aria-label="Buscar lotes de subasta" placeholder="Buscar lotes"
          value={busqueda} onChange={e => setBusqueda(e.target.value)} maxLength={120} />
        <button type="submit" aria-label="Buscar en VÉRTICE"><FaSearch aria-hidden="true" /></button>
      </form>
      <button className="tech-nav-toggle" type="button" aria-controls="mercado-navigation" aria-expanded={abierto} onClick={() => abrir(!abierto)}>{abierto ? 'Cerrar menú' : 'Menú'}</button>
      <nav id="mercado-navigation" aria-label="Navegación principal" className={'nav-menu' + (abierto ? ' tech-open' : '')}>
        <NavLink to="/" end className="nav-link">Inicio</NavLink>
        <NavLink to="/lanzamientos" className="nav-link">Lanzamientos</NavLink>
        <NavLink to="/subastas" className="nav-link nav-subastas">Subastas</NavLink>
        <NavLink to="/productos" className="nav-link">Tienda</NavLink>
        <Link to="/#como-funciona" className="nav-link">Cómo funciona</Link>
        {!isAdmin(user) && <NavLink to="/carrito" className="nav-link" aria-label="Carrito">
          <FaShoppingCart aria-hidden="true" />{cartCount > 0 && <span className="mercado-contador">{cartCount}</span>}
        </NavLink>}

        {user ? <details className="mercado-cuenta" key={ubicacion.key}>
          <summary>{user.nombre || 'Cuenta'}</summary>
          <div><Link to="/cuenta">Mi cuenta</Link>
            {isAdmin(user) && <><Link to="/admin">Administrar tienda</Link><Link to="/admin/subastas">Administrar lotes</Link></>}
            <button type="button" onClick={() => { onLogout(); abrir(false); }}>Cerrar sesión</button></div>
        </details> : <><Link to="/login" className="nav-link">Iniciar sesión</Link><Link to="/register" className="nav-link">Registrarse</Link></>}
      </nav>
    </div>
  </header>;
}
