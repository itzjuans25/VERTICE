package com.ecommerce.catalogo.domain.port.out;

import com.ecommerce.catalogo.domain.model.Compra;

import java.time.Instant;
import java.util.List;

public interface CompraGateway {

    Compra guardar(Compra compra);

    Compra buscarPorId(Long id);

    List<Compra> buscarPorUsuarioId(Long usuarioId);

    List<Compra> obtenerTodas();
    boolean cambiarEstado(Long id, String anterior, String nuevo);
    void registrarPagoSimulado(Long id, Long propietario, Instant fecha);
}
