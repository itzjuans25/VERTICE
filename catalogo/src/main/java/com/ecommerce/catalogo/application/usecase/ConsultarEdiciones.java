package com.ecommerce.catalogo.application.usecase;

import com.ecommerce.catalogo.domain.model.Edicion;
import com.ecommerce.catalogo.domain.model.Producto;
import com.ecommerce.catalogo.domain.port.out.EdicionGateway;
import com.ecommerce.catalogo.domain.port.out.ProductoGateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ConsultarEdiciones {
    private final EdicionGateway ediciones;
    private final ProductoGateway productos;

    public ConsultarEdiciones(EdicionGateway ediciones, ProductoGateway productos) {
        this.ediciones = ediciones;
        this.productos = productos;
    }

    public List<Lanzamiento> listar() {
        return ediciones.listar().stream().map(this::lanzamiento).flatMap(Optional::stream).toList();
    }

    private Optional<Lanzamiento> lanzamiento(Edicion edicion) {
        Producto p = productos.buscarPorId(edicion.productoId());
        return Optional.ofNullable(p).map(producto -> new Lanzamiento(edicion.productoId(), producto.getNombre(),
                producto.getDescripcion(), producto.getCategoria(), producto.getImagenUrl(), producto.getPrecio(),
                producto.getStock(), edicion.total(), edicion.maximoPorCliente(), edicion.apertura(),
                edicion.serie()));
    }

    public record Lanzamiento(Long productoId, String titulo, String relato, String categoria, String imagen,
                              BigDecimal precio, int restantes, int total, int maximoPorCliente, Instant apertura,
                              String serie) {}
}
