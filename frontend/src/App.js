import React, { lazy, Suspense, useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Link, useLocation, useSearchParams } from 'react-router-dom';
import './App.css';
import Login from './components/Login';
import Register from './components/Register';
import ProductList from './components/ProductList';
import ProductDetail from './components/ProductDetail';
import CambiarClave from './components/CambiarClave';
import Cart from './components/Cart';
import Layout from './components/Layout';
import Cuenta, { Avisos, Seguridad } from './components/Cuenta';
import { Lanzamientos } from './components/Ediciones';
import AdminPanel from './components/AdminPanel';
import Orders from './components/Orders';
import { clearSession, getSession, isAdmin } from './session';
import { catalogoApi, contarItems } from './api/catalogoApi';
import { SESSION_EXPIRED_EVENT } from './api/client';
import './tokens.css';
import './mercado.css';

const rutaSubastas = nombre => lazy(() => import('./components/Subastas').then(modulo => ({ default: modulo[nombre] })));
const Inicio = rutaSubastas('Inicio');
const Subastas = rutaSubastas('Lista');
const Detalle = rutaSubastas('Detalle');
const MisPujas = rutaSubastas('MisPujas');
const AdminSubastas = rutaSubastas('AdminSubastas');
const NoEncontrado = rutaSubastas('NoEncontrado');
const Buscar = lazy(() => import('./components/ExplorarMercado').then(modulo => ({ default: modulo.BuscarMercado })));
const Categoria = lazy(() => import('./components/ExplorarMercado').then(modulo => ({ default: modulo.CategoriaMercado })));

function inicioPara(user) {
  return isAdmin(user) ? '/admin' : '/subastas';
}

function RutaPrivada({ user, children }) {
  const { pathname, search } = useLocation();
  return user ? children : <Navigate to="/login" state={{ volver: pathname + search }} replace />;
}

function RutaAdmin({ user, children }) {
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return isAdmin(user) ? children : <Navigate to="/productos" replace />;
}

function RutaCuenta({ user, children }) {
  return <RutaPrivada user={user}><Cuenta>{children}</Cuenta></RutaPrivada>;
}

const vistasActividad = [['pujas', 'Pujas'], ['ganadas', 'Subastas ganadas'], ['compras', 'Compras']];

function Actividad({ user }) {
  const [params] = useSearchParams();
  const elegida = params.get('vista');
  const vista = vistasActividad.some(([id]) => id === elegida) ? elegida : 'pujas';
  return <><nav className="mercado-tabs actividad-tabs" aria-label="Actividad de mi cuenta">
    {vistasActividad.map(([id, titulo]) => <Link key={id} to={'/cuenta?vista=' + id}
      aria-current={vista === id ? 'page' : undefined}>{titulo}</Link>)}
  </nav>{vista === 'compras' ? <Orders user={user} /> : <MisPujas vista={vista} />}</>;
}

function App() {
  const [user, setUser] = useState(null);
  const [inicializando, setInicializando] = useState(true);
  const [cartCount, setCartCount] = useState(0);

  useEffect(() => {
    const session = getSession();
    setUser(session?.user || null);
    setInicializando(false);

    const handleSessionExpired = () => {
      setUser(null);
      setCartCount(0);
    };

    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    return () =>
      window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
  }, []);

  useEffect(() => {
    if (!user || isAdmin(user)) {
      setCartCount(0);
      return undefined;
    }

    let activo = true;
    catalogoApi.carrito
      .obtener()
      .then((carrito) => {
        if (activo) {
          setCartCount(contarItems(carrito));
        }
      })
      .catch(() => {
        if (activo) {
          setCartCount(0);
        }
      });

    return () => {
      activo = false;
    };
  }, [user]);

  const handleLogin = (loggedUser) => {
    setUser(loggedUser);
  };

  const handleLogout = () => {
    clearSession();
    setUser(null);
    setCartCount(0);
  };

  if (inicializando) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        Cargando...
      </div>
    );
  }

  return (
    <Router>
      <div className="App">
        <Layout user={user} onLogout={handleLogout} cartCount={cartCount}>
        <Suspense fallback={<div className="loading" role="status">Cargando…</div>}>
        <Routes>
          <Route
            path="/login"
            element={
              user ? (
                <Navigate to={inicioPara(user)} replace />
              ) : (
                <Login onLogin={handleLogin} />
              )
            }
          />

          <Route
            path="/register"
            element={
              user ? (
                <Navigate to={inicioPara(user)} replace />
              ) : (
                <Register />
              )
            }
          />

          <Route
            path="/admin"
            element={
              <RutaAdmin user={user}>
                <AdminPanel />
              </RutaAdmin>
            }
          />

          <Route
            path="/productos"
            element={<ProductList user={user} setCartCount={setCartCount} />}
          />
          <Route path="/productos/:id" element={<ProductDetail user={user} setCartCount={setCartCount} />} />
          <Route path="/cuenta/contrasena" element={<Navigate to="/cuenta/seguridad" replace />} />
          <Route path="/cuenta/seguridad" element={<RutaCuenta user={user}><Seguridad user={user}><CambiarClave cerrarSesion={handleLogout} /></Seguridad></RutaCuenta>} />
          <Route path="/cuenta" element={<RutaCuenta user={user}><Actividad user={user} /></RutaCuenta>} />
          <Route path="/cuenta/avisos" element={<RutaCuenta user={user}><Avisos /></RutaCuenta>} />
          <Route path="/cuenta/ganadas" element={<Navigate to="/cuenta?vista=ganadas" replace />} />

          <Route
            path="/carrito"
            element={
              <RutaPrivada user={user}>
                <Cart setCartCount={setCartCount} />
              </RutaPrivada>
            }
          />

          <Route
            path="/compras"
            element={<Navigate to="/cuenta?vista=compras" replace />}
          />

          <Route path="/" element={<Inicio />} />
          <Route path="/lanzamientos" element={<Lanzamientos />} />
          <Route path="/buscar" element={<Buscar />} />
          <Route path="/categoria/:slug" element={<Categoria />} />
          <Route path="/subastas" element={<Subastas />} />
          <Route path="/subastas/:id" element={<Detalle user={user} />} />
          <Route path="/mis-pujas" element={<Navigate to="/cuenta?vista=pujas" replace />} />
          <Route path="/admin/subastas" element={<RutaAdmin user={user}><AdminSubastas /></RutaAdmin>} />
          <Route path="*" element={<NoEncontrado />} />
        </Routes>
        </Suspense>
        </Layout>
      </div>
    </Router>
  );
}

export default App;
