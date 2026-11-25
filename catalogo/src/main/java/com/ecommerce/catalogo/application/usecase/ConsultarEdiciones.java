package com.ecommerce.catalogo.application.usecase;

import com.ecommerce.catalogo.domain.model.Edicion;
import com.ecommerce.catalogo.domain.model.Producto;
import com.ecommerce.catalogo.domain.port.out.CompraGateway;
import com.ecommerce.catalogo.domain.port.out.EdicionGateway;
import com.ecommerce.catalogo.domain.port.out.ProductoGateway;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ConsultarEdiciones {
    private final EdicionGateway ediciones;
    private final ProductoGateway productos;
    private final CompraGateway compras;

    public ConsultarEdiciones(EdicionGateway ediciones, ProductoGateway productos, CompraGateway compras) {
        this.ediciones = ediciones;
        this.productos = productos;
        this.compras = compras;
    }

    public List<Lanzamiento> listar() {
        return ediciones.listar().stream().map(this::lanzamiento).flatMap(Optional::stream).toList();
    }

    public Optional<Certificado> verificar(String codigo) {
        if (codigo.startsWith("VT-E-")) {
            return ediciones.porCodigo(codigo).flatMap(this::lanzamiento)
                    .map(l -> new Certificado(codigo, l.titulo(), l.productoId(), l.serie(), "EDICION_REGISTRADA", l.apertura()));
        }
        return compras.certificadoPorCodigo(codigo).map(registro -> new Certificado(codigo,
                registro.titulo(), registro.productoId(), registro.edicion(), "PAGO_SIMULADO_REGISTRADO", registro.fecha()));
    }

    private Optional<Lanzamiento> lanzamiento(Edicion edicion) {
        Producto p = productos.buscarPorId(edicion.productoId());
        return Optional.ofNullable(p).map(producto -> new Lanzamiento(edicion.productoId(), producto.getNombre(),
                producto.getDescripcion(), producto.getCategoria(), producto.getImagenUrl(), producto.getPrecio(),
                producto.getStock(), edicion.total(), edicion.maximoPorCliente(), edicion.apertura(),
                edicion.codigo(), edicion.serie()));
    }

    public record Lanzamiento(Long productoId, String titulo, String relato, String categoria, String imagen,
                              BigDecimal precio, int restantes, int total, int maximoPorCliente, Instant apertura,
                              String codigo, String serie) {}
    public record Certificado(String codigo, String titulo, Long productoId, String edicion, String estado,
                              Instant fecha) {}
}
