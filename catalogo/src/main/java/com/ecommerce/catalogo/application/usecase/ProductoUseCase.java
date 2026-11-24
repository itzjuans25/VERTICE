package com.ecommerce.catalogo.application.usecase;

import com.ecommerce.catalogo.application.port.in.ProductoService;
import com.ecommerce.catalogo.domain.exception.DatosInvalidosException;
import com.ecommerce.catalogo.domain.exception.ProductoNoEncontradoException;
import com.ecommerce.catalogo.domain.exception.StockInsuficienteException;
import com.ecommerce.catalogo.domain.model.Producto;
import com.ecommerce.catalogo.domain.port.out.ProductoGateway;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
public class ProductoUseCase implements ProductoService {

    private final ProductoGateway productoGateway;

    @Override
    public Producto guardarProducto(Producto producto) {
        if (producto == null) {
            throw new DatosInvalidosException("Los datos del producto son obligatorios");
        }
        producto.setId(null);
        completarDatos(producto, new Producto());
        validar(producto);
        return productoGateway.guardar(producto);
    }

    @Override
    public Producto obtenerProducto(Long id) {
        if (id == null) {
            throw new DatosInvalidosException("El id del producto es obligatorio");
        }
        Producto producto = productoGateway.buscarPorId(id);
        if (producto == null) {
            throw new ProductoNoEncontradoException("Producto no encontrado con ID: " + id);
        }
        return producto;
    }

    @Override
    public List<Producto> obtenerTodos() {
        return productoGateway.obtenerTodos();
    }

    @Override
    public Producto actualizarProducto(Producto producto) {
        if (producto == null || producto.getId() == null) {
            throw new DatosInvalidosException("El id del producto es obligatorio");
        }
        Producto anterior = obtenerProducto(producto.getId());
        if (producto.getCategoria() == null) {
            producto.setCategoria(anterior.getCategoria());
        }
        completarDatos(producto, anterior);
        validar(producto);
        return productoGateway.guardar(producto);
    }

    private static void completarDatos(Producto producto, Producto base) {
        producto.setCondicion(java.util.Objects.requireNonNullElse(producto.getCondicion(), base.getCondicion()));
        producto.setMarca(java.util.Objects.requireNonNullElse(producto.getMarca(), base.getMarca()));
        producto.setGarantiaDias(java.util.Objects.requireNonNullElse(producto.getGarantiaDias(), base.getGarantiaDias()));
        if ("NUEVO".equals(producto.getCondicion())) {
            if (producto.getGrado() != null) {
                throw new DatosInvalidosException("El grado solo se asigna a artículos usados");
            }
            producto.setNotaEstado("");
        } else if (producto.getGrado() == null) {
            producto.setGrado(base.getGrado());
        }
        producto.setModelo(producto.getModelo() == null ? base.getModelo() : producto.getModelo());
        producto.setDescripcionLarga(producto.getDescripcionLarga() == null ? base.getDescripcionLarga() : producto.getDescripcionLarga());
        producto.setIncluye(producto.getIncluye() == null ? base.getIncluye() : producto.getIncluye());
        producto.setCompatibilidad(producto.getCompatibilidad() == null ? base.getCompatibilidad() : producto.getCompatibilidad());
        producto.setNotaEstado(producto.getNotaEstado() == null ? base.getNotaEstado() : producto.getNotaEstado());
        producto.setImagenSecundariaUrl(producto.getImagenSecundariaUrl() == null ? base.getImagenSecundariaUrl() : producto.getImagenSecundariaUrl());
        producto.setEspecificaciones(producto.getEspecificaciones() == null ? base.getEspecificaciones() : producto.getEspecificaciones());
        producto.setEstadoPublicacion(base.getEstadoPublicacion());
    }

    @Override
    public void eliminarProducto(Long id) {
        productoGateway.eliminar(id);
    }

    @Override
    public void descontarStock(Long productoId, int cantidad) {
        if (productoId == null || cantidad <= 0) {
            throw new DatosInvalidosException("Producto y cantidad son obligatorios");
        }
        if (!productoGateway.descontarStock(productoId, cantidad)) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para el producto " + productoId);
        }
    }

    private void validar(Producto producto) {
        if (producto == null) {
            throw new DatosInvalidosException("Los datos del producto son obligatorios");
        }
        if ("USADO".equals(producto.getCondicion()) &&
                (producto.getGrado() == null || producto.getNotaEstado() == null || producto.getNotaEstado().isBlank())) {
            throw new DatosInvalidosException("Los artículos usados requieren grado A, B o C y una nota de estado");
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new DatosInvalidosException("El nombre del producto es obligatorio");
        }
        if (producto.getPrecio() == null || producto.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosInvalidosException("El precio debe ser mayor a cero");
        }
        if (producto.getStock() == null || producto.getStock() < 0) {
            throw new DatosInvalidosException("El stock no puede ser negativo");
        }
    }
}
