package com.ecommerce.catalogo.infraestructura.entry_points.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 2000, message = "La descripcion no puede superar 2000 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        @Digits(integer = 10, fraction = 2, message = "El precio tiene un formato invalido")
        BigDecimal precio,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @jakarta.validation.constraints.Pattern(regexp = "/(?:techtrade|productos)/[A-Za-z0-9_-]+[.](?:webp|png|jpg|jpeg)") String imagenUrl,
        @Size(max = 80) String categoria,
        @jakarta.validation.constraints.Pattern(regexp = "NUEVO|USADO") String condicion,
        @Size(max = 80) String marca,
        @Min(0) @jakarta.validation.constraints.Max(3650) Integer garantiaDias,
        @jakarta.validation.constraints.Pattern(regexp = "A|B|C") String grado,
        @Size(max = 100) String modelo,
        @Size(max = 2000) String descripcionLarga,
        @Size(max = 500) String incluye,
        @Size(max = 500) String compatibilidad,
        @Size(max = 700) String notaEstado,
        @jakarta.validation.constraints.Pattern(regexp = "/(?:techtrade|productos)/[A-Za-z0-9_-]+[.](?:webp|png|jpg|jpeg)") String imagenSecundariaUrl,
        @Size(max = 24) java.util.Map<@NotBlank @Size(max = 80) String, @NotBlank @Size(max = 150) String> especificaciones) {
}
