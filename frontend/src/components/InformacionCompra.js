import React, { useEffect, useState } from 'react';
import { catalogoApi } from '../api/catalogoApi';

const dolares = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'USD',
  currencyDisplay: 'code',
});
const fechas = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'long',
  timeZone: 'UTC',
});

function formatearFecha(fecha) {
  return fechas.format(new Date(fecha + 'T00:00:00Z'));
}

export function PrecioReferencia({ id }) {
  const [estado, setEstado] = useState({});
  useEffect(() => {
    let vigente = true;
    catalogoApi.productos.precioUsd(id).then(cotizacion => {
      if (vigente) setEstado({ cotizacion });
    }).catch(() => {
      if (vigente) setEstado({ error: true });
    });
    return () => { vigente = false; };
  }, [id]);

  const { cotizacion, error } = estado;
  return <p className="tt-info-compra" role="status">
    {cotizacion ? <>
      Referencia: <strong>{dolares.format(cotizacion.precioUsd)}</strong>
      <small>Frankfurter · Tasa del {formatearFecha(cotizacion.fechaTasa)}. El pedido se registra en COP.</small>
    </> : error ? 'Conversión a USD no disponible por el momento.' : 'Consultando referencia en USD…'}
  </p>;
}

export function EntregaEstimada() {
  const [estado, setEstado] = useState({});
  useEffect(() => {
    let vigente = true;
    catalogoApi.productos.entrega(3).then(entrega => {
      if (vigente) setEstado({ entrega });
    }).catch(() => {
      if (vigente) setEstado({ error: true });
    });
    return () => { vigente = false; };
  }, []);

  const { entrega, error } = estado;
  return <p className="tt-info-compra" role="status">
    {entrega ? <>
      Entrega estimada: <strong>{formatearFecha(entrega.fechaEstimada)}</strong>
      <small>{entrega.diasHabiles} días hábiles. {entrega.incluyeFestivos
        ? 'Excluye fines de semana y festivos de Colombia · Nager.Date.'
        : 'Calendario parcial: pueden faltar festivos de Colombia.'}</small>
    </> : error ? 'Fecha de entrega no disponible por el momento.' : 'Calculando fecha de entrega…'}
  </p>;
}
