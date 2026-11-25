package com.ecommerce.catalogo.domain.port.out;

import com.ecommerce.catalogo.domain.model.Compra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CompraGateway {

    Compra guardar(Compra compra);

    Compra buscarPorId(Long id);

    List<Compra> buscarPorUsuarioId(Long usuarioId);

    List<Compra> obtenerTodas();
    boolean cambiarEstado(Long id, String anterior, String nuevo);
    boolean registrarPagoSimulado(Long id, Long propietario, Instant fecha);
    long unidadesCompradas(Long usuarioId, Long productoId);
    void guardarCertificado(Long compraId, Certificado certificado);
    Optional<Certificado> certificadoPorCodigo(String codigo);
    List<Certificado> certificados(Long compraId);

    record Certificado(String codigo, Long productoId, String titulo, String edicion, Instant fecha) {}
}
