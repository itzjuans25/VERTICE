import { tituloMarca } from '../marca';
import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { catalogoApi, imagenProducto } from '../api/catalogoApi';
import { observarDisponibilidad, textoStock } from '../api/observarDisponibilidad';
import { Etiquetas, precioCop, useAgregar } from './ProductList';
import { PrecioReferencia } from './InformacionCompra';

export default function ProductDetail({ user, setCartCount }) {
  const { id } = useParams();
  const [estado, setEstado] = useState({});
  const [seleccion, setSeleccion] = useState(0);
  const [reintento, setReintento] = useState(0);
  const { agregar, ocupado, mensaje } = useAgregar({
    user, setCartCount, actualizarStock: () => setReintento(valor => valor + 1),
  });

  useEffect(() => {
    let vigente = true;
    let consultando = false;
    async function consultar(inicial) {
      if (consultando) return;
      consultando = true;
      try {
        const producto = await catalogoApi.productos.obtener(id);
        if (vigente) {
          setEstado({ producto });
          document.title = tituloMarca(producto.nombre);
        }
      } catch (error) {
        if (vigente && (inicial || error.status === 404)) setEstado({ error: error.message });
      } finally {
        consultando = false;
      }
    }
    consultar(true);
    const dejarDeObservar = observarDisponibilidad(() => consultar(false));
    return () => { vigente = false; dejarDeObservar(); };
  }, [id, reintento]);

  useEffect(() => { setSeleccion(0); }, [id]);

  const { producto, error } = estado;
  if (!producto || String(producto.id) !== id) return <main className="tt-shell tt-section">
    <Link to="/productos">Volver al catálogo</Link>
    <p role={error ? 'alert' : 'status'}>{error || 'Cargando producto…'}</p>
  </main>;

  const fotos = [producto.imagenUrl, producto.imagenSecundariaUrl].filter(Boolean);
  if (!fotos.length) fotos.push(null);
  const disponible = producto.stock > 0;
  return <main className="tt-shell tt-section">
    <Link to="/productos">Volver al catálogo</Link>
    <div className="tt-detail">
      <div>
        <img className="tt-detail-image" src={imagenProducto(fotos[seleccion])} alt={producto.nombre + (seleccion ? ', segunda vista ilustrativa' : ', vista principal ilustrativa')} />
        {fotos.length > 1 && <div className="tt-thumbs" aria-label="Galería">
          {fotos.map((foto, i) => <button key={foto} onClick={() => setSeleccion(i)} aria-pressed={seleccion === i}
            aria-label={'Ver foto ' + (i + 1)}><img src={imagenProducto(foto)} alt="" /></button>)}
        </div>}
      </div>
      <section>
        <Etiquetas producto={producto} />
        <h1>{producto.nombre}</h1>
        <p>{producto.descripcion}</p>
        {producto.descripcionLarga && <p>{producto.descripcionLarga}</p>}
        <strong className="tt-detail-price">{precioCop.format(producto.precio)}</strong>
        <PrecioReferencia key={producto.id + '-' + producto.precio} id={producto.id} />
        <dl className="tt-specs">
          <div><dt>Categoría</dt><dd>{producto.categoria || 'Otros'}</dd></div>
          <div><dt>Marca</dt><dd>{producto.marca}</dd></div>
          {producto.modelo && <div><dt>Modelo</dt><dd>{producto.modelo}</dd></div>}
          <div><dt>Garantía</dt><dd>{producto.garantiaDias ? producto.garantiaDias + ' días' : 'Sin garantía declarada'}</dd></div>
          <div><dt>Stock</dt><dd>{textoStock(producto.stock)}</dd></div>
        </dl>
        {producto.incluye && <p><strong>Incluye</strong> {producto.incluye}</p>}
        {producto.compatibilidad && <p><strong>Compatibilidad</strong> {producto.compatibilidad}</p>}
        {producto.notaEstado && <p><strong>Estado del usado</strong> {producto.notaEstado}</p>}
        {Object.keys(producto.especificaciones || {}).length > 0 && <table className="tt-spec-table">
          <caption>Especificaciones técnicas</caption>
          <tbody>{Object.entries(producto.especificaciones).map(([clave, valor]) =>
            <tr key={clave}><th scope="row">{clave}</th><td>{valor}</td></tr>)}</tbody>
        </table>}
        <button className="tt-button" onClick={() => agregar(producto)} disabled={!disponible || ocupado !== null}>
          {disponible ? 'Agregar al carrito' : 'Agotado'}
        </button>
        {mensaje && <p className="tt-aviso" role={mensaje.error ? 'alert' : 'status'}>{mensaje.texto}</p>}
      </section>
    </div>
  </main>;
}
