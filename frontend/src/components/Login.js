import { tituloMarca } from '../marca';
import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import './Auth.css';
import { authApi } from '../api/authApi';
import { isAdmin, saveSession } from '../session';

function Login({ onLogin }) {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const successMessage = new URLSearchParams(location.search).get('clave') === 'actualizada'
    ? 'Contraseña actualizada. Inicia sesión con la nueva.'
    : location.state?.message || '';
  useEffect(() => { document.title = tituloMarca('Iniciar sesión'); }, []);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const respuesta = await authApi.login({
        email: formData.email.trim(),
        password: formData.password,
      });
      const session = saveSession(respuesta);

      onLogin(session.user);
      const volver = location.state?.volver;
      const destino = typeof volver === 'string' && /^\/[a-z][a-z0-9/-]*(?:\?[^#\r\n]*)?$/.test(volver) ? volver : '/subastas';
      navigate(isAdmin(session.user) ? '/admin' : destino, {
        replace: true,
      });
    } catch (err) {
      setError(err.message || 'No se pudo iniciar sesion');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-container">
      <div className="auth-card">
        <h1>Iniciar sesión</h1>
        <p className="auth-subtitle">Bienvenido de nuevo</p>

        {successMessage && (
          <div className="success-message" role="status">{successMessage}</div>
        )}
        {error && <div className="error-message" role="alert">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="login-email">Correo electrónico</label>
            <input
              type="email"
              id="login-email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="tu@email.com"
              autoComplete="email"
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Contraseña</label>
            <input
              type="password"
              id="login-password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="••••••••"
              autoComplete="current-password"
              required
            />
          </div>

          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Iniciando...' : 'Iniciar Sesión'}
          </button>
        </form>

        <p className="auth-footer">
          ¿No tienes cuenta? <Link to="/register">Regístrate aquí</Link>
        </p>
      </div>
    </main>
  );
}

export default Login;
