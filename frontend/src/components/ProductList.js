import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaArrowRight, FaSearch, FaShoppingCart } from 'react-icons/fa';
import { catalogoApi, contarItems, imagenProducto } from '../api/catalogoApi';
import { observarDisponibilidad, textoStock } from '../api/observarDisponibilidad';
import './TechTrade.css';

export const condiciones = { NUEVO: 'Nuevo', USADO: 'Usado' };
export const precioCop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
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
      navigate('/login');
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
  const [filtros, setFiltros] = useState(inicial);
  const [aplicados, setAplicados] = useState(inicial);
  const [categorias, setCategorias] = useState([]);
  const [marcas, setMarcas] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [reintento, setReintento] = useState(0);
  const [estado, setEstado] = useState({ carga: true });
  const { agregar, ocupado, mensaje } = useAgregar({
    user, setCartCount, actualizarStock: () => setReintento(valor => valor + 1),
  });

  useEffect(() => {
    document.title = 'TechStore | Tienda de tecnología';
    catalogoApi.productos.categorias().then(setCategorias).catch(() => setCategorias([]));
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

  function aplicar(evento) {
    evento.preventDefault();
    setPagina(0);
    setAplicados({ ...filtros });
  }
  function cambiar({ target }) {
    setFiltros(valores => ({ ...valores, [target.name]: target.value }));
  }

  return <main className="tt-shell">
    <section className="tt-hero">
      <div>
        <p className="tt-kicker">Tienda de tecnología en Colombia</p>
        <h1>Encuentra tu<br /><em>próxima mejora.</em></h1>
        <p>Computadores, componentes y electrónica nueva y usada.</p>
        <div className="tt-acciones">
          <a className="tt-button" href="#explorar">Explorar catálogo <FaArrowRight aria-hidden="true" /></a>
        </div>
      </div>
      <img src="/techtrade/torre.webp" alt="Computador de escritorio iluminado en azul" />
    </section>

    <section id="explorar" className="tt-section">
      <div className="tt-section-title">
        <h2>Nuestros productos</h2>
        {estado.resultado && <span aria-live="polite">{estado.resultado.total} resultados</span>}
      </div>
      <form className="tt-filtros" onSubmit={aplicar}>
        <label className="tt-busqueda">Buscar productos
          <input name="q" type="search" placeholder="Procesador, monitor, celular…" value={filtros.q} onChange={cambiar} maxLength={120} />
        </label>
        <label>Categoría<select name="categoria" value={filtros.categoria} onChange={cambiar}>
          <option value="">Todas</option>{categorias.map(nombre => <option key={nombre}>{nombre}</option>)}
        </select></label>
        <label>Condición<select name="condicion" value={filtros.condicion} onChange={cambiar}>
          <option value="">Todas</option>{Object.entries(condiciones).map(([valor, nombre]) => <option key={valor} value={valor}>{nombre}</option>)}
        </select></label>
        <label>Marca<select name="marca" value={filtros.marca} onChange={cambiar}>
          <option value="">Todas</option>{marcas.map(marca => <option key={marca}>{marca}</option>)}
        </select></label>
        <label>Desde (COP)<input name="minimo" type="number" min="0" value={filtros.minimo} onChange={cambiar} /></label>
        <label>Hasta (COP)<input name="maximo" type="number" min="0" value={filtros.maximo} onChange={cambiar} /></label>
        <label>Orden<select name="orden" value={filtros.orden} onChange={cambiar}>
          <option value="recientes">Más recientes</option><option value="precio-asc">Menor precio</option><option value="precio-desc">Mayor precio</option>
        </select></label>
        <button className="tt-button" type="submit"><FaSearch aria-hidden="true" /> Buscar</button>
      </form>
      {mensaje && <p className="tt-aviso" role={mensaje.error ? 'alert' : 'status'}>{mensaje.texto}</p>}
      {estado.carga && <div className="tt-grid" role="status" aria-label="Cargando productos">
        {Array.from({ length: 8 }, (_, i) => <div className="tt-skeleton" key={i} aria-hidden="true" />)}
      </div>}
      {estado.error && <div className="tt-vacio" role="alert"><p>{estado.error}</p>
        <button className="tt-button" onClick={() => setReintento(reintento + 1)}>Reintentar</button></div>}
      {estado.resultado?.items.length === 0 && <div className="tt-vacio" role="status">
        <p>No hay productos que coincidan con esos filtros.</p>
        <button className="tt-button tt-secondary" onClick={() => { setFiltros(inicial); setAplicados(inicial); setPagina(0); }}>Limpiar filtros</button>
      </div>}
      <div className="tt-grid">
        {estado.resultado?.items.map(producto => <article className="tt-card product-card" key={producto.id}>
          <Link className="tt-card-image" to={'/productos/' + producto.id}>
            <img src={imagenProducto(producto.imagenUrl)} alt={producto.nombre} loading="lazy" />
          </Link>
          <div className="tt-card-body">
            <Etiquetas producto={producto} />
            <h3><Link to={'/productos/' + producto.id}>{producto.nombre}</Link></h3>
            <p className="tt-detalle-menor">{producto.marca} · {producto.garantiaDias ? producto.garantiaDias + ' días de garantía' : 'Sin garantía declarada'}</p>
            <p className="tt-detalle-menor">{textoStock(producto.stock)}</p>
            <div className="tt-card-footer">
              <strong>{precioCop.format(producto.precio)}</strong>
              <button className="tt-button tt-secondary" aria-label={(producto.stock > 0 ? 'Agregar ' : 'Agotado: ') + producto.nombre}
                onClick={() => agregar(producto)} disabled={!(producto.stock > 0) || ocupado !== null}>
                <FaShoppingCart aria-hidden="true" /> {producto.stock > 0 ? 'Agregar' : 'Agotado'}
              </button>
            </div>
          </div>
        </article>)}
      </div>
      {estado.resultado?.paginas > 1 && <nav className="tt-paginas" aria-label="Páginas del catálogo">
        <button className="tt-button tt-secondary" disabled={!pagina} onClick={() => setPagina(pagina - 1)}>Anterior</button>
        <span>Página {pagina + 1} de {estado.resultado.paginas}</span>
        <button className="tt-button tt-secondary" disabled={pagina + 1 >= estado.resultado.paginas} onClick={() => setPagina(pagina + 1)}>Siguiente</button>
      </nav>}
    </section>
  </main>;
}
