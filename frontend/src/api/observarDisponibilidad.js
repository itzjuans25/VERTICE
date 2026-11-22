export function observarDisponibilidad(actualizar) {
  function actualizarVisible() {
    if (document.visibilityState === 'visible') actualizar();
  }

  window.addEventListener('focus', actualizarVisible);
  document.addEventListener('visibilitychange', actualizarVisible);
  const intervalo = window.setInterval(actualizarVisible, 15000);
  return () => {
    window.removeEventListener('focus', actualizarVisible);
    document.removeEventListener('visibilitychange', actualizarVisible);
    window.clearInterval(intervalo);
  };
}

export function textoStock(stock) {
  if (!(stock > 0)) return 'Agotado';
  return stock === 1 ? '1 unidad disponible' : `${stock} unidades disponibles`;
}
