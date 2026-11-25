package com.ecommerce.catalogo.infraestructura.entry_points.mapper;

import com.ecommerce.catalogo.application.usecase.InformacionTienda;
import com.ecommerce.catalogo.domain.model.Carrito;
import com.ecommerce.catalogo.domain.model.Compra;
import com.ecommerce.catalogo.domain.model.ItemCarrito;
import com.ecommerce.catalogo.domain.model.Producto;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.CarritoResponse;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.CompraResponse;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.ItemCarritoResponse;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.ProductoRequest;
import com.ecommerce.catalogo.infraestructura.entry_points.dto.ProductoResponse;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TiendaDtoMapper {
    private final InformacionTienda informacion;

    public Producto toDomain(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setImagenUrl(request.imagenUrl());
        producto.setCategoria(request.categoria());
        producto.setCondicion(request.condicion());
        producto.setMarca(request.marca());
        producto.setGarantiaDias(request.garantiaDias());
        producto.setGrado(request.grado());
        producto.setModelo(request.modelo());
        producto.setDescripcionLarga(request.descripcionLarga());
        producto.setIncluye(request.incluye());
        producto.setCompatibilidad(request.compatibilidad());
        producto.setNotaEstado(request.notaEstado());
        producto.setImagenSecundariaUrl(request.imagenSecundariaUrl());
        producto.setEspecificaciones(request.especificaciones());
        return producto;
    }

    public ProductoResponse toResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getImagenUrl(),
                producto.getCategoria(), producto.getCondicion(), producto.getGrado(), producto.getMarca(), producto.getModelo(),
                producto.getGarantiaDias(), producto.getStock() != null && producto.getStock() > 0 ? "DISPONIBLE" : "AGOTADO",
                producto.getDescripcionLarga(), producto.getIncluye(), producto.getCompatibilidad(),
                producto.getNotaEstado(), producto.getImagenSecundariaUrl(),
                producto.getEspecificaciones() == null ? java.util.Map.of() : producto.getEspecificaciones());
    }

    public CarritoResponse toResponse(Carrito carrito, Map<Long, Integer> stock) {
        return new CarritoResponse(
                carrito.getId(),
                carrito.getUsuarioId(),
                carrito.getItems().stream()
                        .map(item -> toItemResponse(item, stock.getOrDefault(item.getProductoId(), 0))).toList(),
                carrito.getPrecioTotal());
    }

    public CompraResponse toResponse(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getUsuarioId(),
                compra.getFechaCompra(),
                toItemResponses(compra.getItems()),
                compra.getTotal(),
                compra.getEstado(),
                Boolean.TRUE.equals(compra.getRequierePagoSimulado()) && !"CANCELADA".equals(compra.getEstado())
                        ? new CompraResponse.Pago("SIM-COMPRA-" + compra.getId(), compra.getPagoSimuladoEn()) : null,
                informacion.cotizarSiDisponible(compra.getTotal()).map(InformacionTienda.Cotizacion::precioUsd).orElse(null));
    }

    private static List<ItemCarritoResponse> toItemResponses(List<ItemCarrito> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items.stream().map(item -> toItemResponse(item, null)).toList();
    }

    private static ItemCarritoResponse toItemResponse(ItemCarrito item, Integer stockDisponible) {
        return new ItemCarritoResponse(
                item.getId(),
                item.getProductoId(),
                item.getNombreProducto(),
                item.getPrecioUnitario(),
                item.getCantidad(),
                item.getSubtotal(),
                item.getImagenUrl(),
                stockDisponible);
    }
}
