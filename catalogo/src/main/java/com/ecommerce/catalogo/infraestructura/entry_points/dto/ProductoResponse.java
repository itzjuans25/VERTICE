package com.ecommerce.catalogo.infraestructura.entry_points.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer stock,
        String imagenUrl,
        String categoria, String condicion, String grado, String marca, String modelo,
        Integer garantiaDias, String estadoStock, String descripcionLarga, String incluye, String compatibilidad,
        String notaEstado, String imagenSecundariaUrl, Map<String, String> especificaciones) {
}
