package com.ecommerce.catalogo.infraestructura.entry_points.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

public record ItemCarritoResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        Integer cantidad,
        BigDecimal subtotal,
        String imagenUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer stockDisponible) {
}
