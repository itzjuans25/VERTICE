import React from 'react';
import { marca } from '../marca';

export default function Logo() {
  return <span className="marca-logo">
    <svg viewBox={marca.simbolo.vista} width="28" height="28" aria-hidden="true" focusable="false">
      <path d={marca.simbolo.trazo} fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinejoin="round" />
    </svg>
    {marca.nombre}
  </span>;
}