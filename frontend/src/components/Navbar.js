import React, { useEffect, useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import './Navbar.css';
import {
  FaMicrochip,
  FaStore,
  FaShoppingCart,
  FaBox,
  FaCog,
  FaUser,
  FaSignOutAlt,
  FaSignInAlt,
  FaUserPlus,
} from 'react-icons/fa';
import { isAdmin } from '../session';

function Navbar({ user, onLogout, cartCount }) {
  const admin = isAdmin(user);
  const [abierto, abrir] = useState(false);
  const { pathname, search } = useLocation();
  useEffect(() => abrir(false), [pathname, search]);

  return (
    <nav className="navbar">
      <div className="nav-container">
        <Link to="/" className="nav-logo">
          <FaMicrochip aria-hidden="true" /> TechStore
        </Link>

        <button className="tech-nav-toggle" type="button" aria-controls="tech-navigation" aria-expanded={abierto} onClick={() => abrir(!abierto)}>
          {abierto ? 'Cerrar menú' : 'Menú'}
        </button>
        <div id="tech-navigation" className={'nav-menu' + (abierto ? ' tech-open' : '')}>
          <NavLink to="/subastas" className="nav-link">Subastas</NavLink>
          {user && <NavLink to="/mis-pujas" className="nav-link">Mis pujas</NavLink>}
          {admin && <NavLink to="/admin/subastas" className="nav-link">Administrar lotes</NavLink>}
          {user ? (
            <>
              {admin ? (
                <>
                  <Link to="/admin" className="nav-link">
                    <FaCog /> Panel Admin
                  </Link>
                  <Link to="/productos" className="nav-link">
                    <FaStore /> Ver Tienda
                  </Link>
                </>
              ) : (
                <>
                  <Link to="/productos" className="nav-link">
                    <FaStore /> Productos
                  </Link>
                  <Link to="/carrito" className="nav-link cart-link">
                    <FaShoppingCart /> Carrito
                    {cartCount > 0 && (
                      <span className="cart-badge">{cartCount}</span>
                    )}
                  </Link>
                  <Link to="/compras" className="nav-link">
                    <FaBox /> Mis Compras
                  </Link>
                </>
              )}
              <span className="nav-user">
                <FaUser /> {user.nombre}
              </span>
              <Link to="/cuenta/contrasena" className="nav-link">Cambiar contraseña</Link>
              <button
                type="button"
                onClick={onLogout}
                className="nav-btn logout-btn"
              >
                <FaSignOutAlt /> Cerrar Sesión
              </button>
            </>
          ) : (
            <>
              <Link to="/productos" className="nav-link"><FaStore /> Catálogo</Link>
              <Link to="/login" className="nav-btn">
                <FaSignInAlt /> Iniciar Sesión
              </Link>
              <Link to="/register" className="nav-btn register-btn">
                <FaUserPlus /> Registrarse
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
