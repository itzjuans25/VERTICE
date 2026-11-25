import React from 'react';
import { marca } from '../marca';

export default function Logo() {
  const rectangulo = ([x,y,width,height,rx], i) => <rect key={i} {...{x,y,width,height,rx}} />;
  return <span className="marca-logo">
    <svg viewBox={marca.simbolo.vista} width="28" height="28" aria-hidden="true" focusable="false">
      <g fill="var(--tech-accent)" transform="rotate(-38 32 26)">{marca.simbolo.martillo.map(rectangulo)}</g>
      <g fill="currentColor">{rectangulo(marca.simbolo.base,0)}</g>
    </svg>
    {marca.nombre}
  </span>;
}
