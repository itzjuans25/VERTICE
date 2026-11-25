package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.edicion;

import com.ecommerce.catalogo.domain.model.Edicion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "catalogo_ediciones")
public class EdicionData {
    @Id
    private Long productoId;
    private int total;
    private int maximoPorCliente;
    private Instant apertura;
    @Column(unique = true, nullable = false)
    private String codigo;
    private String serie;

    Edicion toDomain() {
        return new Edicion(productoId, total, maximoPorCliente, apertura, codigo, serie);
    }
}
