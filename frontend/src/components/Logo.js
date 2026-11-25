import React from 'react';
import { marca } from '../marca';

export default function Logo() {
  return <span className="marca-logo">
    <svg viewBox="0 0 24 24" width="28" height="28" aria-hidden="true" focusable="false">
      <path d={marca.simbolo.etiqueta} fill="var(--tech-accent)" />
      <path d={marca.simbolo.trazo} fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
    {marca.nombre}
  </span>;
}