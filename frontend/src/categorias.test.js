import { categoriaLote, categoriaProducto, destacadosPorCategoria } from './categorias';

test('agrupa vehículos sin clasificar los completos como productos de compra directa', () => {
  expect(categoriaLote('CARROS')).toBe(categoriaLote('MOTOS'));
  expect(categoriaProducto('Accesorios de moto')).toBe(categoriaLote('MOTOS'));
  expect(categoriaProducto('Carros')).toBeUndefined();
});

test('distribuye categorías antes de repetir y limita a dos por categoría', () => {
  const items = ['Procesadores','Memoria RAM','Placas base','Relojes','Relojes','Hogar y cocina'].map((categoria,id) => ({ id,categoria }));
  const resultado = destacadosPorCategoria(items,new Date('2026-11-24T20:00:00Z'));
  expect(resultado).toHaveLength(5);
  expect(resultado.filter(p => categoriaProducto(p.categoria).slug === 'tecnologia')).toHaveLength(2);
});