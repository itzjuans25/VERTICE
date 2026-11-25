import React, { useState } from 'react';
import { pesos } from '../marca';

const usd = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'USD', currencyDisplay: 'code' });

export default function PagoSimulado({ referencia, monto, referenciaUsd, pagadoEn, confirmar }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [fecha, setFecha] = useState(null);
  const pagado = Boolean(pagadoEn || fecha);

  async function pagar() {
    setEnviando(true);
    setError('');
    try {
      const respuesta = await confirmar();
      setFecha(respuesta);
    } catch (fallo) {
      setError(fallo.message);
    } finally {
      setEnviando(false);
    }
  }

  return <div className="pago-simulado">
    <strong role="status">{pagado ? 'Pagado' : 'Pendiente'} · {pesos.format(monto)} COP</strong>
    <small>{referencia} · Demostración académica</small>
    {referenciaUsd != null && <small>≈ {usd.format(referenciaUsd)} · Frankfurter</small>}
    {pagado ? <p>Pago simulado confirmado</p> :
      <button type="button" className="tt-button" disabled={enviando} onClick={pagar}>
        {enviando ? 'Confirmando…' : 'Confirmar pago simulado'}
      </button>}
    {error && <p role="alert">{error}</p>}
  </div>;
}
