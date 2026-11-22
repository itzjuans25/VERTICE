import React, { useEffect, useState } from 'react';
import { authApi } from '../api/authApi';
import './Auth.css';

export default function CambiarClave({ cerrarSesion }) {
  const [error, setError] = useState('');
  const [guardando, setGuardando] = useState(false);
  useEffect(() => { document.title = 'Cambiar contraseña | TechStore'; }, []);

  async function guardar(evento) {
    evento.preventDefault();
    setError('');
    const datos = new FormData(evento.currentTarget);
    const nueva = datos.get('nueva');
    if (nueva !== datos.get('confirmacion')) {
      setError('Las contraseñas nuevas no coinciden.');
      return;
    }
    setGuardando(true);
    try {
      await authApi.cambiarClave(datos.get('actual'), nueva);
      cerrarSesion();
      window.location.replace('/login?clave=actualizada');
    } catch (fallo) {
      setError(fallo.message || 'No se pudo cambiar la contraseña.');
    } finally {
      setGuardando(false);
    }
  }

  return <main className="auth-container">
    <section className="auth-card">
      <h2>Cambiar contraseña</h2>
      {error && <p className="error-message" role="alert">{error}</p>}
      <form onSubmit={guardar}>
        <div className="form-group">
          <label htmlFor="clave-actual">Contraseña actual</label>
          <input id="clave-actual" name="actual" type="password" autoComplete="current-password" maxLength={72} required />
        </div>
        <div className="form-group">
          <label htmlFor="clave-nueva">Contraseña nueva</label>
          <input id="clave-nueva" name="nueva" type="password" autoComplete="new-password" minLength={6} maxLength={72} required />
        </div>
        <div className="form-group">
          <label htmlFor="clave-confirmacion">Confirmar contraseña nueva</label>
          <input id="clave-confirmacion" name="confirmacion" type="password" autoComplete="new-password" maxLength={72} required />
        </div>
        <button className="btn-primary" disabled={guardando}>{guardando ? 'Guardando…' : 'Guardar contraseña'}</button>
      </form>
    </section>
  </main>;
}
