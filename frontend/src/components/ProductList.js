import { tituloMarca, pesos } from '../marca';
import { categoriasTienda, categoriaPorSlug } from '../categorias';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiltrosMercado, TarjetaMercado, Paginacion, EstadoMercado } from './MercadoUI';
import { catalogoApi, contarItems } from '../api/catalogoApi';
import { observarDisponibilidad } from '../api/observarDisponibilidad';
import { useMercado } from './ExplorarMercado';
import './TechTrade.css';

export const condiciones = { NUEVO: 'Nuevo', USADO: 'Usado' };
export const precioCop = pesos;
const inicial = { q: '', categoria: '', condicion: '', marca: '', minimo: '', maximo: '', orden: 'recientes' };

export function Etiquetas({ producto }) {
  return <div className="tt-etiquetas">
    <span className="tt-condicion">{condiciones[producto.condicion] || 'Nuevo'}</span>
    {producto.condicion === "USADO" && producto.grado && <span>Grado {producto.grado}</span>}
  </div>;
}

export function useAgregar({ user, setCartCount, actualizarStock }) {
  const [ocupado, setOcupado] = useState(null);
  const [mensaje, setMensaje] = useState(null);
  const navigate = useNavigate();
  async function agregar(producto) {
    if (!user) {
      navigate('/login', { state: { volver: '/productos' } });
      return;
    }
    setOcupado(producto.id);
    setMensaje(null);
    try {
      const carrito = await catalogoApi.carrito.agregar(producto.id, 1);
      setCartCount(contarItems(carrito));
      setMensaje({ texto: 'Producto agregado al carrito.' });
    } catch (error) {
      setMensaje({ texto: error.message, error: true });
      if (error.status === 400 || error.status === 404) actualizarStock();
    } finally {
      setOcupado(null);
    }
  }
  return { agregar, ocupado, mensaje };
}

export default function ProductList({ user, setCartCount }) {
  const [reinicio, setReinicio] = useState(0);
  const [aplicados, setAplicados] = useState(inicial);
  const [marcas, setMarcas] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [reintento, setReintento] = useState(0);
  const [estado, setEstado] = useState({ carga: true });
  const [lanzamientos] = useMercado(catalogoApi.lanzamientos);
  const proximos = new Set(lanzamientos.datos?.filter(e => Date.parse(e.apertura) > Date.now()).map(e => e.productoId));
  const { agregar, ocupado, mensaje } = useAgregar({
    user, setCartCount, actualizarStock: () => setReintento(valor => valor + 1),
  });

  useEffect(() => {
    document.title = tituloMarca();
    catalogoApi.productos.marcas().then(setMarcas).catch(() => setMarcas([]));
  }, []);
  useEffect(() => {
    let vigente = true;
    let consultando = false;
    setEstado({ carga: true });
    async function consultar(inicial) {
      if (consultando) return;
      consultando = true;
      try {
        const resultado = await catalogoApi.productos.explorar({ ...aplicados, pagina, tamano: 12 });
        if (vigente) setEstado({ resultado });
      } catch (error) {
        if (vigente && inicial) setEstado({ error: error.message });
      } finally {
        consultando = false;
      }
    }
    consultar(true);
    const dejarDeObservar = observarDisponibilidad(() => consultar(false));
    return () => { vigente = false; dejarDeObservar(); };
  }, [aplicados, pagina, reintento]);

  function aplicar(filtros) {
    setPagina(0);
    setAplicados({...filtros, categoria:categoriaPorSlug(filtros.categoria)?.tienda.join(',') || ''});
  }
  function limpiar() { aplicar(inicial); setReinicio(v => v+1); }

  return <main className="tt-shell">
    <h1>Tienda</h1>

    <section id="explorar" className="tt-section">
      <div className="tt-section-title">
        <h2>Nuestros productos</h2>
        {estado.resultado && <span aria-live="polite">{estado.resultado.total} resultados</span>}
      </div>
      <FiltrosMercado key={reinicio} categorias={categoriasTienda.map(c => [c.slug,c.nombre])} marcas={marcas} alBuscar={aplicar} alLimpiar={limpiar} />
      {mensaje && <p className="tt-aviso" role={mensaje.error ? 'alert' : 'status'}>{mensaje.texto}</p>}
      <EstadoMercado estado={{ cargando:estado.carga, error:estado.error }} reintentar={() => setReintento(v => v+1)}
        vacio={estado.resultado?.items.length === 0 ? 'No hay productos con estos filtros.' : ''} />
      <div className="tt-grid">{estado.resultado?.items.map(producto => <TarjetaMercado key={producto.id} producto={producto} agregar={agregar} ocupado={ocupado !== null} proximo={proximos.has(producto.id)} />)}</div>
      <Paginacion pagina={pagina} paginas={estado.resultado?.paginas || 0} cambiar={setPagina} />
    </section>
  </main>;
}
