import React, { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { catalogoApi } from '../api/catalogoApi';
import { subastasApi } from '../api/subastasApi';
import { categoriasMercado } from '../categorias';
import { tituloMarca } from '../marca';
import { TarjetaMercado, EstadoMercado } from './MercadoUI';

export function useMercado(cargar) {
  const [estado, setEstado] = useState({ cargando: true });
  const [version, reintentar] = useState(0);
  useEffect(() => {
    let vigente = true;
    setEstado({ cargando: true });
    cargar().then(datos => { if (vigente) setEstado({ datos }); })
      .catch(error => { if (vigente) setEstado({ error: error.message }); });
    return () => { vigente = false; };
  }, [cargar, version]);
  return [estado, () => reintentar(v => v + 1)];
}

async function productosDe(categoria, n) {
  const disponibles = await catalogoApi.productos.categorias();
  const listas = await Promise.all(categoria.tienda.filter(nombre => disponibles.includes(nombre)).map(nombre =>
    catalogoApi.productos.explorar({ categoria: nombre, tamano: n, orden: 'recientes' })));
  return listas.flatMap(lista => lista.items).sort((a,b) => b.id-a.id).slice(0,n);
}

export function Recomendados({ categoria, lado }) {
  const cargar = React.useCallback(() => {
    if (!categoria) return Promise.resolve([]);
    if (lado === 'subastas') return subastasApi.listar({ categoria: categoria.lote, tamano: 3 }).then(p => p.items);
    return productosDe(categoria, 3);
  }, [categoria, lado]);
  const [estado] = useMercado(cargar);
  if (!estado.datos?.length) return null;
  const subastas = lado === 'subastas';
  return <section className="mercado-seccion"><h2>{subastas ? 'También en subasta' : 'También en tienda'}</h2>
    <div className="tt-grid">{estado.datos.map(item => <TarjetaMercado key={item.id} {...(subastas ? { lote: item } : { producto: item })} />)}</div>
    <Link to={'/categoria/' + categoria.slug + (subastas ? '?ver=subastas' : '')}>Ver {categoria.nombre}</Link>
  </section>;
}

export function BuscarMercado() {
  const [params] = useSearchParams();
  const q = params.get('q')?.trim() || '';
  const productos = React.useCallback(() => q ? catalogoApi.productos.explorar({ q: q.slice(0,120), tamano: 12 }) : Promise.resolve({ items: [], total: 0 }), [q]);
  const lotes = React.useCallback(() => q ? subastasApi.listar({ q: q.slice(0,100), tamano: 12 }) : Promise.resolve({ items: [], total: 0 }), [q]);
  return <main className="tt-shell mercado-pagina">
    <h1>Buscar</h1>{q ? <p>Resultados para «{q}»</p> : <p>Escribe el nombre de un producto o lote en el buscador.</p>}
    {q && <><Resultados nombre="Productos" cargar={productos} />
    <Resultados nombre="Subastas" cargar={lotes} subastas /></>}
  </main>;
}

function Resultados({ nombre, cargar, subastas }) {
  const [estado, reintentar] = useMercado(cargar);
  return <section className="mercado-seccion"><h2>{nombre}{estado.datos && ' · ' + estado.datos.total}</h2>
    <EstadoMercado estado={estado} reintentar={reintentar} vacio={estado.datos?.total === 0 ? 'No encontramos resultados.' : ''} />
    <div className="tt-grid">{estado.datos?.items.map(item => <TarjetaMercado key={item.id} {...(subastas ? { lote: item } : { producto: item })} />)}</div>
  </section>;
}

export function CategoriaMercado() {
  const { slug } = useParams();
  const [parametros, cambiar] = useSearchParams();
  const categoria = categoriasMercado.find(c => c.slug === slug);
  const subastas = parametros.get('ver') === 'subastas';
  const cargar = React.useCallback(async () => {
    if (!categoria) return { items: [], total: 0 };
    if (subastas) return subastasApi.listar({ categoria: categoria.lote, tamano: 24 });
    const items = await productosDe(categoria, 24);
    return { items, total: items.length };
  }, [categoria, subastas]);
  useEffect(() => { document.title = tituloMarca(categoria?.nombre); }, [categoria]);
  if (!categoria) return <main className="tt-shell mercado-pagina"><h1>Categoría no encontrada</h1><Link to="/">Ver categorías</Link></main>;
  return <main className="tt-shell mercado-pagina"><h1>{categoria.nombre}</h1>
    <nav className="mercado-tabs" aria-label="Canales de la categoría">
      <button aria-pressed={!subastas} onClick={() => cambiar({})}>En tienda</button>
      <button aria-pressed={subastas} onClick={() => cambiar({ ver: 'subastas' })}>En subasta</button>
    </nav>
    <Resultados nombre={subastas ? 'Lotes' : 'Productos'} cargar={cargar} subastas={subastas} />
  </main>;
}
