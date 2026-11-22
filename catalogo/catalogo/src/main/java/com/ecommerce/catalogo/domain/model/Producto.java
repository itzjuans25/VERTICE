package com.ecommerce.catalogo.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Producto {
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private String imagenUrl;
    private String categoria;
    private String grado;
    private String modelo;
    private String descripcionLarga;
    private String incluye;
    private String compatibilidad;
    private String notaEstado;
    private String imagenSecundariaUrl;
    private java.util.Map<String, String> especificaciones;
    private String condicion = "NUEVO";
    private String marca = "Genérica";
    private Integer garantiaDias = 0;
    private String estadoPublicacion = "ACTIVA";

    public Producto(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock, String imagenUrl, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
        this.categoria = categoria;
    }

    public Producto(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock, String imagenUrl) {
        this(id, nombre, descripcion, precio, stock, imagenUrl, null);
    }
}
