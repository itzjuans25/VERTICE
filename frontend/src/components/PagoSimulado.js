import React, { useState } from 'react';
import { pesos } from '../marca';
import { Link } from 'react-router-dom';

const usd = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'USD', currencyDisplay: 'code' });

export default function PagoSimulado({ referencia, monto, referenciaUsd, pagadoEn, confirmar, certificados }) {
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [fecha, setFecha] = useState(null);
  const [codigos, setCodigos] = useState(null);
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

  async function consultar() {
    setError('');
    try { setCodigos(await certificados()); }
    catch (fallo) { setError(fallo.message); }
  }

  return <div className="pago-simulado">
    <strong role="status">{pagado ? 'Pagado' : 'Pendiente'} · {pesos.format(monto)} COP</strong>
    <small>{referencia} · Demostración académica</small>
    {referenciaUsd != null && <small>≈ {usd.format(referenciaUsd)} · Frankfurter</small>}
    {pagado ? <p>Pago simulado confirmado</p> :
      <button type="button" className="tt-button" disabled={enviando} onClick={pagar}>
        {enviando ? 'Confirmando…' : 'Confirmar pago simulado'}
      </button>}
    {pagado && certificados && codigos === null && <button type="button" className="tt-button tt-secondary" onClick={consultar}>Ver certificados</button>}
    {codigos && (codigos.length > 0 ? <ul>{codigos.map(({ codigo }) => <li key={codigo}>
      <Link to={'/verificar?codigo=' + codigo}>{codigo}</Link></li>)}</ul> : <p>Esta compra no incluye piezas certificadas.</p>)}
    {error && <p role="alert">{error}</p>}
  </div>;
}
