
package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.producto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    
    @Column(columnDefinition = "TEXT")
    private String imagenUrl;
    private String categoria;
    private String grado;
    private String modelo;
    @Column(columnDefinition = "TEXT")
    private String descripcionLarga;
    @Column(columnDefinition = "TEXT")
    private String incluye;
    @Column(columnDefinition = "TEXT")
    private String compatibilidad;
    @Column(columnDefinition = "TEXT")
    private String notaEstado;
    @Column(columnDefinition = "TEXT")
    private String imagenSecundariaUrl;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private java.util.Map<String, String> especificaciones;
    private String condicion = "NUEVO";
    private String tipoVendedor = "TIENDA";
    private String marca = "Genérica";
    private Integer garantiaDias = 0;
    private String estadoPublicacion = "ACTIVA";

    public ProductoData(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock, String imagenUrl, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
        this.categoria = categoria;
    }

    public ProductoData(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock, String imagenUrl) {
        this(id, nombre, descripcion, precio, stock, imagenUrl, null);
    }
}
