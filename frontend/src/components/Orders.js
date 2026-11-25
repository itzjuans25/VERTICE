import { tituloMarca } from '../marca';
import React, { useCallback, useEffect, useState } from 'react';
import './Orders.css';
import { catalogoApi } from '../api/catalogoApi';
import { Link } from 'react-router-dom';
import { FaBoxOpen } from 'react-icons/fa';
import PagoSimulado from './PagoSimulado';

function Orders({ user }) {
  useEffect(() => { document.title = tituloMarca('Mis compras'); }, []);
  const [compras, setCompras] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchCompras = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await catalogoApi.compras.listarPorUsuario(user.id);
      setCompras(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Error al cargar las compras');
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    fetchCompras();
  }, [fetchCompras]);

  if (loading) {
    return (
      <div className="loading">
        <div className="spinner"></div>
        Cargando compras...
      </div>
    );
  }

  if (error) {
    return (
      <div className="orders-error">
        <p>{error}</p>
        <button type="button" className="btn-retry-orders" onClick={fetchCompras}>
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <main className="orders-container">
      <h1 className="page-title">Mis compras</h1>

      {compras.length === 0 ? (
        <div className="empty-orders">
          <FaBoxOpen className="empty-icon" aria-hidden="true" />
          <h2>No tienes compras aún</h2>
          <p>Realiza tu primera compra y aparecerá aquí</p>
          <Link to="/productos">Explorar el catálogo</Link>
        </div>
      ) : (
        <div className="orders-list">
          {compras.map((compra) => (
            <div key={compra.id} className="order-card">
              <div className="order-header">
                <div>
                  <h3>Compra #{compra.id}</h3>
                  <p className="order-date">
                    {new Date(compra.fechaCompra).toLocaleDateString('es-ES', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </p>
                  {compra.estado && (
                    <span className="order-status">{compra.estado}</span>
                  )}
                </div>
                <div className="order-total">
                  <span className="total-label">Total</span>
                  <span className="total-amount">
                    {Number(compra.total).toLocaleString('es-CO')} COP
                  </span>
                </div>
              </div>

              {compra.pagoSimulado && <PagoSimulado
                referencia={compra.pagoSimulado.referencia} monto={compra.total}
                referenciaUsd={compra.referenciaUsd} pagadoEn={compra.pagoSimulado.pagadoEn}
                confirmar={async () => (await catalogoApi.compras.pagar(compra.id)).pagoSimulado.pagadoEn} />}
              <div className="order-items">
                <h4>Productos</h4>
                {(compra.items || []).map((item, index) => (
                  <div
                    key={item.id || `${compra.id}-${index}`}
                    className="order-item"
                  >
                    <span className="item-name">{item.nombreProducto}</span>
                    <span className="item-quantity">x{item.cantidad}</span>
                    <span className="item-price">
                      {(
                        Number(item.precioUnitario) * item.cantidad
                      ).toLocaleString('es-CO')}{' '}
                      COP
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </main>
  );
}

export default Orders;
