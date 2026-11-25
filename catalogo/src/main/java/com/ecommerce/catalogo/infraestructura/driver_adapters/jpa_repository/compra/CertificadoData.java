package com.ecommerce.catalogo.infraestructura.driver_adapters.jpa_repository.compra;

import com.ecommerce.catalogo.domain.port.out.CompraGateway.Certificado;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "catalogo_certificados",
        uniqueConstraints = @UniqueConstraint(columnNames = {"compra_id", "producto_id"}))
class CertificadoData {
    @Id private String codigo;
    private Long compraId;
    private Long productoId;
    private String titulo;
    private String edicion;
    private Instant fecha;

    protected CertificadoData() {}

    CertificadoData(Long compraId, Certificado certificado) {
        this.codigo = certificado.codigo();
        this.compraId = compraId;
        this.productoId = certificado.productoId();
        this.titulo = certificado.titulo();
        this.edicion = certificado.edicion();
        this.fecha = certificado.fecha();
    }

    Certificado toDomain() { return new Certificado(codigo, productoId, titulo, edicion, fecha); }
}
